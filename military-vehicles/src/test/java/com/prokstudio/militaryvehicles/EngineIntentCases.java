package com.prokstudio.militaryvehicles;

import com.prokstudio.militaryvehicles.core.ControlLatch;

/** Shared by JUnit and the standalone smoke runner; drives the real latch without game mocks. */
public final class EngineIntentCases {
    private static final int ENGINE = ControlLatch.ENGINE, BRAKE = ControlLatch.BRAKE,
        FORWARD = ControlLatch.FORWARD, BACK = ControlLatch.BACK, LEFT = ControlLatch.LEFT,
        TIMEOUT = ControlLatch.TIMEOUT;
    private static long checks;
    private EngineIntentCases() {}
    private static void check(boolean actual, String context) {
        checks++;
        if (!actual) throw new AssertionError(context);
    }
    /** A latch that already started the engine from a real first press, like a fresh mount. */
    private static ControlLatch running(long tick) {
        ControlLatch latch = new ControlLatch();
        check(latch.accept(tick, ENGINE), "first packet of a mounted driver is accepted");
        check(latch.consumeToggle(tick), "the first actual press still starts the engine");
        return latch;
    }
    private static void hold(ControlLatch latch, long from, long until, int rawKeys) {
        for (long tick = from; tick <= until; tick++) {
            check(latch.accept(tick, rawKeys), "steady input is accepted");
            check(!latch.consumeToggle(tick), "a key that stays down is not pressed again");
        }
    }

