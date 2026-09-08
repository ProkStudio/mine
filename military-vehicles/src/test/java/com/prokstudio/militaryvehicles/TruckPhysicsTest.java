package com.prokstudio.militaryvehicles;
import com.prokstudio.militaryvehicles.core.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class TruckPhysicsTest {
    @Test void boundsEveryInputCombination() {
        for(int keys=0;keys<=63;keys++) { double speed=0;float yaw=0;
            for(int i=0;i<500;i++) { var m=TruckPhysics.step(speed,yaw,keys,true,true,1);speed=m.speed();yaw=m.yaw();assertTrue(speed>=-TruckSpec.MAX_REVERSE&&speed<=TruckSpec.MAX_FORWARD);assertTrue(yaw>=-180&&yaw<180); }
        }
    }
    @Test void reverseIsSlower() {
        double f=0,r=0;for(int i=0;i<100;i++) { f=TruckPhysics.step(f,0,1,true,true,1).speed();r=TruckPhysics.step(r,0,2,true,true,1).speed(); }
        assertEquals(TruckSpec.MAX_FORWARD,f,1e-9);assertEquals(-TruckSpec.MAX_REVERSE,r,1e-9);
    }
    @Test void oppositeDirectionBrakesThroughZero() {
        double speed=.28;for(int i=0;i<12;i++) { speed=TruckPhysics.step(speed,0,2,true,true,1).speed();assertTrue(speed>=0); }
        assertEquals(0,speed,1e-9);assertTrue(TruckPhysics.step(speed,0,2,true,true,1).speed()<0);
    }
    @Test void brakeOverridesThrottle() { double speed=.28;for(int i=0;i<10;i++) speed=TruckPhysics.step(speed,0,17,true,true,1).speed();assertEquals(0,speed); }
    @Test void cannotPivotOrAccelerateInAir() {
        assertEquals(0,TruckPhysics.step(0,0,8,true,true,1).yaw());var m=TruckPhysics.step(0,0,9,true,false,1);assertEquals(0,m.speed());assertEquals(0,m.yaw());
    }
    @Test void reverseReversesYawResponse() { assertTrue(TruckPhysics.step(.15,0,8,true,true,1).yaw()>0);assertTrue(TruckPhysics.step(-.09,0,8,true,true,1).yaw()<0); }
    @Test void invalidNumbersStopSafely() {
        for(double v:new double[]{Double.NaN,Double.NEGATIVE_INFINITY,Double.POSITIVE_INFINITY}) assertEquals(0,TruckPhysics.step(v,0,1,true,true,1).speed());
        assertEquals(0,TruckPhysics.step(.1,Float.NaN,1,true,true,1).speed());
    }
    @Test void fuelTransferConservesBothStores() {
        for(int tank=0;tank<=2400;tank+=11) for(int can=0;can<=600;can+=7) { int moved=TruckPhysics.transferFuel(tank,can);assertTrue(moved>=0&&moved<=can&&tank+moved<=2400);assertEquals(tank+can,(tank+moved)+(can-moved)); }
        assertEquals(0,TruckPhysics.transferFuel(2400,600));
    }
}
