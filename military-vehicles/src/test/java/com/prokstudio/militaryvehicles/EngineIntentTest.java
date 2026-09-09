package com.prokstudio.militaryvehicles;

import org.junit.jupiter.api.Test;

class EngineIntentTest {
    @Test void firstPressAfterMountStillStartsEngine() { EngineIntentCases.firstPressAfterMountStillStartsEngine(); }
    @Test void heldEngineKeyCannotToggleAfterControlTimeout() { EngineIntentCases.heldEngineKeyCannotToggleAfterControlTimeout(); }
    @Test void repeatedTimeoutsCannotAccumulateIntent() { EngineIntentCases.repeatedTimeoutsCannotAccumulateIntent(); }
    @Test void releaseAfterTimeoutRestoresEnginePress() { EngineIntentCases.releaseAfterTimeoutRestoresEnginePress(); }
    @Test void remountWithHeldKeyCannotStartEngine() { EngineIntentCases.remountWithHeldKeyCannotStartEngine(); }
    @Test void remountAfterReleaseTogglesAgain() { EngineIntentCases.remountAfterReleaseTogglesAgain(); }
    @Test void awaitingReleaseDoesNotBlockDrivingOrBrake() { EngineIntentCases.awaitingReleaseDoesNotBlockDrivingOrBrake(); }
    @Test void staleInputStillBrakesAndDiscardsToggle() { EngineIntentCases.staleInputStillBrakesAndDiscardsToggle(); }
    @Test void invalidAndReplayedPacketsCannotArmTheEngine() { EngineIntentCases.invalidAndReplayedPacketsCannotArmTheEngine(); }
    @Test void separateSeatsDoNotShareEngineIntent() { EngineIntentCases.separateSeatsDoNotShareEngineIntent(); }
    @Test void legacyControlLatchExpectationsPreserved() { EngineIntentCases.legacyControlLatchExpectationsPreserved(); }
}
