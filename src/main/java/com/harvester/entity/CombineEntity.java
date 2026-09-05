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
import net.minecraft.item.Item;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Rideable harvester. World changes and cargo transactions are server-only. */
public class CombineEntity extends MobEntity {
    private static final TrackedData<Boolean> HARVESTING = DataTracker.registerData(CombineEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> HEADER_ENABLED = DataTracker.registerData(CombineEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Integer> FUEL = DataTracker.registerData(CombineEntity.class, TrackedDataHandlerRegistry.INTEGER);

    private final SimpleInventory inventory = new SimpleInventory(27) {
        @Override public boolean canPlayerUse(PlayerEntity player) {
            return CombineEntity.this.isAlive() && player.squaredDistanceTo(CombineEntity.this) <= 64.0;
        }
    };
    private int harvestCooldown;
    private int soundTick;
    private boolean cargoBlocked;
    private boolean positionKnown;
    private double lastWorkX;
    private double lastWorkZ;
    private float reelRotation;
    private float previousReelRotation;

    public CombineEntity(EntityType<? extends CombineEntity> type, World world) {
        super(type, world);
        setPersistent();
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return MobEntity.createMobAttributes().add(EntityAttributes.MAX_HEALTH, 100.0)
                .add(EntityAttributes.MOVEMENT_SPEED, 0.15).add(EntityAttributes.KNOCKBACK_RESISTANCE, 0.8);
    }

    @Override protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);
        builder.add(HARVESTING, false);
        builder.add(HEADER_ENABLED, true);
        // A recovered spawn module must not become a source of unlimited free fuel.
        builder.add(FUEL, 0);
    }

    @Override public ActionResult interactMob(PlayerEntity player, Hand hand) {
        if (getEntityWorld().isClient()) return ActionResult.SUCCESS;
        ItemStack held = player.getStackInHand(hand);
        if (player.isSneaking()) {
            player.openHandledScreen(new CombineScreenFactory());
        } else if (held.isOf(Items.SHEARS)) {
            dataTracker.set(HEADER_ENABLED, !isHeaderEnabled());
            cargoBlocked = false;
            if (!isHeaderEnabled()) setHarvesting(false);
            player.sendMessage(Text.translatable(isHeaderEnabled() ? "message.harvester.header_on" : "message.harvester.header_off"), true);
        } else if (held.isOf(Items.IRON_INGOT)) {
            if (getHealth() < getMaxHealth()) {
                heal(10.0f);
                if (!player.getAbilities().creativeMode) held.decrement(1);
            }
            player.sendMessage(Text.translatable("message.harvester.repaired", (int) getHealth(), (int) getMaxHealth()), true);
        } else if (isFuel(held)) {
            if (getFuel() < HarvesterMod.CONFIG.maxFuel) {
                int added = held.isOf(Items.COAL_BLOCK) ? 800 : 80;
                setFuel(getFuel() + added);
                if (!player.getAbilities().creativeMode) held.decrement(1);
            }
            player.sendMessage(Text.translatable("message.harvester.fuel", getFuel(), HarvesterMod.CONFIG.maxFuel), true);
        } else if (getFirstPassenger() == null) {
            if (player.startRiding(this)) player.sendMessage(Text.translatable("message.harvester.controls"), true);
        } else if (getFirstPassenger() == player) {
            player.stopRiding();
        }
        return ActionResult.SUCCESS;
    }

    private boolean isFuel(ItemStack stack) {
        return stack.isOf(Items.COAL) || stack.isOf(Items.CHARCOAL) || stack.isOf(Items.COAL_BLOCK);
    }

    @Override public LivingEntity getControllingPassenger() {
        Entity passenger = getFirstPassenger();
        return passenger instanceof PlayerEntity player ? player : super.getControllingPassenger();
    }

    @Override protected Vec3d getPassengerAttachmentPos(Entity passenger, EntityDimensions dimensions, float scaleFactor) {
        // Seat follows the lowered cab and rotates with the chassis.
        return new Vec3d(0, 1.15 * scaleFactor, -0.1 * scaleFactor).rotateY(-getYaw() * ((float) Math.PI / 180.0f));
    }

