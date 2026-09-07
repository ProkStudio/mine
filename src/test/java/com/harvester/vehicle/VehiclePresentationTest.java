package com.harvester.vehicle;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class VehiclePresentationTest {
    private static VehiclePresentation.Input input(double t,VehicleType type,boolean engine,boolean working,boolean occupied,boolean ground,double speed,double throttle,double steer,double fuel,double condition) {
        return new VehiclePresentation.Input(t,type,engine,working,occupied,ground,speed,throttle,steer,.4,.2,.1,fuel,condition);
    }
    @Test void filtersAreFrameRateIndependent() {
        var a=new VehiclePresentation();var b=new VehiclePresentation();
        var initial=input(0,VehicleType.PICKUP,false,false,true,true,0,0,0,1,1);a.update(initial);b.update(initial);
        var full=a.update(input(1,VehicleType.PICKUP,true,false,true,true,.2,1,1,.8,1));
        b.update(input(.5,VehicleType.PICKUP,true,false,true,true,.2,1,1,.8,1));
        var halves=b.update(input(1,VehicleType.PICKUP,true,false,true,true,.2,1,1,.8,1));
        assertEquals(full.rpm(),halves.rpm(),1e-6);assertEquals(full.throttle(),halves.throttle(),1e-6);
        assertEquals(full.steering(),halves.steering(),1e-6);assertEquals(full.fuelNeedle(),halves.fuelNeedle(),1e-6);
    }
    @Test void phasesComeFromActualDrivingWorkingAndFlightSignals() {
        assertEquals(VehiclePresentation.Phase.WORKING,new VehiclePresentation().update(input(0,VehicleType.COMBINE,true,true,true,true,.1,1,0,1,1)).phase());
        assertEquals(VehiclePresentation.Phase.BRAKING,new VehiclePresentation().update(input(0,VehicleType.PICKUP,true,false,true,true,.1,-1,0,1,1)).phase());
        assertEquals(VehiclePresentation.Phase.REVERSING,new VehiclePresentation().update(input(0,VehicleType.PICKUP,true,false,true,true,-.1,-1,0,1,1)).phase());
        assertEquals(VehiclePresentation.Phase.HOVERING,new VehiclePresentation().update(input(0,VehicleType.HELICOPTER,true,false,true,false,0,0,0,1,1)).phase());
        assertEquals(VehiclePresentation.Phase.FLYING,new VehiclePresentation().update(input(0,VehicleType.PLANE,true,false,true,false,.3,1,0,1,1)).phase());
    }
    @Test void standRetractsForOccupiedMovingOrAirborneBikes() {
        assertEquals(1,new VehiclePresentation().update(input(0,VehicleType.MOTORCYCLE,false,false,false,true,0,0,0,1,1)).parkingStand());
        for(var sample:new VehiclePresentation.Input[]{input(0,VehicleType.MOTORCYCLE,false,false,true,true,0,0,0,1,1),input(0,VehicleType.MOTORCYCLE,false,false,false,false,0,0,0,1,1),input(0,VehicleType.MOTORCYCLE,true,false,false,true,.2,1,0,1,1)})
            assertEquals(0,new VehiclePresentation().update(sample).parkingStand());
    }
    @Test void gaugesReflectFuelAndSpeedRatherThanDecorativeOscillation() {
        var frame=new VehiclePresentation().update(input(0,VehicleType.PICKUP,true,false,true,true,.16,1,0,.75,1));
        assertEquals(1.05,frame.fuelNeedle(),1e-6);assertEquals(0,frame.speedNeedle(),1e-6);
        assertEquals(VehiclePresentation.Phase.EMPTY_TANK,new VehiclePresentation().update(input(0,VehicleType.PLANE,true,false,true,false,.3,1,0,0,1)).phase());
        assertEquals(VehiclePresentation.Phase.BROKEN,new VehiclePresentation().update(input(0,VehicleType.PLANE,true,false,true,false,.3,1,0,1,0)).phase());
    }
    @Test void historiesAreIndependentAndResetAfterTypeChangeOrTimeJump() {
        var a=new VehiclePresentation();var b=new VehiclePresentation();
        a.update(input(0,VehicleType.PICKUP,true,false,true,true,.3,1,1,1,1));
        assertEquals(0,b.update(input(0,VehicleType.PICKUP,false,false,false,true,0,0,0,1,1)).rpm());
        assertEquals(0,a.update(input(20,VehicleType.DRONE,false,false,false,true,0,0,0,1,1)).rpm());
        assertEquals(0,a.update(input(-10,VehicleType.DRONE,false,false,false,true,0,0,0,1,1)).rpm());
    }
    @Test void invalidTelemetryNeverProducesNonFiniteOrUnboundedTransforms() {
        var frame=new VehiclePresentation().update(new VehiclePresentation.Input(Double.NaN,VehicleType.PLANE,true,true,true,false,Double.POSITIVE_INFINITY,Double.NaN,9,Double.NaN,50,-50,Double.NaN,Double.NaN));
        for(float value:new float[]{frame.rpm(),frame.throttle(),frame.steering(),frame.elevator(),frame.rudder(),frame.fuelNeedle(),frame.speedNeedle(),frame.rpmNeedle(),frame.parkingStand(),frame.gimbalPitch(),frame.swashPitch(),frame.swashRoll()}) assertTrue(Float.isFinite(value));
        assertTrue(Math.abs(frame.elevator())<=.321);assertTrue(Math.abs(frame.rudder())<=.351);
        assertTrue(Math.abs(frame.swashPitch())<=.131);assertTrue(Math.abs(frame.swashRoll())<=.131);
    }
    @Test void shutdownSettlesAndNoDecorativeMotorRunsWithoutPower() {
        var a=new VehiclePresentation();a.update(input(0,VehicleType.PLANE,true,false,true,false,.3,1,0,1,1));
        VehiclePresentation.Frame frame=VehiclePresentation.Frame.REST;
        for(int i=1;i<=200;i++) frame=a.update(input(i,VehicleType.PLANE,false,false,false,true,0,0,0,1,1));
        assertEquals(0,frame.rpm(),1e-6);assertEquals(VehiclePresentation.Phase.PARKED,frame.phase());
    }
}
