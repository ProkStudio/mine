package com.prokstudio.militaryvehicles.item;
import com.prokstudio.militaryvehicles.MilitaryVehicles;
import com.prokstudio.militaryvehicles.core.TruckSpec;
import com.prokstudio.militaryvehicles.config.ServerFleetConfig;
import com.prokstudio.militaryvehicles.core.VehicleKind;
import com.prokstudio.militaryvehicles.entity.TruckEntity;
import com.prokstudio.militaryvehicles.init.MilitaryContent;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraft.world.World;
import java.util.function.Consumer;
/** Shared placement/packing item; immutable registered kind prevents cross-type state conversion. */
public final class TruckItem extends Item {
    private final VehicleKind kind;
    public TruckItem(Settings settings,VehicleKind kind) { super(settings);this.kind=kind; }
    @Override public boolean canBeNested() { return false; }
    @Override public void appendTooltip(ItemStack stack,TooltipContext context,TooltipDisplayComponent display,Consumer<Text> text,TooltipType type) {
        super.appendTooltip(stack,context,display,text,type);text.accept(Text.translatable("tooltip.militaryvehicles.truck").formatted(Formatting.GRAY));
        text.accept(Text.translatable("tooltip.militaryvehicles.capacity",kind.seats.size(),kind.cargoSlots()).formatted(Formatting.GRAY));
        text.accept(Text.translatable("tooltip.militaryvehicles.alpha").formatted(Formatting.YELLOW));
    }
    @Override public ActionResult useOnBlock(ItemUsageContext c) {
        World world=c.getWorld();PlayerEntity player=c.getPlayer();
        if(player==null||player.isSpectator()||!player.getAbilities().allowModifyWorld) return ActionResult.FAIL;
        if(world.isClient()) return ActionResult.SUCCESS;
        BlockPos pos=c.getBlockPos().offset(c.getSide());
        if(!world.isChunkLoaded(pos)||!world.canEntityModifyAt(player,pos)) return ActionResult.FAIL;
        ItemStack stack=c.getStack();var data=stack.getOrDefault(DataComponentTypes.CUSTOM_DATA,NbtComponent.DEFAULT).copyNbt();
        boolean saved=data.contains("VehicleState");TruckEntity truck=new TruckEntity(MilitaryContent.vehicleEntity(kind),world,kind);
        try { if(saved) truck.restore(TruckEntity.SAVE_CODEC.parse(world.getRegistryManager().getOps(NbtOps.INSTANCE),data.getCompound("VehicleState").orElseThrow()).getOrThrow()); }
        catch(RuntimeException ex) { MilitaryVehicles.LOGGER.warn("Invalid packed vehicle; item not consumed",ex);return fail(player,"invalid_state"); }
        truck.refreshPositionAndAngles(pos.getX()+.5,pos.getY(),pos.getZ()+.5,player.getYaw(),0);truck.setCustomName(stack.get(DataComponentTypes.CUSTOM_NAME));
        Box box=truck.getBoundingBox();
        for(double x:new double[]{box.minX,box.maxX}) for(double z:new double[]{box.minZ,box.maxZ}) {
            BlockPos edge=BlockPos.ofFloored(x,box.minY,z);
            if(!world.isChunkLoaded(edge)||!world.getWorldBorder().contains(edge)||!world.canEntityModifyAt(player,edge)) return fail(player,"no_space");
        }
        if(box.maxY>world.getTopYInclusive()+1||!world.isSpaceEmpty(truck,box)||!world.getOtherEntities(truck,box,e->e.canHit()&&e!=player).isEmpty()) return fail(player,"no_space");
        if(!world.getFluidState(pos).isEmpty()) return fail(player,"dry_ground");
        double radius=TruckSpec.LIMIT_RADIUS;
        // One budget across the fleet, not a separate quota for each registered entity type.
        int nearby=world.getEntitiesByClass(TruckEntity.class,box.expand(radius),e->!e.isRemoved()&&e.squaredDistanceTo(truck)<=radius*radius).size();
        if(nearby>=ServerFleetConfig.get(((ServerWorld)world).getServer()).nearbyVehicleLimit()) return fail(player,"limit");
        if(!((ServerWorld)world).spawnEntity(truck)) return ActionResult.FAIL;
        if(saved||!player.getAbilities().creativeMode) stack.decrement(1);
        player.sendMessage(Text.translatable("message.militaryvehicles.placed"),false);return ActionResult.SUCCESS;
    }
    private ActionResult fail(PlayerEntity p,String key) { p.sendMessage(Text.translatable("message.militaryvehicles."+key),true);return ActionResult.FAIL; }
}
