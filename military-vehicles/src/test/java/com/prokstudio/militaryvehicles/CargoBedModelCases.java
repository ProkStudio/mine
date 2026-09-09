package com.prokstudio.militaryvehicles;

import java.util.*;
import com.prokstudio.militaryvehicles.core.TruckGeometry;
import com.prokstudio.militaryvehicles.core.TruckGeometry.Box;
import com.prokstudio.militaryvehicles.core.TruckGeometry.Part;
import com.prokstudio.militaryvehicles.core.VehicleGeometry;
import com.prokstudio.militaryvehicles.core.VehicleKind;

/** Standalone geometry checks for the convertible cargo bed of the base truck. */
public final class CargoBedModelCases {
    private static final float EPS=1e-4f;
    private static final List<String> KIT;
    static {
        List<String> names=new ArrayList<>();
        for(int i=0;i<4;i++) names.add("bed_tilt_hoop_"+i);
        names.add("bed_canvas_cradles");
        names.add("bed_canvas_roll");
        for(int side:new int[]{-1,1}) { names.add("bed_bench_"+side);names.add("bed_lashing_rings_"+side); }
        names.add("tailgate_panel");
        names.add("tailgate_latches");
        names.add("tailgate_step");
        KIT=List.copyOf(names);
    }
    private int checks;

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
    private static Map<String,Part> byName(List<Part> rig) {
        Map<String,Part> parts=new LinkedHashMap<>();
        for(Part part:rig) parts.put(part.name(),part);
        return parts;
    }
    private static Box span(Part part) {
        float x0=Float.MAX_VALUE,y0=Float.MAX_VALUE,z0=Float.MAX_VALUE,x1=-Float.MAX_VALUE,y1=-Float.MAX_VALUE,z1=-Float.MAX_VALUE;
        for(Box box:part.boxes()) {
            Box w=world(part,box);
            x0=Math.min(x0,w.x());y0=Math.min(y0,w.y());z0=Math.min(z0,w.z());
            x1=Math.max(x1,w.x()+w.w());y1=Math.max(y1,w.y()+w.h());z1=Math.max(z1,w.z()+w.d());
        }
        return new Box(x0,y0,z0,x1-x0,y1-y0,z1-z0);
    }

