package com.harvester.item;

import com.harvester.HarvesterMod;
import com.harvester.entity.CombineEntity;
import com.harvester.entity.HarvesterLogic;
import com.harvester.init.ModEntities;
import com.harvester.vehicle.*;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.*;
import net.minecraft.nbt.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.*;
import net.minecraft.util.hit.*;
import net.minecraft.util.math.*;
import net.minecraft.world.*;

public final class VehicleItem extends Item {
    public final VehicleType type;
    public VehicleItem(Settings settings,VehicleType type) { super(settings); this.type=type; }
    @Override public Text getName(ItemStack stack) { return Text.literal(type.displayName); }
    @Override public boolean canBeNested() { return false; }
    @Override public ActionResult useOnBlock(ItemUsageContext c) {
        BlockPos p=c.getBlockPos().offset(c.getSide());
        return place(c.getWorld(),c.getPlayer(),c.getStack(),p.getX()+.5,p.getY(),p.getZ()+.5);
    }
    @Override public ActionResult use(World world,PlayerEntity player,Hand hand) {
        if(type.family!=VehicleType.Family.BOAT) return ActionResult.PASS;
        BlockHitResult hit=raycast(world,player,RaycastContext.FluidHandling.SOURCE_ONLY);
        if(hit.getType()!=HitResult.Type.BLOCK || world.getFluidState(hit.getBlockPos()).isEmpty()) return ActionResult.PASS;
        return place(world,player,player.getStackInHand(hand),hit.getPos().x,hit.getPos().y-.1,hit.getPos().z);
    }
    private ActionResult place(World world,PlayerEntity player,ItemStack stack,double x,double y,double z) {
        if(player==null || player.isSpectator()) return ActionResult.FAIL;
        if(!world.getRegistryKey().equals(World.OVERWORLD)) { player.sendMessage(Text.literal("Техника доступна только в Overworld."),true); return ActionResult.FAIL; }
        if(world.isClient()) return ActionResult.SUCCESS;
        BlockPos pos=BlockPos.ofFloored(x,y,z);
        if(!world.isChunkLoaded(pos) || !world.getWorldBorder().contains(pos) || !world.canEntityModifyAt(player,pos) || !player.getAbilities().allowModifyWorld) return ActionResult.FAIL;
        CombineEntity vehicle=new CombineEntity(ModEntities.COMBINE,world);
        vehicle.initializeVariant(type);
        NbtCompound data=stack.getOrDefault(DataComponentTypes.CUSTOM_DATA,NbtComponent.DEFAULT).copyNbt();
        boolean saved=data.contains("VehicleState");
        try {
            if(saved) {
                VehicleState state=VehicleState.decode(data.getCompoundOrEmpty("VehicleState"),world.getRegistryManager().getOps(NbtOps.INSTANCE));
                if(state.type()!=type) throw new IllegalArgumentException("Item/type mismatch");
                vehicle.restore(state);
            }
        } catch(RuntimeException e) { HarvesterMod.LOGGER.warn("Invalid vehicle item; placement cancelled",e); player.sendMessage(Text.literal("Некорректное состояние техники; предмет не потрачен."),true); return ActionResult.FAIL; }
        vehicle.refreshPositionAndAngles(x,y,z,player.getYaw(),0);
        vehicle.setCustomName(stack.get(DataComponentTypes.CUSTOM_NAME));
        double r=HarvesterMod.CONFIG.vehicleLimit.radius;
        int nearby=world.getEntitiesByClass(CombineEntity.class,new Box(x-r,y-r,z-r,x+r,y+r,z+r),e->!e.isRemoved() && e.squaredDistanceTo(x,y,z)<=r*r).size();
        if(!HarvesterLogic.withinLimit(nearby,HarvesterMod.CONFIG.vehicleLimit.maximum)) { player.sendMessage(Text.literal("Достигнут лимит техники рядом."),true); return ActionResult.FAIL; }
        if(type.family==VehicleType.Family.BOAT && !world.getFluidState(pos).isIn(net.minecraft.registry.tag.FluidTags.WATER)) { player.sendMessage(Text.literal("Катер ставится на воду."),true); return ActionResult.FAIL; }
        if(!world.isSpaceEmpty(vehicle,vehicle.getBoundingBox()) || !world.getOtherEntities(vehicle,vehicle.getBoundingBox(),e->e instanceof CombineEntity).isEmpty()) { player.sendMessage(Text.literal("Недостаточно места для техники."),true); return ActionResult.FAIL; }
        if(!((ServerWorld)world).spawnEntity(vehicle)) return ActionResult.FAIL;
        // Stateful items are consumed even in creative to prevent duplicating their cargo.
        if(saved || !player.getAbilities().creativeMode) stack.decrement(1);
        return ActionResult.SUCCESS;
    }
}
