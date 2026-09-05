package com.harvester.vehicle;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class VehiclePhysicsTest {
    private static VehiclePhysics.Motion rest() { return new VehiclePhysics.Motion(0, 0, 0, 0, 0); }
    private static VehiclePhysics.Flight fly(VehiclePhysics.Motion m, boolean vertical, boolean powered,
                                            boolean grounded, boolean flooded, boolean fresh,
                                            int keys, float yaw, float pitch) {
        return VehiclePhysics.flight(m, vertical, powered, grounded, flooded, fresh, keys, yaw, pitch, .42);
    }
    @Test void shortestYawAndPitchAreBounded() {
        assertEquals(-179, VehiclePhysics.approachAngle(179, -179, 3), 1e-5);
        var f = fly(rest(), true, true, false, false, true, 1, 90, -90);
        assertEquals(3, f.motion().yaw(), 1e-5);
        assertEquals(-2, f.motion().pitch(), 1e-5);
        assertTrue(f.motion().x() < 0);
        assertTrue(f.motion().z() > 0);
        assertTrue(f.motion().y() > 0);
    }
    @Test void invalidLookCannotInjectThrustOrAngles() {
        for (float invalid : new float[]{Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY}) {
            var f = fly(rest(), true, true, true, false, true, 17, invalid, 0);
            assertFalse(f.engineActive());
            assertEquals(0, f.motion().x());
            assertEquals(0, f.motion().z());
            assertTrue(Float.isFinite(f.motion().yaw()));
        }
    }
    @Test void backwardsKeyBrakesWithoutReversing() {
        var m = new VehiclePhysics.Motion(0, 0, .3, 0, 0);
        for (int i = 0; i < 100; i++) {
            m = fly(m, false, true, false, false, true, 2, 0, 0).motion();
            assertTrue(m.z() >= 0);
        }
        assertTrue(m.z() < .001);
    }
    @Test void planeNeedsRunwayAndCannotHoverWithoutThrust() {
        assertTrue(fly(rest(), false, true, true, false, true, 17, 0, -30).motion().y() <= 0);
        var m = new VehiclePhysics.Motion(0, 0, .3, 0, 0);
        assertTrue(fly(m, false, true, true, false, true, 17, 0, -30).motion().y() > 0);
        assertTrue(fly(m, false, true, false, false, true, 0, 0, -30).motion().y() < 0);
    }
    @Test void verticalHoverConsumesFuelButParkedVehicleDoesNot() {
        var hover = fly(rest(), true, true, false, false, true, 0, 0, 0);
        assertEquals(0, hover.motion().y());
        assertTrue(hover.engineActive());
        assertTrue(VehiclePhysics.spendsMovementFuel(true, false, hover.engineActive()));
        var parked = fly(rest(), true, true, true, false, true, 0, 0, 0);
        assertFalse(parked.engineActive());
        assertFalse(VehiclePhysics.spendsMovementFuel(true, false, parked.engineActive()));
        assertFalse(VehiclePhysics.spendsMovementFuel(false, true, true));
    }
    @Test void floodedFlightIgnoresEveryInputCombination() {
        for (boolean vertical : new boolean[]{false, true}) for (int keys = 0; keys < 64; keys++) {
            var f = fly(rest(), vertical, true, false, true, true, keys, 90, -45);
            assertFalse(f.engineActive());
            assertEquals(0, f.motion().x());
            assertEquals(0, f.motion().z());
            assertTrue(f.motion().y() < 0);
            assertEquals(0, f.motion().yaw());
        }
        assertFalse(VehiclePhysics.flooded(false, .05, .4, 1));
        assertTrue(VehiclePhysics.flooded(false, .5, .4, 1));
        assertTrue(VehiclePhysics.flooded(true, .3, .4, 1));
        assertFalse(VehiclePhysics.flooded(true, .1, .4, 1));
        assertFalse(VehiclePhysics.flooded(false, 1, .4, 1.0 / 9));
    }
    @Test void emptyTankAndStaleInputCannotPowerTakeoff() {
        assertFalse(fly(rest(), true, false, true, false, true, 17, 90, -45).engineActive());
        assertFalse(fly(rest(), true, true, true, false, false, 17, 90, -45).engineActive());
        assertEquals(0, VehiclePhysics.usableKeys(63, 11, true));
        assertEquals(0, VehiclePhysics.usableKeys(63, 0, false));
        assertEquals(63, VehiclePhysics.usableKeys(255, 10, true));
        // Timeout in the air fails safely to powered hover, not stale forward thrust.
        var timeout = fly(rest(), true, true, false, false, false, 17, 90, -45);
        assertEquals(0, timeout.motion().z());
        assertEquals(0, timeout.motion().y());
        assertTrue(timeout.engineActive());
    }
    @Test void buoyancyConvergesFromAboveAndBelow() {
        for (double start : new double[]{-.6, -.2, .2, .6}) {
            double y = start, vy = 0;
            for (int i = 0; i < 300; i++) {
                vy = VehiclePhysics.buoyancy(vy, -y); y += vy;
                assertTrue(Double.isFinite(y));
                assertTrue(vy <= .08 && vy >= -.12);
                assertTrue(Math.abs(y) <= .65);
            }
            assertEquals(0, y, 1e-6);
            assertEquals(0, vy, 1e-6);
        }
    }
    @Test void boatOneDryTickDoesNotStopItButLandCannotAccelerate() {
        var moving = new VehiclePhysics.Motion(0, 0, .3, 0, 0);
        var gap = VehiclePhysics.boat(moving, 0, 1, true, false, true, false, 0, .44);
        assertTrue(gap.motion().z() > .25);
        var dry = VehiclePhysics.boat(rest(), 0, 1, true, false, false, true, 0, .44);
        assertEquals(0, dry.motion().z());
        var beached = VehiclePhysics.boat(moving, 0, 1, true, false, false, true, 0, .44);
        assertTrue(beached.motion().z() < moving.z());
    }
    @Test void boatSidewaysDragTurnAndReverseAreLimited() {
        var m = new VehiclePhysics.Motion(.3, 0, .1, 0, 0);
        var result = VehiclePhysics.boat(m, 0, 0, true, true, true, false, 0, .44);
        assertTrue(result.motion().x() < .21);
        assertTrue(result.motion().z() > .09);
        m = rest(); double turn = 0;
        for (int i = 0; i < 400; i++) {
            result = VehiclePhysics.boat(m, turn, 10, true, true, true, false, 0, .44);
            m = result.motion(); turn = result.turnRate();
            assertTrue(Math.abs(turn) <= 2.3);
            assertTrue(Math.hypot(m.x(), m.z()) <= .44 * .35 + 1e-6);
        }
    }
    @Test void longFlightStaysFiniteAndSpeedLimited() {
        for (boolean vertical : new boolean[]{false, true}) {
            var m = rest();
            for (int i = 0; i < 2000; i++) {
                m = fly(m, vertical, true, false, false, true, i % 64, i * 17f, i % 180 - 90).motion();
                assertTrue(Double.isFinite(m.x()) && Double.isFinite(m.y()) && Double.isFinite(m.z()));
                assertTrue(Math.hypot(m.x(), m.z()) <= .420001);
                assertTrue(m.y() >= -.18 && m.y() <= .14);
                assertTrue(Math.abs(m.pitch()) <= 45);
            }
        }
    }
}
