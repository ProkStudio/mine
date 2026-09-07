package com.harvester.client.entity.renderer;

import com.harvester.vehicle.PassengerPose;
import com.harvester.vehicle.PassengerAnimation;
import com.harvester.vehicle.VehicleRig;

/** Immutable/value snapshots only; the deferred renderer never retains an entity/world here. */
public interface VehiclePassengerState {
    PassengerPose.Limbs harvester$getSeatPose();
    void harvester$setSeatPose(PassengerPose.Limbs pose);
    PassengerAnimation.Pose harvester$getAnimatedPose();
    void harvester$setAnimatedPose(PassengerAnimation.Pose pose);
    VehicleRig.Pose harvester$getBodyPose();
    void harvester$setBodyPose(VehicleRig.Pose pose);
    float harvester$getSteering();
    void harvester$setSteering(float steering);
}
