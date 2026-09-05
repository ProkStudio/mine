package com.harvester.entity;

import com.harvester.HarvesterMod;
import com.harvester.config.HarvesterConfig;
import com.harvester.init.ModItems;
import com.harvester.item.FuelCanItem;
import com.harvester.item.VehicleItem;
import com.harvester.vehicle.VehiclePhysics;
import com.harvester.vehicle.VehicleState;
import com.harvester.vehicle.VehicleType;
import net.minecraft.block.*;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.*;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.*;
import net.minecraft.nbt.*;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.FluidTags;
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

/** One non-living vehicle class; the legacy name/registry ID allow old-world migration. */
public class CombineEntity extends Entity {
    private static final TrackedData<Integer> TYPE=DataTracker.registerData(CombineEntity.class,TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> FUEL=DataTracker.registerData(CombineEntity.class,TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> CONDITION=DataTracker.registerData(CombineEntity.class,TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> COLOR=DataTracker.registerData(CombineEntity.class,TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Boolean> HEADER=DataTracker.registerData(CombineEntity.class,TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> RUNNING=DataTracker.registerData(CombineEntity.class,TrackedDataHandlerRegistry.BOOLEAN);
    private final PositionInterpolator interpolator=new PositionInterpolator(this,3);
    private final Set<ServerPlayerEntity> viewers=new HashSet<>();
    private SimpleInventory inventory;
    private int input, inputAge=100, workCooldown, damageCooldown;
    private float impact, wheelAngle, rotorAngle;
    private boolean packed, cargoBlocked, engineFlooded;
    private UUID inputDriver;
    // Transient physics only: do not copy input, water contact or momentum into packed items.
    private double boatTurnRate, lastWaterSurface;
    private int waterMemory;

    public CombineEntity(EntityType<? extends CombineEntity> type, World world) {
        super(type,world);
        inventory=createInventory(27);
        intersectionChecked=true;
    }
    @Override protected void initDataTracker(DataTracker.Builder b) {
        b.add(TYPE,0); b.add(FUEL,0); b.add(CONDITION,100); b.add(COLOR,0); b.add(HEADER,true); b.add(RUNNING,false);
    }
    public VehicleType variant() {
        if(dataTracker==null) return VehicleType.COMBINE;
        return VehicleType.values()[Math.clamp(dataTracker.get(TYPE),0,VehicleType.values().length-1)];
    }
    public HarvesterConfig.Stats stats() { return HarvesterMod.CONFIG.stats(variant()); }
    public void initializeVariant(VehicleType type) {
        dataTracker.set(TYPE,type.ordinal());
        dataTracker.set(CONDITION,HarvesterMod.CONFIG.stats(type).durability);
        inventory=createInventory(HarvesterMod.CONFIG.stats(type).slots);
        calculateDimensions();
    }
    private SimpleInventory createInventory(int size) {
        return new SimpleInventory(size) {
            @Override public boolean canPlayerUse(PlayerEntity p) { return !packed && !isRemoved() && p.squaredDistanceTo(CombineEntity.this)<=64; }
            @Override public boolean isValid(int slot, ItemStack stack) { return !(stack.getItem() instanceof VehicleItem); }
            @Override public void onOpen(ContainerUser user) { if(user.asLivingEntity() instanceof ServerPlayerEntity s) viewers.add(s); }
            @Override public void onClose(ContainerUser user) { if(user.asLivingEntity() instanceof ServerPlayerEntity s) viewers.remove(s); }
        };
    }
    @Override public EntityDimensions getDimensions(EntityPose pose) { VehicleType t=variant(); return EntityDimensions.fixed(t.width,t.height); }
    @Override public void onTrackedDataSet(TrackedData<?> data) { super.onTrackedDataSet(data); if(TYPE.equals(data)) calculateDimensions(); }
    @Override public PositionInterpolator getInterpolator() { return interpolator; }
    /** Not a vanilla client-position-controlled mount. Our driver is getFirstPassenger(). */
    @Override public LivingEntity getControllingPassenger() { return null; }
    @Override protected boolean canAddPassenger(Entity p) { return p instanceof PlayerEntity && getPassengerList().size()<variant().seats; }
    @Override protected Vec3d getPassengerAttachmentPos(Entity p, EntityDimensions dimensions, float scale) {
        int seat=Math.max(0,getPassengerList().indexOf(p));
        double y=variant().family==VehicleType.Family.COMBINE?2.15:variant().height*.70;
        return new Vec3d(0,y,seat==0?-.15:-.85).rotateY(-getYaw()*(float)Math.PI/180);
    }
    @Override public boolean isPushable() { return false; }
    @Override public boolean canHit() { return !isRemoved(); }
    @Override public boolean isAttackable() { return !isRemoved(); }
    @Override public float getStepHeight() { return variant().aircraft() || variant().family==VehicleType.Family.BOAT ? 0 : .6f; }
    @Override public boolean canTeleportBetween(World from, World to) { return to.getRegistryKey().equals(World.OVERWORLD); }
    @Override public boolean handleFallDamage(double distance, float multiplier, DamageSource source) { return false; }
    @Override public void handleFallDamageForPassengers(double distance, float multiplier, DamageSource source) { /* No gratuitous rider damage. */ }

    public void acceptInput(ServerPlayerEntity player, byte keys) {
        if(getFirstPassenger()!=player || player.isSpectator() || isRemoved()) return;
        input=keys & 63; inputAge=0; inputDriver=player.getUuid();
    }
    @Override public ActionResult interact(PlayerEntity player, Hand hand) {
        if(getEntityWorld().isClient()) return ActionResult.SUCCESS;
        if(player.isSpectator() || isRemoved() || packed) return ActionResult.FAIL;
        if(player.isSneaking()) return pickup(player)?ActionResult.SUCCESS:ActionResult.FAIL;
        ItemStack held=player.getStackInHand(hand);
        if(held.getItem() instanceof FuelCanItem can) {
            int amount=Math.min(can.remaining(held),Math.max(0,stats().tank-getFuel()));
            if(amount>0) { dataTracker.set(FUEL,getFuel()+amount); if(!player.getAbilities().creativeMode) can.consume(held,amount); }
            dashboard(player); return ActionResult.SUCCESS;
        }
        if(held.isOf(ModItems.REPAIR_KIT)) {
            if(getCondition()<stats().durability) { dataTracker.set(CONDITION,Math.min(stats().durability,getCondition()+40)); impact=0; consume(player,held); }
            dashboard(player); return ActionResult.SUCCESS;
        }
        if(held.isOf(ModItems.PAINT)) { dataTracker.set(COLOR,(getColor()+1)%16); consume(player,held); return ActionResult.SUCCESS; }
        if(held.isOf(Items.CHEST)) { openCargo(player); return ActionResult.SUCCESS; }
        if(held.isOf(Items.SHEARS) && variant().family==VehicleType.Family.COMBINE) {
            dataTracker.set(HEADER,!isHeaderEnabled());
            player.sendMessage(Text.literal(isHeaderEnabled()?"Жатка опущена":"Жатка поднята (уборка остаётся автоматической)"),true);
            return ActionResult.SUCCESS;
        }
        if(!getEntityWorld().getRegistryKey().equals(World.OVERWORLD)) { player.sendMessage(Text.literal("Техника работает только в Overworld. Shift + ПКМ — забрать."),true); return ActionResult.FAIL; }
        if(player.startRiding(this)) {
            if(variant().aircraft()) player.sendMessage(flightControls(),false);
            else player.sendMessage(Text.literal("W/S — тяга • A/D — поворот • сундук + ПКМ — багажник • Shift + ПКМ — забрать"),true);
        }
        return ActionResult.SUCCESS;
    }
    private Text flightControls() {
        return Text.literal("Курс — взгляд • ").append(Text.keybind("key.forward")).append(" — тяга • ")
            .append(Text.keybind("key.back")).append(" — тормоз • ").append(Text.keybind("key.jump"))
            .append(" — подъём/помощь взлёту • ").append(Text.keybind("key.sprint")).append(" — снижение • ")
            .append(Text.keybind("key.sneak")).append(" — выход. Самолёту нужен разбег; вертолёт/дрон удерживают высоту с расходом топлива.");
    }
    private static void consume(PlayerEntity player, ItemStack stack) { if(!player.getAbilities().creativeMode) stack.decrement(1); }
    private void openCargo(PlayerEntity player) {
        player.openHandledScreen(new NamedScreenHandlerFactory() {
            @Override public Text getDisplayName() { return Text.literal(variant().displayName+" — груз"); }
            @Override public ScreenHandler createMenu(int id, net.minecraft.entity.player.PlayerInventory inv, PlayerEntity p) {
                ScreenHandlerType<?> type=switch(inventory.size()/9) {
                    case 1 -> ScreenHandlerType.GENERIC_9X1;
                    case 2 -> ScreenHandlerType.GENERIC_9X2;
                    case 3 -> ScreenHandlerType.GENERIC_9X3;
                    case 4 -> ScreenHandlerType.GENERIC_9X4;
                    case 5 -> ScreenHandlerType.GENERIC_9X5;
                    default -> ScreenHandlerType.GENERIC_9X6;
                };
                return new GenericContainerScreenHandler(type,id,inv,inventory,inventory.size()/9);
            }
        });
    }
    public VehicleState snapshot() {
        List<ItemStack> cargo=new ArrayList<>();
        for(int i=0;i<inventory.size();i++) cargo.add(inventory.getStack(i).copy());
        return new VehicleState(variant(),getFuel(),getCondition(),getColor(),isHeaderEnabled(),workCooldown,cargo);
    }
    public void restore(VehicleState s) {
        if(s.cargo().size()>54) throw new IllegalArgumentException("Cargo exceeds supported capacity");
        initializeVariant(s.type());
        // Preserve occupied slots and fuel even if a server later reduces its configured capacity.
        inventory=createInventory(Math.min(54,Math.max(stats().slots,((s.cargo().size()+8)/9)*9)));
        dataTracker.set(FUEL,s.fuel()); dataTracker.set(CONDITION,s.condition()); dataTracker.set(COLOR,s.color());
        dataTracker.set(HEADER,s.headerEnabled()); workCooldown=s.workCooldown();
        for(int i=0;i<s.cargo().size();i++) inventory.setStack(i,s.cargo().get(i).copy());
    }
    public ItemStack toVehicleItem() {
        ItemStack item=new ItemStack(ModItems.vehicle(variant()));
        NbtCompound n=new NbtCompound();
        n.put("VehicleState",snapshot().encode(getRegistryManager().getOps(NbtOps.INSTANCE)));
        item.set(DataComponentTypes.CUSTOM_DATA,NbtComponent.of(n));
        if(getCustomName()!=null) item.set(DataComponentTypes.CUSTOM_NAME,getCustomName());
        return item;
    }
    private void closeCargo() { for(ServerPlayerEntity p:List.copyOf(viewers)) p.closeHandledScreen(); viewers.clear(); }
    private boolean pickup(PlayerEntity player) {
        if(packed || isRemoved()) return false;
        final ItemStack item;
        try { item=toVehicleItem(); }
        catch(RuntimeException e) {
            HarvesterMod.LOGGER.error("Vehicle pickup aborted; original retained",e);
            player.sendMessage(Text.literal("Не удалось сохранить технику; она оставлена в мире."),true); return false;
        }
        closeCargo();
        if(player.getInventory().getEmptySlot()<0 || !player.getInventory().insertStack(item)) {
            player.sendMessage(Text.literal("Освободите один слот для техники."),true); return false;
        }
        packed=true; removeAllPassengers(); inventory.clear(); discard(); return true;
    }
    private boolean dropPacked(ServerWorld world) {
        if(packed || isRemoved()) return false;
        final ItemStack item;
        try { item=toVehicleItem(); }
        catch(RuntimeException e) { HarvesterMod.LOGGER.error("Vehicle drop aborted; original retained",e); return false; }
        if(dropStack(world,item)==null) return false;
        packed=true; closeCargo(); removeAllPassengers(); inventory.clear(); discard(); return true;
    }
    @Override public boolean damage(ServerWorld world, DamageSource source, float amount) {
        if(isRemoved() || packed || isInvulnerable() || isAlwaysInvulnerableTo(source) || !Float.isFinite(amount) || amount<=0 || damageCooldown>0) return false;
        damageCooldown=4;
        dataTracker.set(CONDITION,Math.max(0,getCondition()-1));
        impact+=Math.min(amount,10)*10;
        if(impact>=40 || getCondition()==0 || source.getAttacker() instanceof PlayerEntity p && p.getAbilities().creativeMode) dropPacked(world);
        return true;
    }
    @Override public void tick() {
        super.tick();
        if(getEntityWorld().isClient()) {
            double x=getX(),z=getZ();
            interpolator.tick();
            double yaw=Math.toRadians(getYaw());
            double signedDistance=-(getX()-x)*Math.sin(yaw)+(getZ()-z)*Math.cos(yaw);
            wheelAngle=(wheelAngle+(float)signedDistance/.35f)%(float)(Math.PI*2);
            if(isHarvesting()) rotorAngle=(rotorAngle+.65f)%(float)(Math.PI*2);
            return;
        }
        if(packed || isRemoved()) return;
        if(damageCooldown>0) damageCooldown--;
        impact=Math.max(0,impact-.5f);
        if(workCooldown>0) workCooldown--;
        inputAge=Math.min(100,inputAge+1);
        PlayerEntity driver=getFirstPassenger() instanceof PlayerEntity p && !p.isSpectator() ? p : null;
        boolean fresh=driver!=null && driver.getUuid().equals(inputDriver) && inputAge<=10;
        input=VehiclePhysics.usableKeys(input,inputAge,fresh);
        if(!fresh) { inputDriver=null; inputAge=100; }
        if(!getEntityWorld().getRegistryKey().equals(World.OVERWORLD)) {
            setVelocity(Vec3d.ZERO); dataTracker.set(RUNNING,false);
            boatTurnRate=0; waterMemory=0; engineFlooded=false;
            if(driver!=null && age%40==0) dashboard(driver);
            return;
        }
        HarvesterConfig.Stats s=stats();
        VehicleType type=variant();
        boolean boat=type.family==VehicleType.Family.BOAT;
        WaterSample water=type.aircraft() || boat ? sampleWater() : new WaterSample(getY(),0);
        engineFlooded=type.aircraft() && VehiclePhysics.flooded(engineFlooded,water.surface()-getY(),
            Math.min(.50,type.height*.35),water.coverage());
        boolean powered=driver!=null && getFuel()>0 && getCondition()>0 && !engineFlooded;
        int forward=powered?((input&1)!=0?1:0)-((input&2)!=0?1:0):0;
        int turn=powered?((input&8)!=0?1:0)-((input&4)!=0?1:0):0;
        Vec3d old=getVelocity();
        VehiclePhysics.Motion previous=new VehiclePhysics.Motion(old.x,old.y,old.z,getYaw(),getPitch());
        VehiclePhysics.Motion motion;
        boolean flightEngine=false;
        if(type.aircraft()) {
            // Vanilla player look is already server-side; no client positions or extra look payload.
            var flight=VehiclePhysics.flight(previous,type.verticalAircraft(),powered,isOnGround(),engineFlooded,
                fresh,input,driver==null?getYaw():driver.getYaw(),driver==null?getPitch():driver.getPitch(),s.speed);
            motion=flight.motion(); flightEngine=flight.engineActive();
        } else if(boat) {
            boolean contact=water.coverage()>=2.0/9 && water.surface()-getY()>-.06;
            if(contact) { lastWaterSurface=water.surface(); waterMemory=3; }
            else waterMemory=Math.max(0,waterMemory-1);
            boolean recent=waterMemory>0 && !isOnGround()
                && Math.abs(lastWaterSurface-getY()-VehiclePhysics.BOAT_DRAFT)<.20;
            var result=VehiclePhysics.boat(previous,boatTurnRate,input,powered,contact,recent,isOnGround(),
                lastWaterSurface-getY()-VehiclePhysics.BOAT_DRAFT,s.speed);
            motion=result.motion(); boatTurnRate=result.turnRate();
        } else {
            // Land steering is still A/D-only; camera yaw never enters this branch.
            float yaw=HarvesterLogic.steer(getYaw(),turn,2.7f);
            double speed=s.speed;
            BlockState under=getEntityWorld().getBlockState(getBlockPos().down());
            if(under.isOf(Blocks.MUD) || under.isOf(Blocks.SLIME_BLOCK) || under.isOf(Blocks.SOUL_SAND) || under.isOf(Blocks.HONEY_BLOCK)) speed*=.45;
            double target=forward*speed*(forward<0?.45:1);
            double angle=Math.toRadians(yaw);
            motion=new VehiclePhysics.Motion(old.x*.82-Math.sin(angle)*target*.18,old.y-.04,
                old.z*.82+Math.cos(angle)*target*.18,yaw,0);
        }
        setYaw(motion.yaw()); setPitch(motion.pitch());
        double vy=motion.y();
        if(getY()>getEntityWorld().getTopYInclusive()-8) vy=Math.min(0,vy);
        boolean didWork=false;
        if(workCooldown==0) cargoBlocked=false;
        // Starting the drill against a wall is bounded by input, cooldown and block budget.
        if(powered && forward>0 && type.family==VehicleType.Family.DOZER && workCooldown==0) {
            didWork=digFront((ServerWorld)getEntityWorld(),driver);
            workCooldown=HarvesterMod.CONFIG.digRules.intervalTicks;
        }
        double x=getX(), y=getY(), z=getZ();
        Vec3d proposed=new Vec3d(motion.x(),vy,motion.z());
        if(!loadedDestination(proposed)) proposed=Vec3d.ZERO;
        setVelocity(proposed); move(MovementType.SELF,proposed);
        boolean moving=HarvesterLogic.isWorking(powered,getFuel(),MathHelper.square(getX()-x)+MathHelper.square(getY()-y)+MathHelper.square(getZ()-z));
        if(moving && type.family==VehicleType.Family.COMBINE && workCooldown==0) {
            didWork=harvestFront((ServerWorld)getEntityWorld(),driver);
            workCooldown=HarvesterMod.CONFIG.harvestIntervalTicks;
        }
        boolean airborneEngine=flightEngine && !isOnGround();
        boolean fuelDemand=VehiclePhysics.spendsMovementFuel(powered,moving,airborneEngine);
        // A zero movementFuel setting must not reintroduce perpetual powered hovering.
        int movementCost=airborneEngine?Math.max(1,s.movementFuel):s.movementFuel;
        dataTracker.set(FUEL,HarvesterLogic.fuelAfter(getFuel(),movementCost,s.workFuel,fuelDemand,didWork));
        dataTracker.set(RUNNING,powered && getFuel()>0 && (moving || flightEngine || didWork));
        if(driver!=null && age%20==0) dashboard(driver);
        if(isHarvesting()) effects((ServerWorld)getEntityWorld(),moving);
    }
    private boolean loadedDestination(Vec3d movement) {
        if(!Double.isFinite(movement.x) || !Double.isFinite(movement.y) || !Double.isFinite(movement.z)) return false;
        double half=variant().width*.5;
        // Check the footprint, not only the centre, before moving into a chunk or across the border.
        for(double dx:new double[]{-half,half}) for(double dz:new double[]{-half,half}) {
            BlockPos next=BlockPos.ofFloored(getX()+movement.x+dx,getY()+movement.y,getZ()+movement.z+dz);
            if(!getEntityWorld().isChunkLoaded(next) || !getEntityWorld().getWorldBorder().contains(next)) return false;
        }
        return true;
    }
    private record WaterSample(double surface,double coverage) {}
    private WaterSample sampleWater() {
        boolean boat=variant().family==VehicleType.Family.BOAT;
        double side=boat?variant().width*.38:Math.min(.6,variant().width*.22);
        double length=boat?(variant()==VehicleType.BOAT_CARGO?.88:.75):.55;
        double angle=Math.toRadians(getYaw()), sum=0;
        int wet=0;
        // Nine hull probes. Aircraft wings and rotor tips are deliberately excluded.
        for(int a=-1;a<=1;a++) for(int c=-1;c<=1;c++) {
            double x=getX()+Math.cos(angle)*a*side-Math.sin(angle)*c*length;
            double z=getZ()+Math.sin(angle)*a*side+Math.cos(angle)*c*length;
            double top=Double.NEGATIVE_INFINITY;
            for(int y=MathHelper.floor(getY()-.20);y<=MathHelper.floor(getY()+Math.max(.6,variant().height*.5));y++) {
                BlockPos pos=BlockPos.ofFloored(x,y,z);
                if(!getEntityWorld().isChunkLoaded(pos)) continue;
                var fluid=getEntityWorld().getFluidState(pos);
                if(!fluid.isIn(FluidTags.WATER)) continue;
                double surface=y+fluid.getHeight(getEntityWorld(),pos);
                if(surface>getY()-.20) top=Math.max(top,surface);
            }
            if(Double.isFinite(top)) { wet++; sum+=top; }
        }
        return new WaterSample(wet==0?getY():sum/wet,wet/9.0);
    }
    private void effects(ServerWorld world, boolean moving) {
        if(age%4!=0) return;
        VehicleType.Family family=variant().family;
        if(family==VehicleType.Family.BOAT) {
            if(!moving || waterMemory==0) return;
            double rear=variant()==VehicleType.BOAT_CARGO?-1.48:-1.30;
            for(double side:new double[]{-.45,.45}) {
                Vec3d pos=localEffect(side,.32,rear);
                if(!world.getFluidState(BlockPos.ofFloored(pos.x,lastWaterSurface-.08,pos.z)).isIn(FluidTags.WATER)) continue;
                world.spawnParticles(ParticleTypes.SPLASH,pos.x,lastWaterSurface+.06,pos.z,3,.12,.04,.12,.03);
            }
            return;
        }
        // No diesel smoke from an electric drone or from a helicopter's rotor disk.
        if(family==VehicleType.Family.DRONE || family==VehicleType.Family.HELICOPTER) return;
        Vec3d pos=switch(family) {
            case COMBINE -> localEffect(.81,2.53,-.69);
            case DOZER -> localEffect(.61,2.13,.49);
            case PICKUP -> localEffect(.73,.43,variant()==VehicleType.PICKUP_CARGO?-1.25:-1.10);
            case MOTORCYCLE -> localEffect(.36,.48,-.87);
            case PLANE -> localEffect(.39,.62,variant()==VehicleType.PLANE_CARGO?1.62:1.31);
            default -> localEffect(0,.5,-1);
        };
        if(!world.getBlockState(BlockPos.ofFloored(pos.x,pos.y,pos.z)).isAir()) return;
        world.spawnParticles(ParticleTypes.SMOKE,pos.x,pos.y,pos.z,2,.035,.045,.035,.008);
        // Piston placeholder intentionally removed. No pretend replacement OGG or periodic one-shot engine.
    }
    private Vec3d localEffect(double side,double up,double forward) {
        double yaw=Math.toRadians(getYaw());
        return new Vec3d(getX()+Math.cos(yaw)*side-Math.sin(yaw)*forward,getY()+up,
            getZ()+Math.sin(yaw)*side+Math.cos(yaw)*forward);
    }
    private void dashboard(PlayerEntity player) {
        int occupied=0;
        for(int i=0;i<inventory.size();i++) if(!inventory.getStack(i).isEmpty()) occupied++;
        String state=!getEntityWorld().getRegistryKey().equals(World.OVERWORLD)?"только Overworld":getCondition()==0?"нужен ремонт":engineFlooded?"двигатель затоплен; полёт недоступен":getFuel()==0?"нет топлива":cargoBlocked?"бункер заполнен":"готов";
        if(variant().aircraft() && state.equals("готов")) {
            double speed=Math.hypot(getVelocity().x,getVelocity().z);
            if(inputAge>10) state="нет свежего ввода; проверьте управление";
            else if(variant().family==VehicleType.Family.PLANE) state=speed<=VehiclePhysics.TAKEOFF_SPEED?"нужен разбег":"скорость взлёта достигнута";
            else state=isOnGround()?"подъём: клавиша прыжка":"полёт / удержание высоты";
            state+=" | "+String.format(Locale.ROOT,"%.1f",speed*20)+" блок/с";
        }
        player.sendMessage(Text.literal(variant().displayName+" | Топливо "+getFuel()+"/"+stats().tank+" | Груз "+occupied+"/"+inventory.size()+" | Состояние "+getCondition()+"/"+stats().durability+" | "+state),true);
    }
    private List<BlockPos> frontPositions(int height) {
        Set<BlockPos> positions=new LinkedHashSet<>();
        for(int dy=height;dy>=0;dy--) for(int row=0;row<2;row++) for(int side=-stats().workRadius;side<=stats().workRadius;side++) {
            double[] off=HarvesterLogic.headerOffset(getYaw(),side,variant().width*.5+.5+row);
            positions.add(BlockPos.ofFloored(getX()+off[0],getY()+dy,getZ()+off[1]));
        }
        return new ArrayList<>(positions);
    }
    private boolean harvestFront(ServerWorld world, PlayerEntity driver) {
        boolean changed=false;
        for(BlockPos pos:frontPositions(2)) {
            if(!world.isChunkLoaded(pos) || !world.canEntityModifyAt(driver,pos)) continue;
            BlockState state=world.getBlockState(pos), replacement;
            Block block=state.getBlock(); Item seed=null;
            List<ItemStack> drops=new ArrayList<>();
            if(block instanceof CropBlock crop && crop.isMature(state)) {
                seed=block==Blocks.WHEAT?Items.WHEAT_SEEDS:block==Blocks.BEETROOTS?Items.BEETROOT_SEEDS:block==Blocks.CARROTS?Items.CARROT:block==Blocks.POTATOES?Items.POTATO:null;
                if(seed==null) continue;
                replacement=crop.withAge(0);
            } else if(block==Blocks.NETHER_WART && state.get(NetherWartBlock.AGE)==3) {
                seed=Items.NETHER_WART; replacement=state.with(NetherWartBlock.AGE,0);
            } else if(block==Blocks.COCOA && state.get(CocoaBlock.AGE)==2) {
                seed=Items.COCOA_BEANS; replacement=state.with(CocoaBlock.AGE,0);
            } else if(block==Blocks.SWEET_BERRY_BUSH && state.get(SweetBerryBushBlock.AGE)==3) {
                replacement=state.with(SweetBerryBushBlock.AGE,1); drops.add(new ItemStack(Items.SWEET_BERRIES,2+world.random.nextInt(2)));
            } else if(block==Blocks.SUGAR_CANE && world.getBlockState(pos.down()).isOf(Blocks.SUGAR_CANE) && !world.getBlockState(pos.up()).isOf(Blocks.SUGAR_CANE)) {
                replacement=Blocks.AIR.getDefaultState();
            } else if(block==Blocks.MELON || block==Blocks.PUMPKIN) replacement=Blocks.AIR.getDefaultState();
            else continue;
            if(block!=Blocks.SWEET_BERRY_BUSH) for(ItemStack drop:Block.getDroppedStacks(state,world,pos,null)) drops.add(drop.copy());
            if(seed!=null) {
                boolean reserved=false;
                for(ItemStack drop:drops) if(drop.isOf(seed) && !drop.isEmpty()) { drop.decrement(1); reserved=true; break; }
                if(!reserved) continue;
            }
            SimpleInventory staged=stage(drops);
            if(staged==null) { cargoBlocked=true; continue; }
            if(world.setBlockState(pos,replacement,Block.NOTIFY_ALL)) { commitCargo(staged); changed=true; }
        }
        return changed;
    }
    private boolean digFront(ServerWorld world, PlayerEntity driver) {
        int dug=0;
        HarvesterConfig.DigRules rules=HarvesterMod.CONFIG.digRules;
        for(BlockPos pos:frontPositions(1)) {
            if(dug>=rules.blocksPerCycle) break;
            if(!world.isChunkLoaded(pos) || !world.canEntityModifyAt(driver,pos)) continue;
            BlockState state=world.getBlockState(pos);
            if(state.isAir()) continue;
            boolean denied=rules.denied.contains(Registries.BLOCK.getId(state.getBlock()).toString());
            if(!HarvesterLogic.diggable(state.getHardness(world,pos),rules.maxHardness,!state.getFluidState().isEmpty(),world.getBlockEntity(pos)!=null,denied)) continue;
            List<ItemStack> drops=Block.getDroppedStacks(state,world,pos,null,this,new ItemStack(Items.DIAMOND_PICKAXE));
            SimpleInventory staged=stage(drops);
            if(staged==null) { cargoBlocked=true; continue; }
            if(world.setBlockState(pos,Blocks.AIR.getDefaultState(),Block.NOTIFY_ALL)) { commitCargo(staged); dug++; }
        }
        return dug>0;
    }
    private SimpleInventory stage(List<ItemStack> drops) {
        SimpleInventory staged=new SimpleInventory(inventory.size());
        for(int i=0;i<inventory.size();i++) staged.setStack(i,inventory.getStack(i).copy());
        for(ItemStack drop:drops) if(drop.getItem() instanceof VehicleItem || !staged.addStack(drop.copy()).isEmpty()) return null;
        return staged;
    }
    private void commitCargo(SimpleInventory staged) { for(int i=0;i<inventory.size();i++) inventory.setStack(i,staged.getStack(i)); }
    @Override protected void writeCustomData(WriteView view) { view.put("VehicleState",NbtCompound.CODEC,snapshot().encode(getRegistryManager().getOps(NbtOps.INSTANCE))); }
    @Override protected void readCustomData(ReadView view) {
        Optional<NbtCompound> saved=view.read("VehicleState",NbtCompound.CODEC);
        if(saved.isPresent()) restore(VehicleState.decode(saved.get(),getRegistryManager().getOps(NbtOps.INSTANCE)));
        else {
            List<ItemStack> cargo=view.read("Inventory",ItemStack.OPTIONAL_CODEC.listOf()).orElse(List.of());
            restore(new VehicleState(VehicleType.COMBINE,Math.clamp(view.getInt("Fuel",0),0,64000),
                Math.clamp((int)view.getFloat("Health",100),0,10000),0,view.getBoolean("HeaderEnabled",true),0,cargo));
        }
        packed=false; input=0; inputAge=100; inputDriver=null;
        engineFlooded=false; waterMemory=0; boatTurnRate=0; lastWaterSurface=0;
        dataTracker.set(RUNNING,false);
    }
    public boolean isHarvesting() { return dataTracker.get(RUNNING); }
    public boolean isHeaderEnabled() { return dataTracker.get(HEADER); }
    public int getFuel() { return dataTracker.get(FUEL); }
    public int getCondition() { return dataTracker.get(CONDITION); }
    public int getColor() { return dataTracker.get(COLOR); }
    public float wheelAngle() { return wheelAngle; }
    public float rotorAngle() { return rotorAngle; }
}
