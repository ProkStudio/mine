package com.prokstudio.militaryvehicles;

import org.junit.jupiter.api.Test;

class VehicleParkingTest {
    @Test void packingAllowsStoppedDryEmptyVehicle() { ParkingCases.packingAllowsStoppedDryEmptyVehicle(); }
    @Test void packingKeepsPassengerAndDeploymentLocks() { ParkingCases.packingKeepsPassengerAndDeploymentLocks(); }
    @Test void packingRejectsAirAndWater() { ParkingCases.packingRejectsAirAndWater(); }
    @Test void packingIncludesVerticalAndDiagonalMotion() { ParkingCases.packingIncludesVerticalAndDiagonalMotion(); }
    @Test void packingRejectsNonFiniteAndOverflowingVelocity() { ParkingCases.packingRejectsNonFiniteAndOverflowingVelocity(); }
    @Test void packingPreservesLegacySpeedBoundary() { ParkingCases.packingPreservesLegacySpeedBoundary(); }
    @Test void stationSwitchRequiresArmedSoleFirstPassenger() { ParkingCases.stationSwitchRequiresArmedSoleFirstPassenger(); }
    @Test void stationSwitchRejectsAirWaterAndVerticalMotion() { ParkingCases.stationSwitchRejectsAirWaterAndVerticalMotion(); }
    @Test void stationSwitchRejectsNonFiniteAndOverflowingVelocity() { ParkingCases.stationSwitchRejectsNonFiniteAndOverflowingVelocity(); }
    @Test void stationSwitchPreservesStopThreshold() { ParkingCases.stationSwitchPreservesStopThreshold(); }
}