    private void theTruckCarriesTheCargoBedKit() {
        var rig=VehicleGeometry.create(VehicleKind.TRUCK);
        check(rig.equals(TruckGeometry.create()),"the truck rig must still come straight from TruckGeometry");
        Set<String> names=new HashSet<>();
        for(Part part:rig) check(names.add(part.name()),"duplicate part "+part.name()+" on TRUCK");
        for(String name:KIT) check(names.contains(name),"TRUCK is missing "+name);
        for(Part part:rig) {
            if(!KIT.contains(part.name())) continue;
            check(!part.wheel(),part.name()+" must not roll with the wheels");
            check(TruckGeometry.MATERIALS.containsKey(part.material()),part.name()+" uses unknown material "+part.material());
            check(!part.boxes().isEmpty(),part.name()+" has no boxes");
        }
        for(VehicleKind kind:VehicleKind.values()) {
            if(kind==VehicleKind.TRUCK) continue;
            for(Part part:VehicleGeometry.create(kind))
                check(!KIT.contains(part.name()),kind+" must not keep the cargo bed part "+part.name());
        }
    }
    private void cargoBedKitStaysInsideTheCollider() {
        float top=VehicleKind.TRUCK.height*16,reach=VehicleKind.TRUCK.width*16/2;
        for(Part part:VehicleGeometry.create(VehicleKind.TRUCK)) {
            if(!KIT.contains(part.name())) continue;
            for(Box box:part.boxes()) {
                Box w=world(part,box);
                check(w.y()>=-EPS,part.name()+" sinks under the ground");
                check(w.y()+w.h()<=top+EPS,part.name()+" pokes through the truck roof line");
                for(float x:new float[]{w.x(),w.x()+w.w()}) for(float z:new float[]{w.z(),w.z()+w.d()})
                    check(Math.hypot(x,z)<=reach+EPS,part.name()+" pokes out of the truck collider");
            }
        }
    }
    private void cargoBedKitNeverGrowsIntoTheTruckBody() {
        List<Box> mine=new ArrayList<>();List<Box> body=new ArrayList<>();
        for(Part part:VehicleGeometry.create(VehicleKind.TRUCK)) for(Box box:part.boxes()) {
            if(KIT.contains(part.name())) mine.add(world(part,box));
            else if(!part.wheel()) body.add(world(part,box));
        }
        check(mine.size()>=33,"the cargo bed kit lost boxes");
        check(body.size()>=40,"the truck lost part of its bodywork");
        for(int i=0;i<mine.size();i++) {
            for(int j=i+1;j<mine.size();j++) check(!overlaps(mine.get(i),mine.get(j)),"cargo bed kit boxes overlap each other");
            for(Box other:body) check(!overlaps(mine.get(i),other),"cargo bed kit grows into the truck body");
        }
    }
    private void cargoBedKitClearsTheRollingWheels() {
        var rig=VehicleGeometry.create(VehicleKind.TRUCK);
        for(Part wheel:rig) {
            if(!wheel.wheel()) continue;
            float radius=rollingRadius(wheel),lo=Float.MAX_VALUE,hi=-Float.MAX_VALUE;
            for(Box box:wheel.boxes()) {Box w=world(wheel,box);lo=Math.min(lo,w.x());hi=Math.max(hi,w.x()+w.w());}
            for(Part part:rig) {
                if(!KIT.contains(part.name())) continue;
                for(Box box:part.boxes()) {
                    Box w=world(part,box);
                    if(w.x()+w.w()<=lo+EPS||w.x()>=hi-EPS) continue;
                    float dy=gap(wheel.y(),w.y(),w.y()+w.h()),dz=gap(wheel.z(),w.z(),w.z()+w.d());
                    check(Math.hypot(dy,dz)>=radius-1e-3f,"cargo bed kit box fouls the swept circle of "+wheel.name());
                }
            }
        }
    }
    private void tiltFrameMatchesTheCargoBedIntent() {
        var rig=VehicleGeometry.create(VehicleKind.TRUCK);
        Map<String,Part> parts=byName(rig);
        Box floor=span(parts.get("bed_floor")),rails=span(parts.get("bed_top_rails")),walls=span(parts.get("bed_sides"));
        float deck=floor.y()+floor.h(),railTop=rails.y()+rails.h();
        int hoops=0,benches=0;
        for(Part part:rig) {
            String name=part.name();
            if(name.startsWith("bed_tilt_hoop_")) {
                hoops++;
                check(part.boxes().size()==3,name+" needs two posts and a cross beam");
                for(Box box:part.boxes()) {
                    Box w=world(part,box);
                    check(w.y()>=railTop-EPS,name+" cuts into the bed top rails");
                    check(Math.abs(w.x())<=16.4f+EPS&&Math.abs(w.x()+w.w())<=16.4f+EPS,name+" hangs outside the bed walls");
                }
                Box beam=world(part,part.boxes().get(2));
                check(beam.y()>=railTop+12-EPS,name+" sags towards the cargo deck");
                check(beam.w()>=30,name+" does not span the bed");
            }
            if(name.startsWith("bed_bench_")) {
                benches++;
                Box plank=world(part,part.boxes().get(0));
                check(plank.y()>=deck-EPS,name+" sinks into the cargo deck");
                check(plank.d()>=30,name+" is too short to seat a squad");
                check(Math.abs(plank.x())<=15+EPS&&Math.abs(plank.x()+plank.w())<=15+EPS,name+" reaches through the bed walls");
                for(int i=1;i<part.boxes().size();i++) {
                    Box leg=world(part,part.boxes().get(i));
                    check(Math.abs(leg.y()+leg.h()-plank.y())<=EPS,name+" leg does not reach the plank");
                    check(leg.y()>=deck-EPS,name+" leg sinks into the cargo deck");
                }
            }
        }
        check(hoops==4,"the tilt frame needs four hoops, found "+hoops);
        check(benches==2,"the bed needs a bench on each side, found "+benches);
        Part rollPart=parts.get("bed_canvas_roll"),cradlePart=parts.get("bed_canvas_cradles");
        Box roll=world(rollPart,rollPart.boxes().get(0));
        check(rollPart.material().equals("canvas"),"the rolled tilt must be canvas");
        check(roll.y()>=railTop+12-EPS,"the canvas roll hangs below the tilt line");
        for(Box box:cradlePart.boxes()) {
            Box w=world(cradlePart,box);
            check(Math.abs(w.y()+w.h()-roll.y())<=EPS,"the canvas roll does not rest on its cradle");
            check(w.x()>=roll.x()-EPS&&w.x()+w.w()<=roll.x()+roll.w()+EPS,"a canvas cradle sticks out past the roll");
        }
        Part panelPart=parts.get("tailgate_panel");
        Box panel=world(panelPart,panelPart.boxes().get(0)),hinges=span(parts.get("tailgate_hinges"));
        check(panel.z()+panel.d()<=walls.z()+EPS,"the tailgate is buried in the rear wall");
        check(panel.y()>=hinges.y()+hinges.h()-EPS,"the tailgate does not hang from its hinges");
        check(panel.w()>=28,"the tailgate does not close the bed");
        Box latches=span(parts.get("tailgate_latches")),step=span(parts.get("tailgate_step")),bumper=span(parts.get("rear_bumper"));
        check(latches.z()+latches.d()<=panel.z()+EPS,"the latches are sunk into the tailgate");
        check(step.y()+step.h()<=bumper.y()+EPS,"the rear step blocks the bumper");
        check(step.y()>=0,"the rear step sinks under the ground");
        for(int side:new int[]{-1,1}) {
            Box rings=span(parts.get("bed_lashing_rings_"+side));
            check(rings.y()>=deck-EPS&&rings.y()+rings.h()<=railTop+EPS,"lashing rings on side "+side+" left the bed walls");
            check(Math.abs(rings.x())<=15+EPS&&Math.abs(rings.x()+rings.w())<=15+EPS,"lashing rings on side "+side+" poke through the bed walls");
        }
    }
    public int runAll() {
        theTruckCarriesTheCargoBedKit();
        cargoBedKitStaysInsideTheCollider();
        cargoBedKitNeverGrowsIntoTheTruckBody();
        cargoBedKitClearsTheRollingWheels();
        tiltFrameMatchesTheCargoBedIntent();
        return checks;
    }
}
