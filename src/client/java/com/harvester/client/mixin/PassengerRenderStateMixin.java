package com.harvester.client.mixin;

import com.harvester.client.entity.renderer.VehiclePassengerState;
import com.harvester.vehicle.PassengerPose;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(LivingEntityRenderState.class)
public abstract class PassengerRenderStateMixin implements VehiclePassengerState {
    @Unique private PassengerPose.Limbs harvester$seatPose;
    @Override public PassengerPose.Limbs harvester$getSeatPose() { return harvester$seatPose; }
    @Override public void harvester$setSeatPose(PassengerPose.Limbs pose) { harvester$seatPose = pose; }
}
