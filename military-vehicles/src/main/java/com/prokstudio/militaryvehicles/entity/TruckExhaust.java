package com.prokstudio.militaryvehicles.entity;

import com.prokstudio.militaryvehicles.core.TruckFeedback;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

/** Server emission only: nearby clients observe the same source, without duplicate client smoke. */
public final class TruckExhaust {
    private TruckExhaust() {}
    public static void tick(TruckEntity truck, ServerWorld world) {
        boolean powered = truck.engineRunning() && truck.fuel() > 0 && truck.condition() > 0
            && !truck.isRemoved() && !truck.isTouchingWater();
        if (!TruckFeedback.emitExhaust(powered, truck.engineLoad(), world.getTime(), truck.getId())
                || !Float.isFinite(truck.getYaw())) return;
        var offset = TruckFeedback.exhaustOffset(truck.getYaw());
        double x = truck.getX() + offset.x(), y = truck.getY() + offset.y(), z = truck.getZ() + offset.z();
        BlockPos pos = BlockPos.ofFloored(x, y, z);
        if (!world.isChunkLoaded(pos) || !world.getWorldBorder().contains(pos)
                || !world.getBlockState(pos).isAir() || !world.getFluidState(pos).isEmpty()) return;
        world.spawnParticles(ParticleTypes.SMOKE, x, y, z, 1, .006, .008, .006, .008);
    }
}
