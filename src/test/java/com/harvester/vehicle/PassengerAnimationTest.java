package com.harvester.vehicle;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class PassengerAnimationTest {
    private static PassengerAnimation.Input input(double t,VehicleType type,int seat,long id,double steer,double drive,double speed) {
        return new PassengerAnimation.Input(type,seat,id,t,steer,drive,speed,false);
    }
    @Test void allFamiliesAndSeatsProduceIndependentFiniteJoints() {
        for(var type:VehicleType.values()) for(int seat=0;seat<type.seats;seat++) {
            var animation=new PassengerAnimation();
            for(int tick=0;tick<160;tick++) {
                var p=animation.update(input(tick,type,seat,seat,Math.sin(tick*.1),Math.cos(tick*.07),Math.sin(tick*.03)*type.speed));
                for(var joint:List.of(p.rightArm(),p.leftArm(),p.rightLeg(),p.leftLeg())) {
                    assertTrue(Float.isFinite(joint.pitch()));assertTrue(Float.isFinite(joint.yaw()));assertTrue(Float.isFinite(joint.roll()));
                    assertTrue(Math.abs(joint.pitch())<1.7);assertTrue(Math.abs(joint.yaw())<=.851);assertTrue(Math.abs(joint.roll())<=.061);
                }
                assertTrue(Math.abs(p.leanPitch())<=.113);assertTrue(Math.abs(p.leanRoll())<=.041);
                assertTrue(p.weight()>=0 && p.weight()<=1);
            }
        }
    }
    @Test void driversRespondToSteeringAndPedalsInsteadOfOneFrozenPose() {
        for(var type:VehicleType.values()) {
            var animation=new PassengerAnimation();var neutral=animation.update(input(0,type,0,0,0,0,0));
            var turning=animation.update(input(1,type,0,0,1,1,.2));
            assertNotEquals(neutral.rightArm(),turning.rightArm(),type.name());
            if(type.family!=VehicleType.Family.MOTORCYCLE) assertNotEquals(neutral.rightLeg(),turning.rightLeg(),type.name());
        }
    }
    @Test void pillionUsesRearSupportAndNeverSteersTheHandlebars() {
        var animation=new PassengerAnimation();var neutral=animation.update(input(0,VehicleType.MOTORCYCLE,1,1,0,0,0));
        var turning=animation.update(input(1,VehicleType.MOTORCYCLE,1,1,1,1,.3));
        assertEquals(neutral.rightArm(),turning.rightArm());assertEquals(neutral.leftArm(),turning.leftArm());
        var driver=new PassengerAnimation().update(input(0,VehicleType.MOTORCYCLE,0,0,0,0,0));
        assertNotEquals(driver.rightArm(),neutral.rightArm());
    }
    @Test void entryBlendResetsOnSeatVehicleTypeOrTimeDiscontinuity() {
        var a=new PassengerAnimation();assertEquals(0,a.update(input(0,VehicleType.PICKUP,0,1,0,0,0)).weight());
        assertTrue(a.update(input(1,VehicleType.PICKUP,0,1,0,0,0)).weight()>0);
        assertEquals(0,a.update(input(2,VehicleType.PICKUP,0,2,0,0,0)).weight());
        assertEquals(0,a.update(input(3,VehicleType.MOTORCYCLE,1,2,0,0,0)).weight());
        assertEquals(0,a.update(input(20,VehicleType.MOTORCYCLE,1,2,0,0,0)).weight());
        assertEquals(0,a.update(input(-1,VehicleType.MOTORCYCLE,1,2,0,0,0)).weight());
    }
    @Test void controlAndEntrySmoothingAreFrameSubdivisionIndependent() {
        var a=new PassengerAnimation();var b=new PassengerAnimation();
        a.update(input(0,VehicleType.PICKUP,0,1,0,0,0));b.update(input(0,VehicleType.PICKUP,0,1,0,0,0));
        var full=a.update(input(1,VehicleType.PICKUP,0,1,1,1,0));
        b.update(input(.5,VehicleType.PICKUP,0,1,1,1,0));var halves=b.update(input(1,VehicleType.PICKUP,0,1,1,1,0));
        assertEquals(full.rightArm().pitch(),halves.rightArm().pitch(),1e-6);
        assertEquals(full.leftArm().yaw(),halves.leftArm().yaw(),1e-6);
        assertEquals(full.rightLeg().pitch(),halves.rightLeg().pitch(),1e-6);assertEquals(full.weight(),halves.weight(),1e-6);
    }
    @Test void neutralHandTargetsMatchTheModelControlPivots() {
        for(var type:VehicleType.values()) for(int side:new int[]{-1,1}) {
            var seat=VehicleGeometry.seat(type,0);var target=PassengerAnimation.grip(type,side,0,0);
            boolean lever=type.family==VehicleType.Family.DOZER || type.verticalAircraft();
            String name=lever?(side<0?"lever_left":"lever_right"):"steering";
            var part=VehicleGeometry.create(type).stream().filter(p->p.name().equals(name)).findFirst().orElseThrow();
            assertEquals(part.py()-seat.top()+(lever?3.5:0),target.y(),1e-6,type.name());
            assertEquals(part.pz()-seat.z(),target.z(),1e-6,type.name());
            assertEquals(lever?part.px():side*(type.family==VehicleType.Family.MOTORCYCLE?5:2.75),target.x(),1e-6,type.name());
        }
    }
    @Test void invalidInputIsBoundedAndLargeSeatIdentityIsNotTruncated() {
        var animation=new PassengerAnimation();long id=Long.MAX_VALUE-2;
        var p=animation.update(input(Double.NaN,VehicleType.PLANE,0,id,Double.NaN,Double.POSITIVE_INFINITY,Double.NEGATIVE_INFINITY));
        assertTrue(Float.isFinite(p.rightArm().pitch()));assertTrue(Float.isFinite(p.leanPitch()));
        assertTrue(animation.update(input(1,VehicleType.PLANE,0,id,0,0,0)).weight()>0);
        assertEquals(0,animation.update(input(2,VehicleType.PLANE,0,id+1,0,0,0)).weight());
        assertEquals(2,PassengerAnimation.blend(2,4,-1));assertEquals(4,PassengerAnimation.blend(2,4,2));
    }
    @Test void vanillaItemUseAndAttackHavePriorityOverTheControlLayer() {
        assertTrue(PassengerPose.keepVanillaArms(true,0));assertTrue(PassengerPose.keepVanillaArms(false,.5f));
        assertFalse(PassengerPose.keepVanillaArms(false,0));assertFalse(PassengerPose.keepVanillaArms(false,Float.NaN));
        assertEquals(1.2f,PassengerAnimation.blend(1.2f,-1,0));
    }
}
