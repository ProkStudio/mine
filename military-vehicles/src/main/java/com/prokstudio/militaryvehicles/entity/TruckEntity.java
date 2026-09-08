package com.prokstudio.militaryvehicles.entity;
import com.mojang.serialization.Codec;
import com.prokstudio.militaryvehicles.MilitaryVehicles;
import com.prokstudio.militaryvehicles.core.*;
import com.prokstudio.militaryvehicles.init.MilitaryContent;
import com.prokstudio.militaryvehicles.item.FuelCanItem;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.*;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.*;
import net.minecraft.nbt.*;
import net.minecraft.screen.*;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.text.Text;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraft.world.World;
import java.util.*;
/** Non-living truck. Only the server moves it, changes cargo or consumes supplies. */
public final class TruckEntity extends Entity {
    public static final Codec<VehicleSave<ItemStack>> SAVE_CODEC=VehicleSaveCodec.create(ItemStack.OPTIONAL_CODEC);
    private static final TrackedData<Integer> FUEL=DataTracker.registerData(TruckEntity.class,TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> CONDITION=DataTracker.registerData(TruckEntity.class,TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Boolean> ENGINE=DataTracker.registerData(TruckEntity.class,TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Byte> STEER=DataTracker.registerData(TruckEntity.class,TrackedDataHandlerRegistry.BYTE);
    private static final TrackedData<Byte> ENGINE_LOAD=DataTracker.registerData(TruckEntity.class,TrackedDataHandlerRegistry.BYTE);
    private double observedSpeed;
    private final PositionInterpolator interpolator=new PositionInterpolator(this,3);
    private final ControlLatch controls=new ControlLatch();
    private final Set<ServerPlayerEntity> viewers=new HashSet<>();
    private final SimpleInventory cargo;
    private UUID driverId;
    private int fuelTicks,damageCooldown;
    private boolean packed;
    // Preserve unsupported blobs instead of silently replacing cargo with defaults.
    private NbtCompound quarantinedState;
    private float wheelAngle,previousWheelAngle,steerAngle;
    public TruckEntity(EntityType<? extends TruckEntity> type,World world) {
        super(type,world);intersectionChecked=true;
        cargo=new SimpleInventory(TruckSpec.SLOTS) {
            @Override public boolean canPlayerUse(PlayerEntity p) { return !packed&&!isRemoved()&&quarantinedState==null&&p.squaredDistanceTo(TruckEntity.this)<=64; }
            @Override public boolean isValid(int slot,ItemStack stack) { return stack.getItem().canBeNested(); }
            @Override public void onOpen(ContainerUser user) { if(user.asLivingEntity() instanceof ServerPlayerEntity p) viewers.add(p); }
            @Override public void onClose(ContainerUser user) { if(user.asLivingEntity() instanceof ServerPlayerEntity p) viewers.remove(p); }
        };
    }
    @Override protected void initDataTracker(DataTracker.Builder b) { b.add(FUEL,0);b.add(CONDITION,TruckSpec.CONDITION);b.add(ENGINE,false);b.add(STEER,(byte)0);b.add(ENGINE_LOAD,(byte)0); }
    @Override public PositionInterpolator getInterpolator() { return interpolator; }
    @Override public LivingEntity getControllingPassenger() { return null; }
    @Override public boolean isPushable() { return false; }
    @Override public boolean canHit() { return !isRemoved(); }
    @Override public boolean isAttackable() { return !isRemoved(); }
    @Override public float getStepHeight() { return .6f; }
    @Override protected boolean canAddPassenger(Entity passenger) {
        return passenger instanceof PlayerEntity p&&!p.isSpectator()&&quarantinedState==null&&getPassengerList().size()<TruckSpec.SEATS;
    }
    @Override protected Vec3d getPassengerAttachmentPos(Entity p,EntityDimensions dimensions,float scale) {
        int index=Math.max(0,getPassengerList().indexOf(p));double x=index==0?9/16.0:-9/16.0;
        double hip=.75*(p instanceof LivingEntity living?living.getScale():1);
        return new Vec3d(x,24/16.0-hip,16/16.0).rotateY((float)-Math.toRadians(getYaw())).add(p.getVehicleAttachmentPos(this));
    }
    @Override public Vec3d updatePassengerForDismount(LivingEntity p) {
        double distance=TruckSpec.WIDTH/2+.6;
        for(double side:new double[]{distance,-distance}) for(int dy=1;dy>=-2;dy--) {
            Vec3d off=new Vec3d(side,dy,0).rotateY((float)-Math.toRadians(getYaw()));
            BlockPos pos=BlockPos.ofFloored(getX()+off.x,getY()+off.y,getZ()+off.z);World world=getEntityWorld();
            if(!world.isChunkLoaded(pos)||!world.getWorldBorder().contains(pos)) continue;
            var shape=world.getBlockState(pos.down()).getCollisionShape(world,pos.down());
            if(shape.isEmpty()||!world.getFluidState(pos).isEmpty()) continue;
            Vec3d at=new Vec3d(pos.getX()+.5,pos.getY()-1+shape.getMax(Direction.Axis.Y)+.02,pos.getZ()+.5);
            Vec3d delta=at.subtract(p.getX(),p.getY(),p.getZ());
            if(world.isSpaceEmpty(p,p.getBoundingBox().offset(delta))) return at;
        }
        return super.updatePassengerForDismount(p);
    }
    public void acceptInput(ServerPlayerEntity player,int keys) {
        if(player!=getFirstPassenger()||player.isSpectator()||packed||isRemoved()||quarantinedState!=null) return;
        if(player.currentScreenHandler!=player.playerScreenHandler) { stopControls();return; }
        if(!player.getUuid().equals(driverId)) { stopControls();driverId=player.getUuid(); }
        controls.accept(getEntityWorld().getTime(),keys);
    }
    private void stopControls() { controls.reset();driverId=null;setEngine(false);dataTracker.set(STEER,(byte)0); }
    private void setEngine(boolean enabled) { dataTracker.set(ENGINE,enabled);if(!enabled) dataTracker.set(ENGINE_LOAD,(byte)0); }
    public int fuel() { return dataTracker.get(FUEL); }
    public int condition() { return dataTracker.get(CONDITION); }
    public boolean engineRunning() { return dataTracker.get(ENGINE); }
    public float engineLoad() { return Math.clamp(dataTracker.get(ENGINE_LOAD)/100f,0,1); }
    public double observedSpeed() { return observedSpeed; }
    public float wheelAngle(float delta) { return previousWheelAngle+(wheelAngle-previousWheelAngle)*delta; }
    public float steerAngle() { return steerAngle; }
    @Override public void tick() {
        super.tick();
        if(getEntityWorld().isClient()) {
            double x=getX(),z=getZ();interpolator.tick();
            observedSpeed=TruckFeedback.observedSpeed(getX()-x,getZ()-z);
            double travel=TruckPhysics.signedSpeed(getX()-x,getZ()-z,getYaw());previousWheelAngle=wheelAngle;
            if(Math.abs(travel)<2) wheelAngle+=(float)(travel/(5.5/16));
            if(Math.abs(wheelAngle)>Math.PI*200) { float shift=(float)(Math.PI*200)*Math.signum(wheelAngle);wheelAngle-=shift;previousWheelAngle-=shift; }
            steerAngle+=(dataTracker.get(STEER)/100f*.48f-steerAngle)*.35f;return;
        }
        if(packed||isRemoved()) return;
        if(damageCooldown>0) damageCooldown--;
        PlayerEntity driver=getFirstPassenger() instanceof PlayerEntity p&&!p.isSpectator()?p:null;
        long now=getEntityWorld().getTime();
        boolean authorized=driver!=null&&driver.getUuid().equals(driverId)&&controls.fresh(now)
            &&(!(driver instanceof ServerPlayerEntity s)||s.currentScreenHandler==s.playerScreenHandler);
        if(!authorized||quarantinedState!=null) stopControls();
        if(authorized&&controls.consumeToggle(now)) setEngine(!engineRunning());
        if(fuel()==0||condition()==0||isTouchingWater()||quarantinedState!=null) setEngine(false);
        int keys=authorized?controls.keys(now):ControlLatch.BRAKE;
        dataTracker.set(ENGINE_LOAD,TruckFeedback.driveLoad(keys,engineRunning(),isOnGround()));
        double speed=TruckPhysics.signedSpeed(getVelocity().x,getVelocity().z,getYaw());
        var motion=TruckPhysics.step(speed,getYaw(),keys,engineRunning(),isOnGround(),1);
        setYaw(motion.yaw());dataTracker.set(STEER,(byte)Math.round(motion.steer()/.48f*100));
        double angle=Math.toRadians(getYaw());
        Vec3d proposed=new Vec3d(-Math.sin(angle)*motion.speed(),Math.max(-1.2,getVelocity().y-.04),Math.cos(angle)*motion.speed());
        if(!destinationLoaded(proposed)) { proposed=Vec3d.ZERO;stopControls(); }
        setVelocity(proposed);move(MovementType.SELF,proposed);
        if(engineRunning()) { fuelTicks++;if(fuelTicks>=10) { fuelTicks-=10;dataTracker.set(FUEL,Math.max(0,fuel()-1)); }if(fuel()==0) setEngine(false); }
        TruckExhaust.tick(this,(ServerWorld)getEntityWorld());
        if(driver!=null&&age%10==0) dashboard(driver);
    }
    private boolean destinationLoaded(Vec3d delta) {
        if(!Double.isFinite(delta.x)||!Double.isFinite(delta.y)||!Double.isFinite(delta.z)) return false;
        Box box=getBoundingBox().offset(delta);
        for(double x:new double[]{box.minX,box.maxX}) for(double z:new double[]{box.minZ,box.maxZ}) {
            BlockPos pos=BlockPos.ofFloored(x,box.minY,z);
            if(!getEntityWorld().isChunkLoaded(pos)||!getEntityWorld().getWorldBorder().contains(pos)) return false;
        }
        return true;
    }
    @Override public ActionResult interact(PlayerEntity player,Hand hand) {
        if(getEntityWorld().isClient()) return ActionResult.SUCCESS;
        if(packed||isRemoved()||player.isSpectator()) return ActionResult.FAIL;
        if(quarantinedState!=null) { message(player,"invalid_state");return ActionResult.FAIL; }
        if(player.isSneaking()) return pickup(player)?ActionResult.SUCCESS:ActionResult.FAIL;
        ItemStack held=player.getStackInHand(hand);
        if(held.getItem() instanceof FuelCanItem can) {
            int amount=TruckPhysics.transferFuel(fuel(),can.remaining(held));dataTracker.set(FUEL,fuel()+amount);
            if(!player.getAbilities().creativeMode) can.consume(held,amount);
            dashboard(player);return ActionResult.SUCCESS;
        }
        if(held.isOf(MilitaryContent.REPAIR_KIT)) {
            if(condition()<TruckSpec.CONDITION) { dataTracker.set(CONDITION,Math.min(TruckSpec.CONDITION,condition()+50));if(!player.getAbilities().creativeMode) held.decrement(1); }
            dashboard(player);return ActionResult.SUCCESS;
        }
        if(held.isOf(Items.CHEST)) { openCargo(player);return ActionResult.SUCCESS; }
        if(player.startRiding(this)) {
            player.sendMessage(Text.translatable("message.militaryvehicles.controls",Text.keybind("key.militaryvehicles.engine"),Text.keybind("key.sneak")),false);return ActionResult.SUCCESS;
        }
        message(player,"seats_full");return ActionResult.FAIL;
    }
    private void openCargo(PlayerEntity player) {
        player.openHandledScreen(new NamedScreenHandlerFactory() {
            @Override public Text getDisplayName() { return Text.translatable("container.militaryvehicles.cargo"); }
            @Override public ScreenHandler createMenu(int id,net.minecraft.entity.player.PlayerInventory inv,PlayerEntity p) { return new CargoScreenHandler(id,inv,cargo); }
        });
    }
    private void dashboard(PlayerEntity player) {
        int occupied=0;for(int i=0;i<cargo.size();i++) if(!cargo.getStack(i).isEmpty()) occupied++;
        Text status=Text.translatable("message.militaryvehicles."+(quarantinedState!=null?"invalid_state":condition()==0?"broken":isTouchingWater()?"flooded":fuel()==0?"empty":engineRunning()?"running":"stopped"));
        player.sendMessage(Text.translatable("hud.militaryvehicles.truck",fuel(),TruckSpec.TANK,condition(),TruckSpec.CONDITION,occupied,TruckSpec.SLOTS,status),true);
    }
    private void message(PlayerEntity player,String key) { player.sendMessage(Text.translatable("message.militaryvehicles."+key),true); }
    public VehicleSave<ItemStack> snapshot() {
        if(quarantinedState!=null) throw new IllegalStateException("Unsupported save is quarantined");
        List<ItemStack> items=new ArrayList<>(TruckSpec.SLOTS);for(int i=0;i<cargo.size();i++) items.add(cargo.getStack(i).copy());
        return new VehicleSave<>(1,TruckSpec.ID,fuel(),condition(),fuelTicks,items);
    }
    public void restore(VehicleSave<ItemStack> state) {
        for(ItemStack stack:state.cargo()) if(!stack.isEmpty()&&!stack.getItem().canBeNested()) throw new IllegalArgumentException("Nested vehicle/container in cargo");
        dataTracker.set(FUEL,state.fuel());dataTracker.set(CONDITION,state.condition());fuelTicks=state.fuelTicks();
        for(int i=0;i<cargo.size();i++) cargo.setStack(i,state.cargo().get(i).copy());
        quarantinedState=null;stopControls();setVelocity(Vec3d.ZERO);
    }
    public ItemStack packedItem() {
        var ops=getRegistryManager().getOps(NbtOps.INSTANCE);NbtCompound data=new NbtCompound();
        data.put("VehicleState",SAVE_CODEC.encodeStart(ops,snapshot()).getOrThrow());
        ItemStack item=new ItemStack(MilitaryContent.TRUCK);item.set(DataComponentTypes.CUSTOM_DATA,NbtComponent.of(data));
        if(getCustomName()!=null) item.set(DataComponentTypes.CUSTOM_NAME,getCustomName());return item;
    }
    private boolean pickup(PlayerEntity player) {
        if(hasPassengers()||getVelocity().horizontalLengthSquared()>.000225) { message(player,"park_first");return false; }
        if(player.getInventory().getEmptySlot()<0) { message(player,"inventory_full");return false; }
        final ItemStack item;
        try { item=packedItem(); }catch(RuntimeException ex) { MilitaryVehicles.LOGGER.error("Truck packing failed; original retained",ex);message(player,"invalid_state");return false; }
        if(!player.getInventory().insertStack(item)) return false;finishPacking();return true;
    }
    private void finishPacking() {
        packed=true;stopControls();for(ServerPlayerEntity viewer:List.copyOf(viewers)) viewer.closeHandledScreen();
        viewers.clear();removeAllPassengers();cargo.clear();discard();
    }
    @Override public boolean damage(ServerWorld world,DamageSource source,float amount) {
        if(packed||isRemoved()||quarantinedState!=null||isInvulnerable()||isAlwaysInvulnerableTo(source)||!Float.isFinite(amount)||amount<=0||damageCooldown>0||getPassengerList().contains(source.getAttacker())) return false;
        damageCooldown=5;dataTracker.set(CONDITION,Math.max(0,condition()-(int)Math.ceil(Math.min(amount,100))));
        if(condition()==0) {
            stopControls();try { if(dropStack(world,packedItem())!=null) finishPacking(); }
            catch(RuntimeException ex) { MilitaryVehicles.LOGGER.error("Truck drop failed; original retained",ex); }
        }
        return true;
    }
    @Override protected void writeCustomData(WriteView view) {
        if(quarantinedState!=null) view.put("VehicleState",NbtCompound.CODEC,quarantinedState.copy());else view.put("VehicleState",SAVE_CODEC,snapshot());
    }
    @Override protected void readCustomData(ReadView view) {
        var saved=view.read("VehicleState",NbtCompound.CODEC);
        if(saved.isPresent()) {
            try { restore(SAVE_CODEC.parse(getRegistryManager().getOps(NbtOps.INSTANCE),saved.get()).getOrThrow()); }
            catch(RuntimeException ex) { quarantinedState=saved.get().copy();MilitaryVehicles.LOGGER.error("Unsupported truck save; inert entity retains raw state",ex); }
        }
        packed=false;damageCooldown=0;stopControls();
    }
}
