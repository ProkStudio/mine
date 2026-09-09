package com.prokstudio.militaryvehicles;

import java.util.*;
import com.prokstudio.militaryvehicles.core.TruckGeometry.Box;
import com.prokstudio.militaryvehicles.core.TruckGeometry.Part;
import com.prokstudio.militaryvehicles.core.VehicleGeometry;
import com.prokstudio.militaryvehicles.core.VehicleKind;

/** Standalone geometry checks for the running gear and crew hardware of the wheeled rigs. */
public final class WheeledModelCases {
    private static final float EPS=1e-4f;
    private static final List<String> BUGGY_AXLES=List.of("front","rear");
    private static final List<String> CARRIER_AXLES=List.of("front","mid_front","mid_rear","rear");
    private static final int[] SIDES={-1,1};
    private static final VehicleKind[] WHEELED={VehicleKind.BUGGY,VehicleKind.CARRIER};
    private int checks;

    private static List<String> kit(VehicleKind kind) {
        List<String> names=new ArrayList<>();
        if(kind==VehicleKind.BUGGY) {
            for(String axle:BUGGY_AXLES) for(int side:SIDES) {
                names.add("a_arm_lower_"+axle+"_"+side);
                names.add("a_arm_upper_"+axle+"_"+side);
                names.add("bump_stop_"+axle+"_"+side);
            }
            names.add("track_rod");
            for(int side:SIDES) names.add("harness_anchor_"+side);
            names.add("rack_fuel_can");names.add("first_aid_box");
        } else {
            for(String axle:CARRIER_AXLES) for(int side:SIDES) {
                names.add("swing_arm_"+axle+"_"+side);
                names.add("torsion_bar_"+axle+"_"+side);
                names.add("damper_"+axle+"_"+side);
            }
            for(int side:SIDES) {
                names.add("ramp_actuator_"+side);
                names.add("smoke_launcher_"+side);
                names.add("smoke_tubes_"+side);
            }
            names.add("stowage_basket");names.add("antenna_mount");names.add("whip_antenna");
        }
        return names;
    }
    private void check(boolean ok,String message) {
        checks++;
        if(!ok) throw new AssertionError(message);
    }
    private static Box world(Part part,Box box) {
        return new Box(part.x()+box.x(),part.y()+box.y(),part.z()+box.z(),box.w(),box.h(),box.d());
    }
    private static boolean overlaps(Box a,Box c) {
        return Math.min(a.x()+a.w(),c.x()+c.w())-Math.max(a.x(),c.x())>EPS
            &&Math.min(a.y()+a.h(),c.y()+c.h())-Math.max(a.y(),c.y())>EPS
            &&Math.min(a.z()+a.d(),c.z()+c.d())-Math.max(a.z(),c.z())>EPS;
    }
    private static float rollingRadius(Part wheel) {
        double radius=0;
        for(Box box:wheel.boxes()) for(float y:new float[]{box.y(),box.y()+box.h()}) for(float z:new float[]{box.z(),box.z()+box.d()})
            radius=Math.max(radius,Math.hypot(y,z));
        return (float)radius;
    }
    private static float gap(float centre,float lo,float hi) {
        return (float)Math.max(0,Math.max(centre-hi,lo-centre));
    }

