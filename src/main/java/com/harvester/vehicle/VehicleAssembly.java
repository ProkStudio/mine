package com.harvester.vehicle;

import java.util.*;
import static com.harvester.vehicle.VehicleGeometry.*;

/** Mechanical attachment pass in UNSCALED blueprint pixels. No world or client dependencies. */
public final class VehicleAssembly {
    private final List<Part> parts=new ArrayList<>();
    private VehicleAssembly() {}
    private static Cuboid box(double x,double y,double z,double w,double h,double d) {
        if(!Double.isFinite(x+y+z+w+h+d) || w<=0 || h<=0 || d<=0) throw new IllegalArgumentException("Invalid attachment");
        return new Cuboid((float)x,(float)y,(float)z,(float)w,(float)h,(float)d);
    }
    private void part(String name,String material,double x,double y,double z,char axis,double pitch,Cuboid... boxes) {
        parts.add(new Part(name,material,(float)x,(float)y,(float)z,axis,List.of(boxes),(float)pitch,0,0));
    }
    private void fixed(String name,String material,Cuboid... boxes) { part(name,material,0,0,0,' ',0,boxes); }
    private void mirror(int side,double cabinHalf,double y,double z) {
        double inner=cabinHalf-.8,outer=cabinHalf+3.5;
        fixed("mirror_bracket_"+side,"metal",box(side<0?-outer:inner,y,z,outer-inner,.65,.8),box(side*cabinHalf-.45,y-1,z-.3,.9,2,1.4));
    }
    private void hydraulic(String kind,int side,double y,double z) {
        double length=Math.sqrt(128),offset=length*.5;
        part("hydraulic_"+kind+"_barrel_"+side,"dark",side*(kind.equals("header")?10:11),y,z,'d',45,box(-.8,-.8,0,1.6,1.6,length*.60));
        part("hydraulic_"+kind+"_piston_"+side,"metal",side*(kind.equals("header")?10:11),y,z,'d',45,box(-.36,-.36,offset,.72,.72,length-offset));
    }
    private void wipers(VehicleType type) {
        double y,z,x,length,pitch;
        switch(type.family) {
            case COMBINE -> { y=30.2;z=18.05;x=7.6;length=7.8;pitch=0; }
            case DOZER -> { y=24.1;z=7.05;x=6.8;length=6.8;pitch=0; }
            case PICKUP -> { y=20.4;z=13.72;x=6.1;length=6.2;pitch=-22.5; }
            case HELICOPTER -> { y=14.6;z=20.12;x=6.3;length=6.4;pitch=-22.5; }
            case BOAT -> { y=12;z=11.70;x=type.blueprintWidth*4;length=5.0;pitch=0; }
            default -> { return; }
        }
        for(int side:new int[]{-1,1}) {
            part("wiper_"+side,"rubber",side*x,y,z,'w',pitch,box(-.16,0,-.13,.32,length,.26),box(-.34,1.0,.08,.22,length-1,.30));
            fixed("wiper_mount_"+side,"metal",box(side*x-.65,y-.6,z-.65,1.3,1.1,.8));
        }
    }
    private void controls(VehicleType type) {
        Seat s=seat(type,0);boolean bike=type.family==VehicleType.Family.MOTORCYCLE;
        if(!bike) {
            double floor=s.top()-4.0;
            if(type.family==VehicleType.Family.BOAT) floor=6.6;
            if(type.family==VehicleType.Family.PLANE) floor=9;
            if(type.family==VehicleType.Family.HELICOPTER) floor=10;
            if(type.family==VehicleType.Family.DRONE) floor=12;
            if(type.family==VehicleType.Family.DOZER || type.verticalAircraft()) {
                for(int side:new int[]{-1,1}) fixed("lever_console_"+side,"dark",box(side*3-.75,floor,s.z()+6.25,1.5,s.top()+2-floor,1.5));
            } else {
                fixed("steering_pedestal","metal",box(-.5,floor,s.z()+8.8,1,s.top()+5-floor,1),box(-.5,s.top()+4.5,s.z()+7.8,1,1,2));
            }
            fixed("throttle_console","dark",box(-4.5,floor,s.z()+4.3,1.4,s.top()+2.5-floor,1.4));
            double forward=switch(type.family) {case PICKUP,PLANE,DRONE -> 9;case COMBINE -> 9.5;case DOZER -> 10.5;default -> 11;};
            double panelScale=type.family==VehicleType.Family.DRONE?.7:1;
            fixed("instrument_pedestal","dark",box(-3*panelScale,floor,s.z()+forward+.35,6*panelScale,s.top()+3.5-floor,1));
            for(int side:new int[]{-1,1}) {
                part(side<0?"pedal_drive":"pedal_brake","rubber",side*2.1,floor+.35,s.z()+9,'q',0,box(-.8,0,-1,1.6,.35,2));
                fixed("pedal_mount_"+side,"metal",box(side*2.1-.6,floor-.25,s.z()+8.3,1.2,.65,1.4));
            }
        }
    }
    public static List<Part> complete(VehicleType type,List<Part> original) {
        VehicleAssembly a=new VehicleAssembly();List<Cuboid> headerBolts=new ArrayList<>();
        for(Part p:original) {
            // Keep visible moving/fixed surfaces on distinct depth planes.
            if(type.family==VehicleType.Family.DOZER && (p.name().equals("metal") || p.name().equals("rubber"))) {
                List<Cuboid> separated=new ArrayList<>();
                for(Cuboid b:p.boxes()) {
                    if(p.material().equals("metal") && b.y()==8 && b.z()==10 && b.w()==2 && b.h()==2 && b.d()==15) separated.add(box(b.x(),b.y(),b.z(),b.w(),2.12,b.d()));
                    else if(p.material().equals("rubber") && b.w()==6 && b.h()==3 && b.d()==36) separated.add(box(b.x()+.12,b.y(),b.z(),5.76,b.h(),b.d()));
                    else separated.add(b);
                }
                p=new Part(p.name(),p.material(),p.px(),p.py(),p.pz(),p.axis(),List.copyOf(separated),p.restPitch(),p.restYaw(),p.restRoll());
            }
            if(p.name().startsWith("header_ram_") || p.name().startsWith("blade_ram_") || p.name().equals("steering_column")) continue;
            if(type.family==VehicleType.Family.COMBINE && p.name().equals("detail_brass")) {
                List<Cuboid> keep=new ArrayList<>();
                for(Cuboid b:p.boxes()) {
                    if(Math.abs(b.y()-8.2)<.001 && Math.abs(b.z()-22)<.001 && Math.abs(b.w()-.55)<.001) headerBolts.add(box(b.x(),b.y()-7,b.z()-26,b.w(),b.h(),b.d()));
                    else keep.add(b);
                }
                if(!keep.isEmpty()) a.parts.add(new Part(p.name(),p.material(),p.px(),p.py(),p.pz(),p.axis(),List.copyOf(keep),p.restPitch(),p.restYaw(),p.restRoll()));
            } else a.parts.add(p);
        }
        a.controls(type);a.wipers(type);
        switch(type.family) {
            case COMBINE -> {
                double w=type.blueprintWidth*16;
                a.part("header_fasteners","brass",0,7,26,'h',0,headerBolts.toArray(Cuboid[]::new));
                for(int side:new int[]{-1,1}) {
                    a.mirror(side,10.5,37.2,16);
                    a.fixed("header_chassis_anchor_"+side,"metal",box(side*10-.8,9.5,16,1.6,11,2.4));a.hydraulic("header",side,16,18);
                    a.part("header_bearing_"+side,"metal",0,7,26,'h',0,box(side<0?-w/2:w/2-3.3,1,1,3.3,5.8,2.2),box(side*10-1,0,-1,2,2,2));
                    List<Cuboid> spokes=new ArrayList<>();
                    for(double x=-w/2+3;x<w/2-2;x+=7) { spokes.add(box(x,-3.5,-.28,.65,7,.56));spokes.add(box(x,-.28,-3.5,.65,.56,7)); }
                    if(side==1) a.part("reel_spokes","metal",0,12,28,'x',0,spokes.toArray(Cuboid[]::new));
                }
                a.fixed("fan_bearing","metal",box(14.6,15.4,-7.6,1.2,1.2,1.2));
            }
            case DOZER -> {
                for(int side:new int[]{-1,1}) {
                    a.mirror(side,9,31.2,5);a.hydraulic("blade",side,15,16);
                    a.fixed("blade_anchor_"+side,"metal",box(side*11-1,14,15,2,2,2));
                    a.part("blade_clevis_"+side,"metal",side*11,7,24,'b',0,box(-1,-1,-1,2,2,2));
                    a.fixed("step_support_"+side,"dark",box(side<0?-13.5:11.8,8,-7.2,1,7,1));
                }
            }
            case PICKUP -> {
                double rear=type==VehicleType.PICKUP_CARGO?-18.5:-16;
                for(int side:new int[]{-1,1}) {
                    a.mirror(side,10,24,6);
                    a.part("signal_brake_"+side,"red",side*10,14,rear-.47,' ',0,box(-.8,-.5,-.08,1.6,1,.12));
                    a.part("signal_reverse_"+side,"lamp",side*10,13.1,rear-.48,' ',0,box(-.7,-.3,-.07,1.4,.6,.12));
                    a.fixed("cargo_rack_mount_"+side,"metal",box(side<0?-10.5:9,17,-15.5,1.5,2,2));
                }
                a.part("driveshaft","metal",0,5.7,-10,'f',0,box(-.45,-.45,0,.9,.9,20));
                a.fixed("differentials","dark",box(-2,4,-12,4,3,2),box(-2,4,10,4,3,2));
                a.fixed("exhaust_hanger","metal",box(-9.2,5,rear-.4,1.9,2.7,.6));
            }
            case MOTORCYCLE -> {
                a.part("front_yoke","metal",0,5.5,12,'u',0,box(-2.4,-.45,-2.1,4.8,.9,2.7),box(-2.4,13,-2.2,4.8,1,1.3),box(-.65,13,-4.3,1.3,1.6,3),box(-.65,14,-4.6,1.3,.8,1.8));
                for(int side:new int[]{-1,1}) {
                    a.fixed("rear_seat_rail_"+side,"metal",box(side*2.5-.4,11.5,-13.9,.8,2.7,10.9));
                    a.fixed("pillion_grip_bridge_"+side,"metal",box(side<0?-4.8:2.7,13,-10,2.1,.6,1));
                }
                for(int i=0;i<2;i++) { Seat s=seat(type,i);a.fixed("footrest_crossbar_"+i,"metal",box(-7,s.top()-3.8,s.z()+9.6,14,.5,.75)); }
                a.fixed("steering_head","dark",box(-1.2,12,7,2.4,7,2.1));a.fixed("instrument_stalk","metal",box(-.45,16,9.5,.9,3.3,.5));
                a.part("signal_brake","red",0,13.55,-15.35,' ',0,box(-1.2,-.38,-.08,2.4,.76,.12));
                a.part("rear_sprocket","metal",-2.0,5.5,-12,'x',0,box(-.2,-2,-.45,.4,4,.9),box(-.2,-.45,-2,.4,.9,4));
                a.fixed("rear_axle","metal",box(-4.8,5.1,-12.4,9.6,.8,.8));
            }
            case BOAT -> {
                double w=type.blueprintWidth*16,back=-boatLength(type)/2;
                a.fixed("transom","paint",box(-w/2+1,6,back,w-2,4,1.5));
                a.fixed("outboard_transom_clamp","metal",box(-3.6,8.6,back-1.4,7.2,1.2,3),box(-.6,7.4,back-1.2,1.2,2,1.2));
                a.part("outboard_gearcase","metal",0,5,back-1,'u',0,box(-.7,-4.7,-2.8,1.4,1.4,3.5));
                for(int i=0;i<2;i++) a.fixed("boarding_ladder_"+i,"metal",box(w/2-6.2+i*4,0,back-1.2,.65,10.6,.65),box(w/2-6.2+i*4,9.8,back-1.2,.65,.8,2));
                for(int side:new int[]{-1,1}) a.fixed("fender_rope_"+side,"trim",box(side*(w/2+.05)-.16,8,-5,.32,3,.32),box(side<0?-w/2-.1:w/2-1.5,10.7,-5,1.6,.3,.32));
            }
            case PLANE -> {
                a.fixed("propeller_shaft","metal",box(-.6,14.5,21.8,1.2,1.2,3));
                for(int side:new int[]{-1,1}) {
                    double x=side*(type.blueprintWidth*8-6);a.fixed("wing_strut_socket_"+side,"metal",box(x-1.2,29.45,11.1,2.4,1.2,2.2));
                    a.fixed("gear_axle_bearing_"+side,"metal",box(side*8.3-.8,3.6,9.5,1.6,4,1.4));
                }
                double tail=type==VehicleType.PLANE_CARGO?-28:-24;
                a.fixed("elevator_hinge_pin","metal",box(-10.4,14.75,tail+1.2,20.8,.6,.6));a.fixed("rudder_hinge_pin","metal",box(-.4,15.6,tail+1.7,.8,9,.6));
            }
            case HELICOPTER -> {
                a.fixed("tail_rotor_gearbox","metal",box(-.3,24.3,-33.7,4.2,1.4,1.4));a.fixed("seat_pedestal","dark",box(-2,9.6,1.2,4,1.8,4.6));
                a.fixed("canopy_sills","paint",box(-8.2,13.2,19.2,16.4,1.5,1.5),box(-8.2,32.2,12,16.4,1,1.5));a.fixed("mast_coupling","brass",box(-1,38.7,-5,2,1.2,2));
            }
            case DRONE -> {
                double r=type.blueprintWidth*6;
                for(int x:new int[]{-1,1}) for(int z:new int[]{-1,1}) {
                    double len=r*Math.sqrt(2);
                    a.parts.add(new Part("radial_boom_"+x+"_"+z,"metal",0,6.5f,0,' ',List.of(box(-.65,-.55,z<0?-len:0,1.3,1.1,len)),0,x*z*45,0));
                    a.fixed("motor_shaft_"+x+"_"+z,"brass",box(x*r-.45,7.8,z*r-.45,.9,1.5,.9));
                }
                a.fixed("camera_yoke","metal",box(-2.1,2.4,5.1,.55,1.8,1),box(1.55,2.4,5.1,.55,1.8,1),box(-2.1,2.3,5.1,4.2,.5,.7));
                a.fixed("console_crossmember","metal",box(-4,12,3.8,8,1,1.2),box(-3.4,12,4,.8,3.6,1.4),box(2.6,12,4,.8,3.6,1.4));
            }
        }
        if(type.family==VehicleType.Family.PICKUP || type.family==VehicleType.Family.COMBINE) {
            for(Part p:original) if(p.name().startsWith("wheel_") && !p.name().startsWith("wheel_hub_")) {
                a.part("spring_link_"+p.name().substring(6),"metal",p.px(),p.py()+4,p.pz(),'s',0,box(-.48,-4,-.48,.96,4,.96));
                double inside=Math.abs(p.px())-3.3;
                a.fixed("spring_mount_"+p.name(),"dark",box(p.px()<0?p.px()-.6:inside,p.py()+3.5,p.pz()-.7,3.9,1.1,1.4));
            }
        }
        Set<String> names=new HashSet<>();for(Part p:a.parts) if(!names.add(p.name()) || p.boxes().isEmpty()) throw new IllegalStateException("Invalid assembly: "+p.name());
        return List.copyOf(a.parts);
    }
}
