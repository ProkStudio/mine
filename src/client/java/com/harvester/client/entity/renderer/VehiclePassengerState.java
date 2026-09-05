package com.harvester.client.entity.renderer;

import com.harvester.vehicle.PassengerPose;

/** A value snapshot only: never retain a live player/vehicle in a queued render state. */
public interface VehiclePassengerState {
    PassengerPose.Limbs harvester$getSeatPose();
    void harvester$setSeatPose(PassengerPose.Limbs pose);
}
