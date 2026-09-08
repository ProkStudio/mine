package com.prokstudio.militaryvehicles;
import com.prokstudio.militaryvehicles.core.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class ControlLatchTest {
    @Test void onlyOnePacketPerServerTick() { var c=new ControlLatch();assertTrue(c.accept(50,1));assertFalse(c.accept(50,2));assertFalse(c.accept(49,2));assertEquals(1,c.keys(50));assertTrue(c.accept(51,2)); }
    @Test void heldEngineKeyTogglesOnlyOnce() {
        var c=new ControlLatch();assertTrue(c.accept(0,32));assertTrue(c.consumeToggle(0));
        for(int i=1;i<100;i++) { c.accept(i,32);assertFalse(c.consumeToggle(i)); }c.accept(100,0);c.accept(101,32);assertTrue(c.consumeToggle(101));assertFalse(c.consumeToggle(101));
    }
    @Test void staleKeysBrakeAndDiscardToggle() { var c=new ControlLatch();c.accept(10,33);assertTrue(c.fresh(20));assertFalse(c.fresh(21));assertEquals(16,c.keys(21));assertFalse(c.consumeToggle(21)); }
    @Test void invalidBitsCannotRefreshTimeout() { var c=new ControlLatch();c.accept(0,1);for(int bits:new int[]{64,127,128,255,-1}) assertFalse(c.accept(9,bits));assertFalse(c.fresh(11)); }
    @Test void resetCannotLeakDriverCommands() { var c=new ControlLatch();c.accept(0,33);c.reset();assertFalse(c.fresh(0));assertFalse(c.consumeToggle(0));assertEquals(16,c.keys(0)); }
}
