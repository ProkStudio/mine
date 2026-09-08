package com.prokstudio.militaryvehicles;
import org.junit.jupiter.api.Test;
class VehicleOperationsTest {
    @Test void aimIsRateLimitedWrappedAndPitchClamped() {OperationsCases.aimIsRateLimitedWrappedAndPitchClamped();}
    @Test void cancelledDeploymentCannotFire() {OperationsCases.cancelledDeploymentCannotFire();}
    @Test void deploymentLocksFromIntentThroughRetraction() {OperationsCases.deploymentLocksFromIntentThroughRetraction();}
    @Test void deploymentRejectsMotionAirAndFaults() {OperationsCases.deploymentRejectsMotionAirAndFaults();}
    @Test void driverAndGunnerPrivilegesAreSeparate() {OperationsCases.driverAndGunnerPrivilegesAreSeparate();}
    @Test void fuelServiceConservesTotalsAndReserve() {OperationsCases.fuelServiceConservesTotalsAndReserve();}
    @Test void fuelServiceRejectsInvalidStores() {OperationsCases.fuelServiceRejectsInvalidStores();}
    @Test void gunRequiresAmmoFuelGroundAndCooldown() {OperationsCases.gunRequiresAmmoFuelGroundAndCooldown();}
    @Test void repairsRequireAStockedKitAndClampToCapacity() {OperationsCases.repairsRequireAStockedKitAndClampToCapacity();}
    @Test void stationParkingRejectsNonFiniteSpeed() {OperationsCases.stationParkingRejectsNonFiniteSpeed();}
}
