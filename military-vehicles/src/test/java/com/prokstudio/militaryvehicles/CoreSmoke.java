package com.prokstudio.militaryvehicles;
import com.prokstudio.militaryvehicles.core.*;
import java.util.*;
/** Standalone assertions, not a count of JUnit cases or game scenarios. */
public final class CoreSmoke {
    private static int checks;
    private static void check(boolean condition) { checks++;if(!condition) throw new AssertionError("assertion "+checks); }
    public static void main(String[] args) {
        for(int keys=0;keys<=63;keys++) for(boolean powered:new boolean[]{false,true}) {
            double speed=0;float yaw=179;
            for(int tick=0;tick<200;tick++) {
                var m=TruckPhysics.step(speed,yaw,keys,powered,true,1);speed=m.speed();yaw=m.yaw();
                check(Double.isFinite(speed)&&speed>=-TruckSpec.MAX_REVERSE&&speed<=TruckSpec.MAX_FORWARD);
                check(Float.isFinite(yaw)&&yaw>=-180&&yaw<180);
            }
        }
        for(int tank=0;tank<=TruckSpec.TANK;tank+=7) for(int can=0;can<=TruckSpec.CAN_FUEL;can+=11) {
            int moved=TruckPhysics.transferFuel(tank,can);check(moved>=0&&moved<=can&&tank+moved<=TruckSpec.TANK);
            check(tank+can==(tank+moved)+(can-moved));
        }
        ControlLatch c=new ControlLatch();check(c.accept(100,32));check(c.consumeToggle(100));check(!c.accept(100,0));check(!c.consumeToggle(100));
        check(c.accept(101,32));check(!c.consumeToggle(101));check(c.keys(112)==16);check(!c.fresh(112));check(!c.accept(113,128));check(c.accept(113,0));
        Set<String> names=new HashSet<>();int wheels=0;
        for(var p:TruckGeometry.create()) {
            check(names.add(p.name()));check(TruckGeometry.MATERIALS.containsKey(p.material()));
            if(p.wheel()&&!p.name().startsWith("wheel_hub")) wheels++;
            for(var b:p.boxes()) {
                check(b.w()>0&&b.h()>0&&b.d()>0);
                for(float x:new float[]{p.x()+b.x(),p.x()+b.x()+b.w()}) for(float z:new float[]{p.z()+b.z(),p.z()+b.z()+b.d()}) check(Math.hypot(x,z)/16<=TruckSpec.WIDTH/2.0+.001);
                check(p.y()+b.y()>=-.001&&(p.y()+b.y()+b.h())/16<=TruckSpec.HEIGHT);
            }
        }
        check(wheels==6);System.out.println("CORE_SMOKE_PASS assertions="+checks);
    }
}
