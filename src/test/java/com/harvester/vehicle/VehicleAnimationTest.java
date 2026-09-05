package com.harvester.vehicle;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class VehicleAnimationTest {
    @Test void smoothingIsFrameRateIndependentAndBounded() {
        double once=VehicleAnimation.smooth(0,4,.22,1);
        double halves=VehicleAnimation.smooth(VehicleAnimation.smooth(0,4,.22,.5),4,.22,.5);
        assertEquals(once,halves,1e-9);
        assertTrue(once>0 && once<4);
        assertEquals(1,VehicleAnimation.smooth(1,4,.22,0),1e-9);
    }
    @Test void wheelRotationUsesSignedDistanceAndRadius() {
        assertEquals(.5,VehicleAnimation.signedDistance(0,.5,0),1e-9);
        assertEquals(-.5,VehicleAnimation.signedDistance(0,-.5,0),1e-9);
        assertEquals(.5,VehicleAnimation.signedDistance(-.5,0,90),1e-9);
        assertEquals(1,VehicleAnimation.wheelPhase(.5,.5),1e-6);
        assertEquals(-1,VehicleAnimation.wheelPhase(-.5,.5),1e-6);
    }
    @Test void hubAndTireHaveIdenticalRollingRadius() {
        for(VehicleType type:VehicleType.values()) {
            var parts=VehicleGeometry.create(type);
            for(var hub:parts) if(hub.name().startsWith("wheel_hub_")) {
                var tire=parts.stream().filter(p->p.name().equals(hub.name().replace("wheel_hub_","wheel_"))).findFirst().orElseThrow();
                assertEquals(VehicleAnimation.wheelRadius(tire,parts),VehicleAnimation.wheelRadius(hub,parts),1e-9);
                assertTrue(VehicleAnimation.wheelRadius(tire,parts)>.1);
            }
        }
    }
    @Test void rotorPhaseDoesNotDependOnFrameSubdivision() {
        var a=new VehicleAnimation(); var b=new VehicleAnimation();
        a.update(0,0,0,0,true,true,VehicleType.Family.HELICOPTER);
        b.update(0,0,0,0,true,true,VehicleType.Family.HELICOPTER);
        var full=a.update(1,0,0,0,true,true,VehicleType.Family.HELICOPTER);
        b.update(.5,0,0,0,true,true,VehicleType.Family.HELICOPTER);
        var half=b.update(1,0,0,0,true,true,VehicleType.Family.HELICOPTER);
        assertEquals(full.engineRotor(),half.engineRotor(),1e-6);
    }
    @Test void longerFramesUseTheSameResponseForRotorSpeedAndPhase() {
        var a=new VehicleAnimation(); var b=new VehicleAnimation();
        a.update(0,0,0,0,true,true,VehicleType.Family.HELICOPTER);
        b.update(0,0,0,0,true,true,VehicleType.Family.HELICOPTER);
        var full=a.update(4,0,0,0,true,false,VehicleType.Family.HELICOPTER);
        b.update(2,0,0,0,true,false,VehicleType.Family.HELICOPTER);
        var halves=b.update(4,0,0,0,true,false,VehicleType.Family.HELICOPTER);
        assertEquals(full.engineRotor(),halves.engineRotor(),1e-6);
        assertEquals(full.headerLift(),halves.headerLift(),1e-6);
    }
    @Test void stoppedRotorsSettleAndHeaderDoesNotJump() {
        var a=new VehicleAnimation();
        a.update(0,0,0,0,true,true,VehicleType.Family.DRONE);
        var raised=a.update(1,0,0,0,true,false,VehicleType.Family.DRONE);
        assertTrue(raised.headerLift()>0 && raised.headerLift()<4);
        for(int t=2;t<=200;t++) a.update(t,0,0,0,false,false,VehicleType.Family.DRONE);
        var before=a.update(201,0,0,0,false,false,VehicleType.Family.DRONE);
        var after=a.update(202,0,0,0,false,false,VehicleType.Family.DRONE);
        assertEquals(before.engineRotor(),after.engineRotor(),1e-5);
        assertEquals(4,after.headerLift(),1e-5);
    }
    @Test void historiesAreIndependentAndTeleportDoesNotSpinWheels() {
        var a=new VehicleAnimation(); var b=new VehicleAnimation();
        a.update(0,0,0,0,true,true,VehicleType.Family.PICKUP);
        b.update(0,0,0,0,false,true,VehicleType.Family.PICKUP);
        var moved=a.update(1,0,.3,0,true,true,VehicleType.Family.PICKUP);
        assertTrue(moved.wheelTravel()>0);
        assertEquals(0,b.update(1,0,0,0,false,true,VehicleType.Family.PICKUP).wheelTravel());
        assertEquals(moved.wheelTravel(),a.update(2,100,100,0,true,true,VehicleType.Family.PICKUP).wheelTravel());
        assertEquals(-1,VehicleAnimation.rotorDirection("rotor_-1_-1"));
        assertEquals(1,VehicleAnimation.rotorDirection("rotor_-1_1"));
    }
}