    @Override public void tick() {
        super.tick();
        if (getEntityWorld().isClient()) { tickClientEffects(); return; }
        double dx = positionKnown ? getX() - lastWorkX : 0.0;
        double dz = positionKnown ? getZ() - lastWorkZ : 0.0;
        lastWorkX = getX();
        lastWorkZ = getZ();
        positionKnown = true;
        Entity passenger = getFirstPassenger();
        boolean working = HarvesterLogic.isWorking(passenger instanceof PlayerEntity, getFuel(), dx * dx + dz * dz);
        if (passenger instanceof PlayerEntity driver && getFuel() > 0) {
            setYaw(driver.getYaw());
            setHeadYaw(driver.getYaw());
            setBodyYaw(driver.getYaw());
        }
        if (harvestCooldown > 0) harvestCooldown--;
        if (working && isHeaderEnabled() && harvestCooldown == 0) {
            harvestCooldown = HarvesterMod.CONFIG.harvestIntervalTicks;
            cargoBlocked = false;
            harvestCrops();
        }
        setHarvesting(working && isHeaderEnabled() && !cargoBlocked);
        if (working) {
            setFuel(getFuel() - HarvesterMod.CONFIG.fuelPerTick);
            if (getFuel() == 0) {
                setHarvesting(false);
                if (passenger instanceof PlayerEntity player) player.sendMessage(Text.translatable("message.harvester.empty"), true);
            }
        }
        if (passenger == null || getFuel() == 0) setVelocity(getVelocity().multiply(0.55, 1.0, 0.55));
        if (passenger instanceof PlayerEntity driver && age % 20 == 0) {
            int occupied = 0;
            for (int slot = 0; slot < inventory.size(); slot++) if (!inventory.getStack(slot).isEmpty()) occupied++;
            String status = getFuel() == 0 ? "empty" : !isHeaderEnabled() ? "header_off" : cargoBlocked ? "full" : "header_on";
            driver.sendMessage(Text.translatable("message.harvester.dashboard", getFuel(), HarvesterMod.CONFIG.maxFuel,
                    occupied, inventory.size(), Text.translatable("message.harvester." + status)), true);
        }
    }

    /** Keep vanilla collision/gravity; reverse and lateral travel are deliberately slower. */
    @Override public void travel(Vec3d movementInput) {
        if (getControllingPassenger() instanceof PlayerEntity player) {
            setYaw(player.getYaw());
            setPitch(0.0f);
            setMovementSpeed((float) HarvesterMod.CONFIG.drivingSpeed);
            float forward = player.forwardSpeed;
            if (forward < 0) forward *= 0.45f;
            Vec3d input = getFuel() > 0 ? new Vec3d(player.sidewaysSpeed * 0.25f, 0, forward) : Vec3d.ZERO;
            super.travel(input);
            return;
        }
        super.travel(movementInput);
    }

    /** Two rows under the front header, not a square harvesting behind the vehicle. */
    private void harvestCrops() {
        int radius = HarvesterMod.CONFIG.harvestRadius;
        Set<BlockPos> visited = new HashSet<>();
        for (int dy = 2; dy >= -1; dy--) {
            for (int forward = 2; forward <= 3; forward++) {
                for (int lateral = -radius; lateral <= radius; lateral++) {
                    double[] offset = HarvesterLogic.headerOffset(getYaw(), lateral, forward);
                    BlockPos pos = BlockPos.ofFloored(getX() + offset[0], getY() + dy, getZ() + offset[1]);
                    // Rotated samples can resolve to the same block. Harvest it at most once.
                    if (visited.add(pos)) harvestBlock(pos);
                }
            }
        }
    }

