package com.prokstudio.militaryvehicles;
import org.junit.jupiter.api.Test;
class TrackedDriveTest {
    @Test void bothDirectionsRespectPerTrackLimits() {OperationsCases.bothDirectionsRespectPerTrackLimits();}
    @Test void brakingStopsBothTracksIncludingPivot() {OperationsCases.brakingStopsBothTracksIncludingPivot();}
    @Test void collisionCorrectionDoesNotStoreForwardImpulse() {OperationsCases.collisionCorrectionDoesNotStoreForwardImpulse();}
    @Test void counterRotatingTracksPivotAtRest() {OperationsCases.counterRotatingTracksPivotAtRest();}
    @Test void invalidValuesFailClosed() {OperationsCases.invalidValuesFailClosed();}
    @Test void oppositeInputCrossesZeroBeforeReversing() {OperationsCases.oppositeInputCrossesZeroBeforeReversing();}
    @Test void padsCirculateBoundedlyAndIndependently() {OperationsCases.padsCirculateBoundedlyAndIndependently();}
    @Test void turningDirectionIsConsistentInReverse() {OperationsCases.turningDirectionIsConsistentInReverse();}
    @Test void unpoweredAndAirCannotAddKineticEnergy() {OperationsCases.unpoweredAndAirCannotAddKineticEnergy();}
}