    public static void firstPressAfterMountStillStartsEngine() {
        ControlLatch latch = running(0);
        hold(latch, 1, 60, ENGINE);
        check(latch.accept(61, 0), "release packet accepted");
        check(!latch.consumeToggle(61), "releasing the key is not a press");
        check(latch.accept(62, ENGINE), "second press accepted");
        check(latch.consumeToggle(62), "a real second press stops the engine again");
        check(!latch.consumeToggle(62), "one press is consumed exactly once");
        check(!latch.awaitingRelease(), "an uninterrupted seat never waits for a release");
    }
    public static void heldEngineKeyCannotToggleAfterControlTimeout() {
        ControlLatch latch = running(0);
        hold(latch, 1, 5, ENGINE);
        long resumed = 5 + TIMEOUT + 1;
        check(latch.accept(resumed, ENGINE), "input resumes after the gap");
        check(latch.awaitingRelease(), "a gap disarms the still-held engine key");
        check(!latch.consumeToggle(resumed), "lag alone must not toggle the engine");
        hold(latch, resumed + 1, resumed + 40, ENGINE);
        check(latch.keys(resumed + 40) == ENGINE, "the key snapshot itself is still reported");
    }
    public static void repeatedTimeoutsCannotAccumulateIntent() {
        ControlLatch latch = running(0);
        long tick = 1;
        for (int gap = 0; gap < 5; gap++) {
            tick += TIMEOUT + 3;
            check(latch.accept(tick, ENGINE), "input resumes after every gap");
            check(!latch.consumeToggle(tick), "each gap stays disarmed");
            check(latch.awaitingRelease(), "the held key remains disarmed");
        }
        check(latch.accept(tick + 1, 0), "release after the gaps");
        check(latch.accept(tick + 2, ENGINE), "press after the release");
        check(latch.consumeToggle(tick + 2), "exactly one press survives repeated gaps");
    }
    public static void releaseAfterTimeoutRestoresEnginePress() {
        ControlLatch latch = running(0);
        long resumed = 1 + TIMEOUT + 1;
        check(latch.accept(resumed, ENGINE) && !latch.consumeToggle(resumed), "disarmed after the gap");
        check(latch.accept(resumed + 1, BRAKE), "driver releases the engine key");
        check(!latch.awaitingRelease(), "an observed release rearms the key");
        check(!latch.consumeToggle(resumed + 1), "the release itself is not a press");
        check(latch.accept(resumed + 2, ENGINE), "new press accepted");
        check(latch.consumeToggle(resumed + 2), "a real press after the gap still works");
    }
    public static void remountWithHeldKeyCannotStartEngine() {
        ControlLatch latch = running(0);
        latch.reset();
        check(latch.awaitingRelease(), "a cleared seat disarms the held key");
        check(latch.accept(1, ENGINE), "first packet after remount accepted");
        check(!latch.consumeToggle(1), "returning to the seat must not start the engine by itself");
        hold(latch, 2, 30, ENGINE);
        check(!latch.consumeToggle(30), "still no synthesized press");
    }
    public static void remountAfterReleaseTogglesAgain() {
        ControlLatch latch = running(0);
        latch.reset();
        check(latch.accept(1, ENGINE) && !latch.consumeToggle(1), "held key stays disarmed");
        check(latch.accept(2, FORWARD), "driver lets the engine key go");
        check(latch.accept(3, FORWARD | ENGINE), "deliberate press while driving");
        check(latch.consumeToggle(3), "a deliberate press after remount is honored");
        check(latch.keys(3) == (FORWARD | ENGINE), "driving bits are unchanged by the intent rule");
    }
    public static void awaitingReleaseDoesNotBlockDrivingOrBrake() {
        ControlLatch latch = running(0);
        latch.reset();
        int driving = FORWARD | LEFT | ENGINE;
        check(latch.accept(1, driving), "driving input accepted while disarmed");
        check(latch.keys(1) == driving, "throttle and steering are never suppressed");
        check(!latch.consumeToggle(1), "but the held engine key is still disarmed");
        check(latch.accept(2, BACK | BRAKE), "reverse and brake accepted");
        check(latch.keys(2) == (BACK | BRAKE), "brake and reverse survive");
        check(!latch.awaitingRelease(), "a packet without the engine bit rearms it");
    }
    public static void staleInputStillBrakesAndDiscardsToggle() {
        ControlLatch latch = new ControlLatch();
        check(latch.accept(10, FORWARD | ENGINE), "driver input accepted");
        check(latch.fresh(10 + TIMEOUT) && !latch.fresh(10 + TIMEOUT + 1), "timeout window unchanged");
        check(latch.keys(10 + TIMEOUT + 1) == BRAKE, "stale input brakes");
        check(!latch.consumeToggle(10 + TIMEOUT + 1), "a stale toggle is dropped, not queued");
        check(latch.accept(10 + TIMEOUT + 2, ENGINE), "input resumes");
        check(!latch.consumeToggle(10 + TIMEOUT + 2), "the dropped toggle does not come back");
    }
    public static void invalidAndReplayedPacketsCannotArmTheEngine() {
        ControlLatch latch = new ControlLatch();
        check(latch.accept(5, FORWARD), "baseline packet");
        for (int bits : new int[]{64, 96, 127, 128, 255, Integer.MIN_VALUE, -1}) {
            check(!latch.accept(6, bits), "bits outside the mask are rejected");
            check(!latch.consumeToggle(6), "a rejected packet cannot toggle");
        }
        check(!latch.accept(5, ENGINE) && !latch.accept(4, ENGINE) && !latch.accept(-1, ENGINE),
            "replayed, reordered and negative ticks are rejected");
        check(!latch.consumeToggle(5), "rejected replays leave no intent");
        check(latch.keys(5) == FORWARD, "the last valid snapshot is untouched");
        check(latch.accept(6, ENGINE) && latch.consumeToggle(6), "a valid press still works");
    }
    public static void separateSeatsDoNotShareEngineIntent() {
        ControlLatch first = running(0), second = running(0);
        first.reset();
        check(first.awaitingRelease() && !second.awaitingRelease(), "latches keep independent state");
        check(first.accept(1, ENGINE) && !first.consumeToggle(1), "cleared seat stays disarmed");
        check(second.accept(1, 0) && second.accept(2, ENGINE) && second.consumeToggle(2),
            "the untouched seat is unaffected");
        check(!first.consumeToggle(1), "no intent leaks between vehicles");
    }
    public static void legacyControlLatchExpectationsPreserved() {
        ControlLatch a = new ControlLatch();
        check(a.accept(50, 1) && !a.accept(50, 2) && !a.accept(49, 2), "one packet per server tick");
        check(a.keys(50) == 1 && a.accept(51, 2), "legacy packet ordering");
        ControlLatch b = new ControlLatch();
        check(b.accept(0, 32) && b.consumeToggle(0), "legacy first engine press");
        for (int i = 1; i < 100; i++) {
            b.accept(i, 32);
            check(!b.consumeToggle(i), "legacy held key toggles only once");
        }
        b.accept(100, 0);
        b.accept(101, 32);
        check(b.consumeToggle(101) && !b.consumeToggle(101), "legacy release and press");
        ControlLatch c = new ControlLatch();
        c.accept(10, 33);
        check(c.fresh(20) && !c.fresh(21) && c.keys(21) == 16 && !c.consumeToggle(21), "legacy stale behavior");
        ControlLatch d = new ControlLatch();
        d.accept(0, 1);
        for (int bits : new int[]{64, 127, 128, 255, -1}) check(!d.accept(9, bits), "legacy invalid bits");
        check(!d.fresh(11), "legacy invalid bits cannot refresh the timeout");
        ControlLatch e = new ControlLatch();
        e.accept(0, 33);
        e.reset();
        check(!e.fresh(0) && !e.consumeToggle(0) && e.keys(0) == 16, "legacy reset cannot leak commands");
    }
    public static long runAll() {
        checks = 0;
        firstPressAfterMountStillStartsEngine();
        heldEngineKeyCannotToggleAfterControlTimeout();
        repeatedTimeoutsCannotAccumulateIntent();
        releaseAfterTimeoutRestoresEnginePress();
        remountWithHeldKeyCannotStartEngine();
        remountAfterReleaseTogglesAgain();
        awaitingReleaseDoesNotBlockDrivingOrBrake();
        staleInputStillBrakesAndDiscardsToggle();
        invalidAndReplayedPacketsCannotArmTheEngine();
        separateSeatsDoNotShareEngineIntent();
        legacyControlLatchExpectationsPreserved();
        return checks;
    }
}