    private void harvestBlock(BlockPos pos) {
        ServerWorld world = (ServerWorld) getEntityWorld();
        if (!world.isChunkLoaded(pos)) return;
        BlockState state = world.getBlockState(pos);
        Block block = state.getBlock();
        List<ItemStack> drops = new ArrayList<>();
        BlockState replanted;
        Item seed = null;
        if (block instanceof CropBlock crop && crop.isMature(state)) {
            seed = block == Blocks.WHEAT ? Items.WHEAT_SEEDS : block == Blocks.BEETROOTS ? Items.BEETROOT_SEEDS
                    : block == Blocks.CARROTS ? Items.CARROT : block == Blocks.POTATOES ? Items.POTATO : null;
            if (seed == null) return; // Unknown mod crops need an explicit planting rule.
            replanted = crop.withAge(0);
        } else if (block == Blocks.NETHER_WART && state.get(NetherWartBlock.AGE) == NetherWartBlock.MAX_AGE) {
            seed = Items.NETHER_WART;
            replanted = state.with(NetherWartBlock.AGE, 0);
        } else if (block == Blocks.COCOA && state.get(CocoaBlock.AGE) == 2) {
            seed = Items.COCOA_BEANS;
            replanted = state.with(CocoaBlock.AGE, 0);
        } else if (block == Blocks.SWEET_BERRY_BUSH && state.get(SweetBerryBushBlock.AGE) == 3) {
            drops.add(new ItemStack(Items.SWEET_BERRIES, 2 + world.random.nextInt(2)));
            replanted = state.with(SweetBerryBushBlock.AGE, 1);
        } else if (block == Blocks.SUGAR_CANE && world.getBlockState(pos.down()).isOf(Blocks.SUGAR_CANE)) {
            // Never remove a middle segment: updates would break the top outside the cargo transaction.
            if (world.getBlockState(pos.up()).isOf(Blocks.SUGAR_CANE)) return;
            replanted = Blocks.AIR.getDefaultState();
        } else if (block == Blocks.MELON || block == Blocks.PUMPKIN) {
            replanted = Blocks.AIR.getDefaultState();
        } else {
            return;
        }
        if (block != Blocks.SWEET_BERRY_BUSH) {
            for (ItemStack drop : Block.getDroppedStacks(state, world, pos, null)) drops.add(drop.copy());
        }
        // Reserve planting material from this harvest, never create a free seed.
        if (seed != null && !reserveSeed(drops, seed)) return;
        SimpleInventory staged = new SimpleInventory(inventory.size());
        for (int slot = 0; slot < inventory.size(); slot++) staged.setStack(slot, inventory.getStack(slot).copy());
        for (ItemStack drop : drops) {
            if (!drop.isEmpty() && !staged.addStack(drop.copy()).isEmpty()) {
                cargoBlocked = true;
                return; // Keep both crop and real cargo unchanged when the complete drop cannot fit.
            }
        }
        if (!world.setBlockState(pos, replanted, Block.NOTIFY_ALL)) return;
        for (int slot = 0; slot < inventory.size(); slot++) inventory.setStack(slot, staged.getStack(slot));
        world.spawnParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5, pos.getY() + 0.7, pos.getZ() + 0.5, 1, 0, 0.1, 0, 0);
    }

    private static boolean reserveSeed(List<ItemStack> drops, Item seed) {
        for (ItemStack drop : drops) {
            if (drop.isOf(seed) && !drop.isEmpty()) { drop.decrement(1); return true; }
        }
        return false;
    }

    @Override protected void writeCustomData(WriteView view) {
        super.writeCustomData(view);
        view.putInt("Fuel", getFuel());
        view.putBoolean("HeaderEnabled", isHeaderEnabled());
        List<ItemStack> stacks = new ArrayList<>();
        for (int slot = 0; slot < inventory.size(); slot++) stacks.add(inventory.getStack(slot));
        view.put("Inventory", ItemStack.OPTIONAL_CODEC.listOf(), stacks);
    }

    @Override protected void readCustomData(ReadView view) {
        super.readCustomData(view);
        setFuel(view.getInt("Fuel", 0));
        dataTracker.set(HEADER_ENABLED, view.getBoolean("HeaderEnabled", true));
        setHarvesting(false);
        positionKnown = false;
        cargoBlocked = false;
        harvestCooldown = 0;
        inventory.clear();
        view.read("Inventory", ItemStack.OPTIONAL_CODEC.listOf()).ifPresent(stacks -> {
            for (int slot = 0; slot < Math.min(inventory.size(), stacks.size()); slot++) inventory.setStack(slot, stacks.get(slot));
        });
    }

    @Override protected void dropEquipment(ServerWorld world, net.minecraft.entity.damage.DamageSource source, boolean causedByPlayer) {
        super.dropEquipment(world, source, causedByPlayer);
        for (int slot = 0; slot < inventory.size(); slot++) {
            ItemStack stack = inventory.removeStack(slot);
            if (!stack.isEmpty()) dropStack(world, stack);
        }
        dropStack(world, new ItemStack(ModItems.COMBINE_SPAWN_EGG));
    }

    @Override protected boolean canAddPassenger(Entity passenger) { return passenger instanceof PlayerEntity && getPassengerList().isEmpty(); }
    @Override public boolean isPushable() { return false; }
    private void tickClientEffects() {
        previousReelRotation = reelRotation;
        if (isHarvesting()) reelRotation += 0.3f;
        if (reelRotation > Math.PI * 2) {
            reelRotation -= (float) (Math.PI * 2);
            previousReelRotation -= (float) (Math.PI * 2);
        }
        if (isHarvesting() && ++soundTick % 20 == 0) getEntityWorld().playSoundClient(getX(), getY(), getZ(), SoundEvents.BLOCK_PISTON_CONTRACT, SoundCategory.NEUTRAL, 0.35f, 0.6f, false);
    }
    public float getReelRotation(float tickProgress) { return previousReelRotation + (reelRotation - previousReelRotation) * tickProgress; }
    public boolean isHeaderEnabled() { return dataTracker.get(HEADER_ENABLED); }
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
