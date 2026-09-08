package com.prokstudio.militaryvehicles;
import com.prokstudio.militaryvehicles.core.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class FleetPhysicsTest {
    @Test void eachVehicleReachesItsOwnForwardAndReverseLimit() {
        for(var k:VehicleKind.values()) for(int keys:new int[]{ControlLatch.FORWARD,ControlLatch.BACK}) {
            double speed=0;
            for(int tick=0;tick<300;tick++) speed=TruckPhysics.step(k,speed,0,keys,true,true,1).speed();
            assertEquals(keys==ControlLatch.FORWARD?k.handling.forward():-k.handling.reverse(),speed,1e-9);
        }
    }
    @Test void brakingAndReverseSteeringWorkForEveryProfile() {
        for(var k:VehicleKind.values()) {
            double speed=k.handling.forward();
            for(int tick=0;tick<100;tick++) speed=TruckPhysics.step(k,speed,0,ControlLatch.FORWARD|ControlLatch.BRAKE,true,true,1).speed();
            assertEquals(0,speed);
            assertTrue(TruckPhysics.step(k,k.handling.forward(),0,ControlLatch.RIGHT,true,true,1).yaw()>0);
            assertTrue(TruckPhysics.step(k,-k.handling.reverse(),0,ControlLatch.RIGHT,true,true,1).yaw()<0);
            assertEquals(0,TruckPhysics.step(k,0,0,ControlLatch.RIGHT,true,true,1).yaw());
        }
    }
    @Test void noPowerAirAndInvalidNumbersCannotCreateAcceleration() {
        for(var k:VehicleKind.values()) {
            for(boolean powered:new boolean[]{false,true}) for(boolean ground:new boolean[]{false,true}) {
                if(powered&&ground) continue;
                var m=TruckPhysics.step(k,0,0,ControlLatch.FORWARD|ControlLatch.RIGHT,powered,ground,1);
                assertEquals(0,m.speed());assertEquals(0,m.steer());
            }
            for(double invalid:new double[]{Double.NaN,Double.POSITIVE_INFINITY,Double.NEGATIVE_INFINITY}) {
                assertEquals(new TruckPhysics.Motion(0,0,0),TruckPhysics.step(k,invalid,10,1,true,true,1));
                assertEquals(new TruckPhysics.Motion(0,0,0),TruckPhysics.step(k,.1,10,1,true,true,invalid));
            }
        }
    }
    @Test void fuelTransferAndAudioNormalizationFollowSelectedProfile() {
        for(var k:VehicleKind.values()) {
            assertEquals(11,TruckPhysics.transferFuel(k,k.tank-11,600));assertEquals(0,TruckPhysics.transferFuel(k,k.tank+1,600));
            assertEquals(0,TruckPhysics.transferFuel(k,-1,600));assertEquals(0,TruckPhysics.transferFuel(k,0,-1));
            var forward=new EngineFeedback();var reverse=new EngineFeedback();
            EngineFeedback.Frame a=null,b=null;
            for(int i=0;i<150;i++) { a=forward.update(k,true,1,k.handling.forward());b=reverse.update(k,true,1,-k.handling.forward()); }
            assertEquals(a.pitch(),b.pitch());assertEquals(a.volume(),b.volume());assertTrue(a.pitch()>1);
        }
    }
}
