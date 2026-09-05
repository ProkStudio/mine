package com.harvester.client.mixin;

import com.harvester.client.entity.renderer.VehiclePassengerState;
import com.harvester.vehicle.PassengerPose;
import com.harvester.vehicle.VehicleRig;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(LivingEntityRenderState.class)
public abstract class PassengerRenderStateMixin implements VehiclePassengerState {
    @Unique private PassengerPose.Limbs harvester$seatPose;
    @Unique private VehicleRig.Pose harvester$bodyPose=VehicleRig.Pose.ZERO;
    @Unique private float harvester$steering;
    @Override public PassengerPose.Limbs harvester$getSeatPose() { return harvester$seatPose; }
    @Override public void harvester$setSeatPose(PassengerPose.Limbs pose) { harvester$seatPose=pose; }
    @Override public VehicleRig.Pose harvester$getBodyPose() { return harvester$bodyPose; }
    @Override public void harvester$setBodyPose(VehicleRig.Pose pose) { harvester$bodyPose=pose; }
    @Override public float harvester$getSteering() { return harvester$steering; }
    @Override public void harvester$setSteering(float steering) { harvester$steering=steering; }
}
