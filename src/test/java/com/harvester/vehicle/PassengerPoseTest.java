package com.harvester.vehicle;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PassengerPoseTest {
    @Test void cameraFullTurnDoesNotRotateBody() {
        for (int camera = -720; camera <= 720; camera++) {
            var pose = PassengerPose.facing(30, camera, 0);
            assertEquals(30, pose.bodyYaw(), 1e-5);
            assertTrue(Math.abs(pose.headYaw()) <= 65);
        }
    }
    @Test void headAnglesAreRelativeBoundedAndFinite() {
        assertEquals(2, PassengerPose.facing(179, -179, 0).headYaw(), 1e-5);
        assertEquals(-2, PassengerPose.facing(-179, 179, 0).headYaw(), 1e-5);
        assertEquals(-35, PassengerPose.facing(0, 0, -90).headPitch(), 1e-5);
        assertEquals(45, PassengerPose.facing(0, 0, 90).headPitch(), 1e-5);
        var invalid = PassengerPose.facing(Float.NaN, Float.POSITIVE_INFINITY, Float.NaN);
        assertTrue(Float.isFinite(invalid.bodyYaw()));
        assertTrue(Float.isFinite(invalid.headYaw()));
        assertTrue(Float.isFinite(invalid.headPitch()));
    }
    @Test void everySeatHasFiniteLimbPose() {
        for (VehicleType type : VehicleType.values()) for (int seat = 0; seat < type.seats; seat++) {
            var pose = PassengerPose.limbs(type, seat);
            assertTrue(Float.isFinite(pose.armPitch()) && Float.isFinite(pose.legPitch()));
            assertTrue(pose.armPitch() >= -1.3f && pose.armPitch() <= 0);
            assertTrue(pose.legPitch() >= -1.6f && pose.legPitch() <= 0);
            assertTrue(pose.armInward() >= 0 && pose.armInward() <= .4f);
            assertTrue(pose.legSpread() >= 0 && pose.legSpread() <= .4f);
        }
    }
    @Test void motorcyclePassengerDoesNotHoldDriverHandlebars() {
        for (VehicleType type : new VehicleType[]{VehicleType.MOTORCYCLE, VehicleType.MOTORCYCLE_TOURING}) {
            assertNotEquals(PassengerPose.limbs(type, 0), PassengerPose.limbs(type, 1));
            assertTrue(PassengerPose.limbs(type, 1).armPitch() > PassengerPose.limbs(type, 0).armPitch());
        }
    }
    @Test void itemUseAndAttackKeepVanillaArms() {
        assertTrue(PassengerPose.keepVanillaArms(true, 0));
        assertTrue(PassengerPose.keepVanillaArms(false, .5f));
        assertFalse(PassengerPose.keepVanillaArms(false, 0));
    }
}
