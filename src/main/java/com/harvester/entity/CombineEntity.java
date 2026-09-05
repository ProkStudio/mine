package com.harvester.entity;

import com.harvester.HarvesterMod;
import com.harvester.init.ModItems;
import net.minecraft.block.*;
import net.minecraft.entity.*;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

/** Rideable harvester with a persistent vanilla 9x3 cargo inventory. */
public class CombineEntity extends MobEntity {
    private static final TrackedData<Boolean> HARVESTING = DataTracker.registerData(CombineEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Integer> FUEL = DataTracker.registerData(CombineEntity.class, TrackedDataHandlerRegistry.INTEGER);

    private final SimpleInventory inventory = new SimpleInventory(27);
    private int harvestCooldown;
    private int soundTick;
    private boolean engineRunning;

    public CombineEntity(EntityType<? extends CombineEntity> type, World world) { super(type, world); }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return MobEntity.createMobAttributes().add(EntityAttributes.MAX_HEALTH, 100.0)
                .add(EntityAttributes.MOVEMENT_SPEED, 0.15).add(EntityAttributes.KNOCKBACK_RESISTANCE, 0.8);
    }

    @Override protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);
        builder.add(HARVESTING, false);
        builder.add(FUEL, HarvesterMod.CONFIG.maxFuel);
    }

    @Override public ActionResult interactMob(PlayerEntity player, Hand hand) {
        if (getEntityWorld().isClient()) return ActionResult.SUCCESS;
        ItemStack held = player.getStackInHand(hand);
        if (player.isSneaking()) {
            player.openHandledScreen(new CombineScreenFactory());
        } else if (held.isOf(Items.IRON_INGOT) && getHealth() < getMaxHealth()) {
            heal(10.0f);
            if (!player.getAbilities().creativeMode) held.decrement(1);
            player.sendMessage(Text.literal("🔧 Комбайн отремонтирован: " + (int) getHealth() + "/" + (int) getMaxHealth()), true);
        } else if (isFuel(held) && getFuel() < HarvesterMod.CONFIG.maxFuel) {
            int added = held.isOf(Items.COAL_BLOCK) ? 800 : 80;
            setFuel(Math.min(HarvesterMod.CONFIG.maxFuel, getFuel() + added));
            if (!player.getAbilities().creativeMode) held.decrement(1);
            player.sendMessage(Text.literal("⛽ Топливо: " + getFuel() + "/" + HarvesterMod.CONFIG.maxFuel), true);
        } else if (getFirstPassenger() == null) {
            player.startRiding(this);
            player.sendMessage(Text.literal("🚜 WASD — движение | Shift+ПКМ — инвентарь | Уголь — заправка"), true);
        } else if (getFirstPassenger() == player) {
            player.stopRiding();
        }
        return ActionResult.SUCCESS;
    }

    private boolean isFuel(ItemStack stack) { return stack.isOf(Items.COAL) || stack.isOf(Items.COAL_BLOCK); }

    /** Marks the rider as the controller so vanilla sends their WASD input to this vehicle. */
    @Override public LivingEntity getControllingPassenger() {
        Entity passenger = getFirstPassenger();
        return passenger instanceof PlayerEntity player ? player : super.getControllingPassenger();
    }

    @Override protected Vec3d getPassengerAttachmentPos(Entity passenger, EntityDimensions dimensions, float scaleFactor) {
        // A high, forward cockpit seat keeps the dashboard below the camera in first person.
        return new Vec3d(0, 2.15 * scaleFactor, -0.55 * scaleFactor);
    }

    @Override public void tick() {
        super.tick();
        if (getEntityWorld().isClient()) { tickClientEffects(); return; }
        Entity passenger = getFirstPassenger();
        if (passenger instanceof PlayerEntity driver) tickDriving(driver); else stopEngine();
        if (!engineRunning || getFuel() <= 0) return;
        if (--harvestCooldown <= 0) {
            harvestCooldown = HarvesterMod.CONFIG.harvestIntervalTicks;
            harvestCrops();
        }
        setFuel(getFuel() - HarvesterMod.CONFIG.fuelPerTick);
        if (getFuel() <= 0) {
            stopEngine();
            if (passenger instanceof PlayerEntity player) player.sendMessage(Text.literal("⚠ Топливо кончилось. Заправьте комбайн углём."), true);
        }
    }

    private void tickDriving(PlayerEntity driver) {
        if (getFuel() <= 0) { stopEngine(); return; }
        // The engine stays active while the driver is in the cab, allowing a parked combine to harvest a row.
        engineRunning = true;
        setHarvesting(Math.abs(driver.forwardSpeed) > 0.01 || Math.abs(driver.sidewaysSpeed) > 0.01);
        setYaw(driver.getYaw());
        setHeadYaw(driver.getYaw());
        setMovementSpeed((float) HarvesterMod.CONFIG.drivingSpeed);
    }

    /** Lets LivingEntity perform collision, slopes and gravity using the rider's synchronized input. */
    @Override public void travel(Vec3d movementInput) {
        LivingEntity controller = getControllingPassenger();
        if (controller instanceof PlayerEntity player && getFuel() > 0) {
            setYaw(player.getYaw());
            setPitch(0.0f);
            setMovementSpeed((float) HarvesterMod.CONFIG.drivingSpeed);
            super.travel(new Vec3d(player.sidewaysSpeed * 0.5f, movementInput.y, player.forwardSpeed));
            return;
        }
        super.travel(movementInput);
    }

    private void stopEngine() {
        engineRunning = false;
        setHarvesting(false);
        setVelocity(getVelocity().multiply(0.55, 1.0, 0.55));
    }

    private void harvestCrops() {
        BlockPos center = getBlockPos();
        int radius = HarvesterMod.CONFIG.harvestRadius;
        for (int dx = -radius; dx <= radius; dx++) for (int dz = -radius; dz <= radius; dz++) {
            harvestBlock(center.add(dx, -1, dz));
            harvestBlock(center.add(dx, 0, dz));
            harvestBlock(center.add(dx, 1, dz));
        }
    }

    private void harvestBlock(BlockPos pos) {
        ServerWorld world = (ServerWorld) getEntityWorld();
        BlockState state = world.getBlockState(pos);
        Block block = state.getBlock();
        List<ItemStack> drops = new ArrayList<>();
        BlockState replanted = null;
        if (block instanceof CropBlock crop && crop.isMature(state)) {
            drops.addAll(Block.getDroppedStacks(state, (ServerWorld) world, pos, null));
            replanted = crop.withAge(0);
        } else if (block == Blocks.NETHER_WART && state.get(NetherWartBlock.AGE) == NetherWartBlock.MAX_AGE) {
            drops.addAll(Block.getDroppedStacks(state, (ServerWorld) world, pos, null));
            replanted = state.with(NetherWartBlock.AGE, 0);
        } else if (block == Blocks.SUGAR_CANE && world.getBlockState(pos.down()).isOf(Blocks.SUGAR_CANE)) {
            drops.addAll(Block.getDroppedStacks(state, (ServerWorld) world, pos, null));
            replanted = Blocks.AIR.getDefaultState();
        } else if (block == Blocks.MELON || block == Blocks.PUMPKIN) {
            drops.addAll(Block.getDroppedStacks(state, (ServerWorld) world, pos, null));
            replanted = Blocks.AIR.getDefaultState();
        }
        if (replanted == null) return;
        world.setBlockState(pos, replanted, Block.NOTIFY_ALL);
        for (ItemStack drop : drops) {
            ItemStack remainder = inventory.addStack(drop.copy());
            if (!remainder.isEmpty()) dropStack(world, remainder);
        }
        world.spawnParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 1, 0, 0.15, 0, 0);
    }

    @Override protected void writeCustomData(WriteView view) {
        view.putInt("Fuel", getFuel());
        List<ItemStack> stacks = new ArrayList<>();
        for (int slot = 0; slot < inventory.size(); slot++) stacks.add(inventory.getStack(slot));
        view.put("Inventory", ItemStack.OPTIONAL_CODEC.listOf(), stacks);
    }

    @Override protected void readCustomData(ReadView view) {
        setFuel(view.getInt("Fuel", HarvesterMod.CONFIG.maxFuel));
        inventory.clear();
        view.read("Inventory", ItemStack.OPTIONAL_CODEC.listOf()).ifPresent(stacks -> {
            for (int slot = 0; slot < Math.min(inventory.size(), stacks.size()); slot++) inventory.setStack(slot, stacks.get(slot));
        });
    }

    @Override protected void dropEquipment(ServerWorld world, net.minecraft.entity.damage.DamageSource source, boolean causedByPlayer) {
        super.dropEquipment(world, source, causedByPlayer);
        for (int slot = 0; slot < inventory.size(); slot++) {
            ItemStack stack = inventory.getStack(slot);
            if (!stack.isEmpty()) dropStack(world, stack);
        }
        dropStack(world, new ItemStack(ModItems.COMBINE_SPAWN_EGG));
        inventory.clear();
    }

    @Override protected boolean canAddPassenger(Entity passenger) { return getPassengerList().isEmpty(); }
    @Override public boolean isPushable() { return false; }
    private void tickClientEffects() {
        if (isHarvesting() && ++soundTick % 20 == 0) getEntityWorld().playSoundClient(getX(), getY(), getZ(), SoundEvents.BLOCK_PISTON_CONTRACT, SoundCategory.NEUTRAL, 0.35f, 0.6f, false);
    }
    public boolean isHarvesting() { return dataTracker.get(HARVESTING); }
    public void setHarvesting(boolean value) { dataTracker.set(HARVESTING, value); }
    public int getFuel() { return dataTracker.get(FUEL); }
    public void setFuel(int value) { dataTracker.set(FUEL, Math.clamp(value, 0, HarvesterMod.CONFIG.maxFuel)); }

    private final class CombineScreenFactory implements NamedScreenHandlerFactory {
        @Override public Text getDisplayName() { return Text.translatable("container.harvester.combine"); }
        @Override public ScreenHandler createMenu(int syncId, net.minecraft.entity.player.PlayerInventory playerInventory, PlayerEntity player) {
            return GenericContainerScreenHandler.createGeneric9x3(syncId, playerInventory, inventory);
        }
    }
}
