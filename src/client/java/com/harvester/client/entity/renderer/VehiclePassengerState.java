package com.harvester.client.entity.renderer;

import com.harvester.vehicle.PassengerPose;
import com.harvester.vehicle.VehicleRig;

/** Only immutable/value state; no queued references to a player, vehicle or world. */
public interface VehiclePassengerState {
    PassengerPose.Limbs harvester$getSeatPose();
    void harvester$setSeatPose(PassengerPose.Limbs pose);
    VehicleRig.Pose harvester$getBodyPose();
    void harvester$setBodyPose(VehicleRig.Pose pose);
    float harvester$getSteering();
    void harvester$setSteering(float steering);
}
