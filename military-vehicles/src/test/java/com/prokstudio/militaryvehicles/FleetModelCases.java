package com.prokstudio.militaryvehicles;

import com.prokstudio.militaryvehicles.core.ExpansionGeometry;
import com.prokstudio.militaryvehicles.core.VehicleKind;
import com.prokstudio.militaryvehicles.core.TruckGeometry.Box;
import com.prokstudio.militaryvehicles.core.TruckGeometry.Part;
import java.util.*;

/** Standalone model-completeness regressions for the tracked chassis. Geometry only: no Minecraft runtime and no in-game QA. */
public final class FleetModelCases {
    private static final float WHEEL_TOP=9.1f,UPPER_RUN=12.2f,TURRET_FLOOR=28,DECK_TOP=22,BAND_FRONT=31,BAND_REAR=-31;
    private static long checks;
    private static void check(boolean value,String context) { checks++;if(!value) throw new AssertionError(context); }
    private static List<Part> rig(VehicleKind kind) { return ExpansionGeometry.create(kind); }
    private static List<Part> group(List<Part> rig,String prefix) { return rig.stream().filter(p->p.name().startsWith(prefix)).toList(); }
    private static Part one(List<Part> rig,String name) {
        var found=rig.stream().filter(p->p.name().equals(name)).toList();
        check(found.size()==1,"exactly one "+name);
        return found.getFirst();
    }
    private static List<VehicleKind> tracked() { return List.of(VehicleKind.TANK,VehicleKind.HOWITZER); }

