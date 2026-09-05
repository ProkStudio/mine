package com.harvester.vehicle;

import net.minecraft.block.BlockState;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** Bounded loaded-block queries only; no chunk loads, raycasts across the world or heightmap scans. */
public final class VehicleGround {
    public record Surface(BlockPos pos,BlockState state,double y,boolean water) {}
    private VehicleGround() {}
    public static Surface sample(World world,double x,double fromY,double z,int depth) {
        BlockPos top=BlockPos.ofFloored(x,fromY,z);
        for(int i=0;i<=Math.clamp(depth,0,6);i++) {
            BlockPos pos=top.down(i);
            if(!world.isChunkLoaded(pos)) return null;
            var fluid=world.getFluidState(pos);
            var state=world.getBlockState(pos);
            if(fluid.isIn(FluidTags.WATER)) return new Surface(pos,state,pos.getY()+fluid.getHeight(world,pos),true);
            if(!fluid.isEmpty()) return null;
            var shape=state.getCollisionShape(world,pos);
            if(!shape.isEmpty()) return new Surface(pos,state,pos.getY()+shape.getBoundingBox().maxY,false);
        }
        return null;
    }
}
