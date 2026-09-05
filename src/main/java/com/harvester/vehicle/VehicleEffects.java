package com.harvester.vehicle;

import com.harvester.entity.CombineEntity;
import net.minecraft.block.BlockState;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

/** Effects originate at the same tilted geometry as the mesh and seats. All budgets are bounded. */
public final class VehicleEffects {
    private VehicleEffects() {}
    public static void work(ServerWorld world,BlockPos pos,BlockState before) {
        world.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK,before),
            pos.getX()+.5,pos.getY()+.6,pos.getZ()+.5,2,.22,.16,.22,.015);
    }
    public static void tick(CombineEntity vehicle,ServerWorld world,boolean moving) {
        if(vehicle.age%4!=0) return;
        var type=vehicle.variant(); var family=type.family;
        if(type.verticalAircraft()) {
            if(!vehicle.isEngineActive() || vehicle.isEngineFlooded()) return;
            double radius=Math.min(1.1,type.width*.55);
            for(int side:new int[]{-1,1}) for(int front:new int[]{-1,1}) {
                Vec3d at=vehicle.localEffect(side*radius,0,front*radius);
                var ground=VehicleGround.sample(world,at.x,vehicle.getY()+.1,at.z,5);
                if(ground==null || vehicle.getY()-ground.y()>4.5 || vehicle.getY()<ground.y()-.15) continue;
                if(ground.water()) world.spawnParticles(ParticleTypes.SPLASH,at.x,ground.y()+.06,at.z,2,.15,.025,.15,.025);
                else world.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK,ground.state()),at.x,ground.y()+.07,at.z,2,.13,.025,.13,.02);
            }
            return; // No diesel smoke on an electric drone or in a rotor disk.
        }
        if(family==VehicleType.Family.BOAT) {
            if(!moving || !vehicle.isInWater()) return;
            double rear=type==VehicleType.BOAT_CARGO?-1.48:-1.30;
            for(double side:new double[]{-.45,.45}) {
                Vec3d at=vehicle.localEffect(side,.3,rear);
                var ground=VehicleGround.sample(world,at.x,vehicle.getY()+.8,at.z,2);
                if(ground!=null && ground.water()) world.spawnParticles(ParticleTypes.SPLASH,at.x,ground.y()+.06,at.z,3,.12,.04,.12,.03);
            }
            return;
        }
        if(moving && vehicle.isOnGround() && !type.aircraft()) {
            double side=family==VehicleType.Family.MOTORCYCLE?.12:Math.min(type.width*.43,1.08);
            double rear=family==VehicleType.Family.MOTORCYCLE?-.75:-.7;
            for(int s:new int[]{-1,1}) {
                Vec3d at=vehicle.localEffect(side*s,.1,rear);
                var ground=VehicleGround.sample(world,at.x,vehicle.getY()+.5,at.z,2);
                if(ground!=null && !ground.water() && Math.abs(vehicle.getY()-ground.y())<.65)
                    world.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK,ground.state()),at.x,ground.y()+.05,at.z,2,.08,.025,.08,.012);
            }
        }
        if(!vehicle.isEngineActive()) return;
        Vec3d at=switch(family) {
            case COMBINE -> vehicle.localEffect(.81,2.53,-.69);
            case DOZER -> vehicle.localEffect(.61,2.13,.49);
            case PICKUP -> vehicle.localEffect(.73,.43,type==VehicleType.PICKUP_CARGO?-1.25:-1.10);
            case MOTORCYCLE -> vehicle.localEffect(.36,.48,-.87);
            case PLANE -> vehicle.localEffect(.39,.62,type==VehicleType.PLANE_CARGO?1.62:1.31);
            default -> vehicle.localEffect(0,.5,-1);
        };
        BlockPos pos=BlockPos.ofFloored(at.x,at.y,at.z);
        if(world.isChunkLoaded(pos) && world.getBlockState(pos).isAir() && !world.getFluidState(pos).isIn(FluidTags.WATER))
            world.spawnParticles(ParticleTypes.SMOKE,at.x,at.y,at.z,2,.035,.045,.035,.008);
    }
}