    public static void trackedRunningGearIsComplete() {
        for(var kind:tracked()) {
            var rig=rig(kind);
            check(group(rig,"sprocket_teeth_").size()==2,"two driven sprockets "+kind);
            check(group(rig,"idler_rim_").size()==2,"two idlers "+kind);
            check(group(rig,"return_roller_").size()==6,"six return rollers "+kind);
            check(group(rig,"track_tensioner_").size()==2,"two tensioners "+kind);
            check(group(rig,"track_scraper_").size()==2,"two scrapers "+kind);
            check(group(rig,"spare_track_links_").size()==2,"spare links on both sides "+kind);
            check(group(rig,"hull_stowage_box_").size()==2,"stowage on both sides "+kind);
            check(one(rig,"rear_tow_lugs").boxes().size()==2,"paired rear tow lugs "+kind);
            check(one(rig,"rear_stowage_bins").boxes().size()==2,"paired rear bins "+kind);
            one(rig,"driver_hatch_handle");
            for(int side:new int[]{-1,1}) {
                var sprocket=one(rig,"sprocket_teeth_"+side);var idler=one(rig,"idler_rim_"+side);
                check(sprocket.z()<0&&idler.z()>0,"sprocket drives the engine end, idler tensions the front");
                check(Math.abs(sprocket.x())==23&&Math.abs(idler.x())==23,"rings sit on the track centreline");
                check(sprocket.boxes().size()==8&&idler.boxes().size()==6,"toothed sprocket and spoked idler");
                for(var ring:List.of(sprocket,idler)) check(ring.wheel()&&!ring.front()&&ring.material().equals("metal"),"rings roll but never steer");
                for(var roller:group(rig,"return_roller_"+side+"_")) check(roller.wheel()&&roller.y()>WHEEL_TOP,"roller carries the upper run");
            }
        }
    }
    public static void rotatingHardwareStaysAboveGroundThroughFullTurn() {
        for(var kind:tracked()) for(var part:rig(kind)) if(part.wheel())
            for(int frame=0;frame<64;frame++) for(var box:part.boxes())
                for(float x:new float[]{box.x(),box.x()+box.w()}) for(float y:new float[]{box.y(),box.y()+box.h()}) for(float z:new float[]{box.z(),box.z()+box.d()}) {
                    double spin=frame*Math.PI/32;
                    double ry=y*Math.cos(spin)-z*Math.sin(spin),rz=y*Math.sin(spin)+z*Math.cos(spin);
                    check(part.y()+ry>=-1e-5,"never digs into the ground: "+kind+" "+part.name());
                    check(part.y()+ry<=kind.height*16+1e-5,"inside the height box: "+kind+" "+part.name());
                    check(Math.hypot(part.x()+x,part.z()+rz)<=kind.width*8+1e-5,"inside the yaw collider: "+kind+" "+part.name());
                }
    }
    public static void staticHardwareFitsTheCollider() {
        for(var kind:VehicleKind.values()) if(kind.tracked()||kind.support()) for(var part:rig(kind)) {
            check(!part.boxes().isEmpty(),"no empty part "+part.name());
            for(var box:part.boxes()) {
                check(box.w()>0&&box.h()>0&&box.d()>0,"positive cuboid "+part.name());
                for(float x:new float[]{part.x()+box.x(),part.x()+box.x()+box.w()}) for(float y:new float[]{part.y()+box.y(),part.y()+box.y()+box.h()}) for(float z:new float[]{part.z()+box.z(),part.z()+box.z()+box.d()}) {
                    check(y>=-1e-5&&y/16<=kind.height,"static height "+kind+" "+part.name());
                    check(Math.hypot(x,z)/16<=kind.width/2,"static collider "+kind+" "+part.name());
                }
            }
        }
    }
    public static void newRingsDidNotTurnIntoRoadWheels() {
        for(var kind:tracked()) {
            var rig=rig(kind);
            var tires=rig.stream().filter(p->p.wheel()&&p.material().equals("rubber")).toList();
            check(tires.size()==kind.wheels,"road wheel count unchanged "+kind);
            check(tires.stream().noneMatch(Part::front),"tracks never steer "+kind);
            for(var tire:tires) {
                check(tire.y()==kind.wheelRadius,"rolling radius "+kind);
                check(rig.stream().anyMatch(p->p.wheel()&&p.material().equals("metal")&&p.x()==tire.x()&&p.y()==tire.y()&&p.z()==tire.z()&&p.front()==tire.front()),"hub at every road wheel "+kind);
            }
            check(rig.stream().filter(p->p.wheel()&&p.material().equals("metal")).count()>=24,"hubs plus sprockets, idlers and rollers "+kind);
            Set<String> names=new HashSet<>();
            for(var part:rig) check(names.add(part.name()),"unique part name "+part.name());
        }
    }
    public static void newHardwareKeepsItsClearances() {
        for(var kind:tracked()) {
            var rig=rig(kind);
            for(var roller:group(rig,"return_roller_")) {
                Box box=roller.boxes().getFirst();
                check(roller.y()+box.y()>WHEEL_TOP,"roller clears the road wheels "+kind);
                check(roller.y()+box.y()+box.h()<UPPER_RUN,"roller stays under the upper run "+kind);
            }
            for(int side:new int[]{-1,1}) {
                Box tensioner=one(rig,"track_tensioner_"+side).boxes().getFirst();
                check(tensioner.z()>=BAND_FRONT,"tensioner mounts outside the band "+kind);
                Box scraper=one(rig,"track_scraper_"+side).boxes().getFirst();
                check(scraper.z()+scraper.d()<=BAND_REAR,"scraper mounts behind the band "+kind);
                Box stowage=one(rig,"hull_stowage_box_"+side).boxes().getFirst();
                check(stowage.y()+stowage.h()<=TURRET_FLOOR,"stowage stays under the turret sweep "+kind);
                Box links=one(rig,"spare_track_links_"+side).boxes().getFirst();
                check(links.y()>=DECK_TOP,"spare links rest above the hull deck "+kind);
            }
            Box lugs=one(rig,"rear_tow_lugs").boxes().getFirst();
            check(lugs.z()<one(rig,"front_tow_lugs").boxes().getFirst().z(),"recovery points at both ends "+kind);
        }
    }
    public static void armedRigsKeepSeparateMissionHardware() {
        var tank=rig(VehicleKind.TANK);var gun=rig(VehicleKind.HOWITZER);
        one(tank,"turret_armor");one(tank,"turret_antenna");one(tank,"turret_optics");
        check(tank.stream().noneMatch(p->p.name().equals("barrel_travel_lock")),"turret tank needs no travel lock");
        one(gun,"turret_shield");one(gun,"barrel_travel_lock");
        check(group(gun,"outrigger_leg_").size()==4,"four outrigger legs");
        for(var part:List.of(one(gun,"turret_shield_optics"),one(gun,"turret_shield_antenna")))
            check(part.y()==ExpansionGeometry.TURRET_Y&&part.z()==ExpansionGeometry.TURRET_Z&&part.name().startsWith("turret_"),"shield hardware traverses with the mount");
        check(!tank.equals(gun),"armed rigs stay distinct");
        for(var kind:List.of(VehicleKind.TANKER,VehicleKind.WORKSHOP,VehicleKind.RECOVERY)) {
            var rig=rig(kind);
            check(group(rig,"sprocket_teeth_").isEmpty()&&group(rig,"return_roller_").isEmpty(),"no track gear on wheeled bodies "+kind);
            check(rig.stream().filter(p->p.wheel()&&p.material().equals("rubber")).count()==kind.wheels,"service wheel count "+kind);
        }
    }
    public static long runAll() {
        checks=0;
        trackedRunningGearIsComplete();rotatingHardwareStaysAboveGroundThroughFullTurn();staticHardwareFitsTheCollider();
        newRingsDidNotTurnIntoRoadWheels();newHardwareKeepsItsClearances();armedRigsKeepSeparateMissionHardware();
        return checks;
    }
}