    private void bothWheeledRigsCarryTheChassisKit() {
        Set<String> everything=new HashSet<>(kit(VehicleKind.BUGGY));everything.addAll(kit(VehicleKind.CARRIER));
        for(VehicleKind kind:WHEELED) {
            var rig=VehicleGeometry.create(kind);var own=kit(kind);
            Set<String> names=new HashSet<>();
            for(Part part:rig) check(names.add(part.name()),"duplicate part "+part.name()+" on "+kind);
            for(String name:own) check(names.contains(name),kind+" is missing "+name);
            for(Part part:rig) {
                if(!own.contains(part.name())) continue;
                check(!part.wheel(),part.name()+" must not roll with the wheels on "+kind);
                check(VehicleGeometry.MATERIALS.containsKey(part.material()),part.name()+" uses unknown material "+part.material());
                check(!part.boxes().isEmpty(),part.name()+" has no boxes on "+kind);
            }
            for(Part part:rig) if(!own.contains(part.name()))
                check(!everything.contains(part.name()),kind+" reuses the other rig's part "+part.name());
        }
        for(VehicleKind kind:VehicleKind.values()) {
            if(kind==VehicleKind.BUGGY||kind==VehicleKind.CARRIER) continue;
            for(Part part:VehicleGeometry.create(kind))
                check(!everything.contains(part.name()),kind+" must not carry the wheeled chassis part "+part.name());
        }
    }
    private void chassisKitStaysInsideTheCollider() {
        for(VehicleKind kind:WHEELED) {
            float top=kind.height*16,reach=kind.width*16/2;
            var own=kit(kind);
            for(Part part:VehicleGeometry.create(kind)) {
                if(!own.contains(part.name())) continue;
                for(Box box:part.boxes()) {
                    Box w=world(part,box);
                    check(w.y()>=-EPS,part.name()+" sinks under the ground on "+kind);
                    check(w.y()+w.h()<=top+EPS,part.name()+" pokes through the roof of "+kind);
                    for(float x:new float[]{w.x(),w.x()+w.w()}) for(float z:new float[]{w.z(),w.z()+w.d()})
                        check(Math.hypot(x,z)<=reach+EPS,part.name()+" pokes out of the side of "+kind);
                }
            }
        }
    }
    private void chassisKitNeverGrowsIntoTheBodywork() {
        for(VehicleKind kind:WHEELED) {
            var own=kit(kind);List<Box> mine=new ArrayList<>();List<Box> body=new ArrayList<>();
            for(Part part:VehicleGeometry.create(kind)) for(Box box:part.boxes()) {
                if(own.contains(part.name())) mine.add(world(part,box));
                else if(!part.wheel()) body.add(world(part,box));
            }
            check(mine.size()>=17,kind+" lost part of the chassis kit");
            check(body.size()>=40,kind+" lost part of its bodywork");
            for(int i=0;i<mine.size();i++) {
                for(int j=i+1;j<mine.size();j++)
                    check(!overlaps(mine.get(i),mine.get(j)),kind+" chassis kit boxes overlap each other");
                for(Box other:body) check(!overlaps(mine.get(i),other),kind+" chassis kit grows into the bodywork");
            }
        }
    }
    private void chassisKitClearsTheRollingWheels() {
        for(VehicleKind kind:WHEELED) {
            var rig=VehicleGeometry.create(kind);var own=kit(kind);
            for(Part wheel:rig) {
                if(!wheel.wheel()) continue;
                float radius=rollingRadius(wheel),lo=Float.MAX_VALUE,hi=-Float.MAX_VALUE;
                for(Box box:wheel.boxes()) {Box w=world(wheel,box);lo=Math.min(lo,w.x());hi=Math.max(hi,w.x()+w.w());}
                for(Part part:rig) {
                    if(!own.contains(part.name())) continue;
                    for(Box box:part.boxes()) {
                        Box w=world(part,box);
                        if(w.x()+w.w()<=lo+EPS||w.x()>=hi-EPS) continue;
                        float dy=gap(wheel.y(),w.y(),w.y()+w.h()),dz=gap(wheel.z(),w.z(),w.z()+w.d());
                        check(Math.hypot(dy,dz)>=radius-1e-3f,kind+" chassis kit box fouls the swept circle of "+wheel.name());
                    }
                }
            }
        }
    }
    private void runningGearMatchesTheChassisIntent() {
        for(Part part:VehicleGeometry.create(VehicleKind.BUGGY)) for(Box box:part.boxes()) {
            Box w=world(part,box);String name=part.name();
            if(name.startsWith("a_arm_lower_")) check(w.y()+w.h()<=5+EPS,name+" rides up into the axle beam");
            if(name.startsWith("a_arm_upper_")) check(w.y()>=10-EPS,name+" hangs below the tube frame");
            if(name.startsWith("a_arm_")||name.equals("track_rod"))
                check(Math.abs(w.x())<=10+EPS&&Math.abs(w.x()+w.w())<=10+EPS,name+" reaches into the tyres of the buggy");
        }
        int arms=0,bars=0,dampers=0,tyres=0,steered=0;
        for(Part part:VehicleGeometry.create(VehicleKind.CARRIER)) {
            String name=part.name();
            if(name.startsWith("swing_arm_")) arms++;
            if(name.startsWith("torsion_bar_")) bars++;
            if(name.startsWith("damper_")) dampers++;
            if(part.wheel()&&part.material().equals("rubber")) {tyres++;if(part.front()) steered++;}
            for(Box box:part.boxes()) {
                Box w=world(part,box);
                if(name.startsWith("swing_arm_")||name.startsWith("torsion_bar_")||name.startsWith("damper_"))
                    check(Math.abs(w.x())<=16+EPS&&Math.abs(w.x()+w.w())<=16+EPS,name+" reaches into the tyres of the carrier");
                if(name.startsWith("torsion_bar_")) check(w.y()>=8-EPS&&w.y()+w.h()<=9+EPS,name+" left its mounting height");
            }
        }
        check(arms==8,"the carrier needs one swing arm per wheel station, found "+arms);
        check(bars==8,"the carrier needs one torsion bar per wheel station, found "+bars);
        check(dampers==8,"the carrier needs one damper per wheel station, found "+dampers);
        check(tyres==VehicleKind.CARRIER.wheels,"the carrier lost a tyre: "+tyres);
        check(steered==VehicleKind.CARRIER.steeringWheels,"the carrier lost a steering tyre: "+steered);
    }
    public int runAll() {
        bothWheeledRigsCarryTheChassisKit();
        chassisKitStaysInsideTheCollider();
        chassisKitNeverGrowsIntoTheBodywork();
        chassisKitClearsTheRollingWheels();
        runningGearMatchesTheChassisIntent();
        return checks;
    }
}
