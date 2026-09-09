package com.prokstudio.militaryvehicles;

import java.util.*;
import com.prokstudio.militaryvehicles.core.TruckGeometry.Box;
import com.prokstudio.militaryvehicles.core.TruckGeometry.Part;
import com.prokstudio.militaryvehicles.core.VehicleGeometry;
import com.prokstudio.militaryvehicles.core.VehicleKind;

/** Standalone geometry checks for the troop bay fit-out and the ramp boarding aids of the carrier. */
public final class TroopBayModelCases {
    private static final float EPS=1e-4f;
    private static final int[] SIDES={-1,1};
    private static final VehicleKind CARRIER=VehicleKind.CARRIER;
    private static final float FLOOR=15,ROOF=43,BUMPER=10,RAMP_HALF_WIDTH=11,TAIL_LIGHT_TOP=20;
    private int checks;

    private static List<String> kit() {
        List<String> names=new ArrayList<>(List.of("bay_walkway","bay_first_aid_box",
            "bay_extinguisher_bracket","bay_extinguisher","ramp_step","ramp_step_arms"));
        for(int side:SIDES) {
            names.add("troop_grab_rail_"+side);
            names.add("bay_light_housing_"+side);
            names.add("bay_dome_light_"+side);
            names.add("bay_kit_rack_"+side);
            names.add("ramp_grab_handle_"+side);
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
    private static float outerX(Box box) {
        return Math.max(Math.abs(box.x()),Math.abs(box.x()+box.w()));
    }
    private static float innerX(Box box) {
        return box.x()<=0&&box.x()+box.w()>=0?0:Math.min(Math.abs(box.x()),Math.abs(box.x()+box.w()));
    }

    private void onlyTheCarrierCarriesTheTroopBayKit() {
        var own=kit();var rig=VehicleGeometry.create(CARRIER);
        Set<String> names=new HashSet<>();
        for(Part part:rig) check(names.add(part.name()),"duplicate part "+part.name()+" on the carrier");
        for(String name:own) check(names.contains(name),"the carrier is missing "+name);
        for(Part part:rig) {
            if(!own.contains(part.name())) continue;
            check(!part.wheel(),part.name()+" must not roll with the wheels");
            check(VehicleGeometry.MATERIALS.containsKey(part.material()),part.name()+" uses unknown material "+part.material());
            check(!part.boxes().isEmpty(),part.name()+" has no boxes");
        }
        for(VehicleKind kind:VehicleKind.values()) {
            if(kind==CARRIER) continue;
            for(Part part:VehicleGeometry.create(kind))
                check(!own.contains(part.name()),kind+" must not carry the troop bay part "+part.name());
        }
    }
    private void troopBayKitStaysInsideTheCollider() {
        float top=CARRIER.height*16,reach=CARRIER.width*16/2;
        var own=kit();
        for(Part part:VehicleGeometry.create(CARRIER)) {
            if(!own.contains(part.name())) continue;
            for(Box box:part.boxes()) {
                Box w=world(part,box);
                check(w.y()>=-EPS,part.name()+" sinks under the ground");
                check(w.y()+w.h()<=top+EPS,part.name()+" pokes through the roof of the carrier");
                for(float x:new float[]{w.x(),w.x()+w.w()}) for(float z:new float[]{w.z(),w.z()+w.d()})
                    check(Math.hypot(x,z)<=reach+EPS,part.name()+" pokes out of the hull of the carrier");
            }
        }
    }
    private void troopBayKitNeverGrowsIntoTheHull() {
        var own=kit();List<Box> mine=new ArrayList<>();List<Box> body=new ArrayList<>();
        for(Part part:VehicleGeometry.create(CARRIER)) for(Box box:part.boxes()) {
            if(own.contains(part.name())) mine.add(world(part,box));
            else if(!part.wheel()) body.add(world(part,box));
        }
        check(mine.size()>=23,"the troop bay lost part of its fit-out");
        check(body.size()>=60,"the carrier lost part of its hull");
        for(int i=0;i<mine.size();i++) {
            for(int j=i+1;j<mine.size();j++)
                check(!overlaps(mine.get(i),mine.get(j)),"troop bay boxes overlap each other");
            for(Box other:body) check(!overlaps(mine.get(i),other),"the troop bay grows into the hull");
        }
    }
    private void troopBayKitClearsTheRollingWheels() {
        var rig=VehicleGeometry.create(CARRIER);var own=kit();
        for(Part wheel:rig) {
            if(!wheel.wheel()) continue;
            float radius=rollingRadius(wheel),lo=Float.MAX_VALUE,hi=-Float.MAX_VALUE;
            for(Box box:wheel.boxes()) {Box w=world(wheel,box);lo=Math.min(lo,w.x());hi=Math.max(hi,w.x()+w.w());}
            for(Part part:rig) {
                if(!own.contains(part.name())) continue;
                for(Box box:part.boxes()) {
                    Box w=world(part,box);
                    boolean inboard=w.x()+w.w()<=lo+EPS||w.x()>=hi-EPS;
                    float dy=gap(wheel.y(),w.y(),w.y()+w.h()),dz=gap(wheel.z(),w.z(),w.z()+w.d());
                    check(inboard||Math.hypot(dy,dz)>=radius-1e-3f,part.name()+" fouls the swept circle of "+wheel.name());
                }
            }
        }
    }
    private void troopBayMatchesTheBoardingIntent() {
        float cushion=Float.MAX_VALUE,backTop=-Float.MAX_VALUE;
        for(var seat:CARRIER.seats) {cushion=Math.min(cushion,seat.topY()-3);backTop=Math.max(backTop,seat.topY()+9);}
        var own=kit();int rails=0,housings=0,lights=0,racks=0,handles=0;Box walkway=null;
        for(Part part:VehicleGeometry.create(CARRIER)) {
            String name=part.name();
            if(!own.contains(name)) continue;
            if(name.startsWith("troop_grab_rail_")) rails++;
            if(name.startsWith("bay_light_housing_")) housings++;
            if(name.startsWith("bay_dome_light_")) lights++;
            if(name.startsWith("bay_kit_rack_")) racks++;
            if(name.startsWith("ramp_grab_handle_")) handles++;
            for(Box box:part.boxes()) {
                Box w=world(part,box);
                if(name.equals("bay_walkway")) {
                    walkway=w;
                    check(w.y()>=FLOOR-EPS,"the gangway sinks into the hull floor");
                    check(w.y()+w.h()<=cushion+EPS,"the gangway rises above the seat cushions");
                    check(w.w()>=10-EPS,"the gangway is too narrow to board through");
                }
                if(name.startsWith("troop_grab_rail_")) {
                    check(w.y()>=backTop-EPS,"the ceiling rail hangs into the seat backs");
                    check(w.y()+w.h()<=ROOF+EPS,"the ceiling rail pushes through the roof");
                    check(outerX(w)<=15+EPS,"the ceiling rail reaches into the side hull");
                }
                if(name.startsWith("bay_light_housing_")) {
                    check(w.y()+w.h()<=ROOF+EPS,"the bay light housing pushes through the roof");
                    check(w.y()>=backTop-EPS,"the bay light housing hangs into the seat backs");
                }
                if(name.startsWith("bay_dome_light_")) {
                    check(w.y()+w.h()<=ROOF-1+EPS,"the dome light sticks into its own housing");
                    check(w.y()>=backTop-EPS,"the dome light hangs into the seat backs");
                }
                if(name.startsWith("bay_kit_rack_")) {
                    check(w.y()>=cushion-EPS,"the kit rack blocks the seat cushions");
                    check(w.y()+w.h()<=41+EPS,"the kit rack blocks the ceiling rail");
                    check(outerX(w)<=15+EPS,"the kit rack reaches into the side hull");
                }
                if(name.equals("bay_first_aid_box")||name.startsWith("bay_extinguisher")) {
                    check(w.y()>=cushion-EPS,"the bay stores block the seat cushions");
                    check(w.z()+w.d()<=-28+EPS,"the bay stores block the rear seat row");
                    check(w.z()>=-33+EPS,"the bay stores sink into the rear bulkhead");
                    check(outerX(w)<=15+EPS,"the bay stores reach into the side hull");
                }
                if(name.equals("ramp_step")) {
                    check(w.y()>=-EPS,"the ramp step digs into the ground");
                    check(w.y()+w.h()<=BUMPER+EPS,"the ramp step fouls the rear bumper");
                    check(w.z()+w.d()<=-35+EPS,"the ramp step hides under the hull instead of behind the ramp");
                }
                if(name.equals("ramp_step_arms")) {
                    check(w.y()>=8-EPS,"the ramp step arms hang below the step");
                    check(w.y()+w.h()<=BUMPER+EPS,"the ramp step arms foul the rear bumper");
                }
                if(name.startsWith("ramp_grab_handle_")) {
                    check(w.z()+w.d()<=-35+EPS,"the ramp handle sits inside the troop bay");
                    check(w.y()>=TAIL_LIGHT_TOP-EPS,"the ramp handle covers the tail lights");
                    check(w.y()+w.h()<=ROOF-9+EPS,"the ramp handle climbs out of reach");
                    check(innerX(w)>=RAMP_HALF_WIDTH-EPS,"the ramp handle blocks the ramp opening");
                }
            }
        }
        check(rails==2,"the troop bay needs one ceiling rail per side, found "+rails);
        check(housings==2,"the troop bay needs one light housing per side, found "+housings);
        check(lights==2,"the troop bay needs one dome light per side, found "+lights);
        check(racks==2,"the troop bay needs one kit rack per side, found "+racks);
        check(handles==2,"the ramp needs one grab handle per side, found "+handles);
        check(walkway!=null,"the troop bay lost its gangway");
        for(var seat:CARRIER.seats)
            check(walkway.z()<=seat.z()+EPS&&walkway.z()+walkway.d()>=seat.z()-EPS,"the gangway does not reach the seat row at z="+seat.z());
    }
    public int runAll() {
        onlyTheCarrierCarriesTheTroopBayKit();
        troopBayKitStaysInsideTheCollider();
        troopBayKitNeverGrowsIntoTheHull();
        troopBayKitClearsTheRollingWheels();
        troopBayMatchesTheBoardingIntent();
        return checks;
    }
}
