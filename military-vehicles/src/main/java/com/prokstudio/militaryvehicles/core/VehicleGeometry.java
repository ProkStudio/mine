package com.prokstudio.militaryvehicles.core;

import java.util.*;
import com.prokstudio.militaryvehicles.core.TruckGeometry.Box;
import com.prokstudio.militaryvehicles.core.TruckGeometry.Part;

/** Original CC0 rigs. The three initial vehicle geometries remain unchanged. */
public final class VehicleGeometry {
    public static final Map<String,Integer> MATERIALS;
    static {
        var palette=new TreeMap<>(TruckGeometry.MATERIALS);
        palette.put("sand",0xB09C74);palette.put("armor",0x4E6252);palette.put("marking",0xDBD6BD);
        MATERIALS=Collections.unmodifiableMap(palette);
    }
    private VehicleGeometry() {}
    private static Box b(float x,float y,float z,float w,float h,float d) { return new Box(x,y,z,w,h,d); }
    private static void part(List<Part> p,String name,String material,Box... boxes) { p.add(new Part(name,material,0,0,0,false,false,List.of(boxes))); }
    public static List<Part> create(VehicleKind kind) {
        return switch(kind) {case TRUCK->TruckGeometry.create();case BUGGY->buggy();case CARRIER->carrier();default->ExpansionGeometry.create(kind);};
    }
    private static void wheels(List<Part> p,VehicleKind kind,float x,float... axles) {
        float r=kind.wheelRadius;
        for(int i=0;i<axles.length;i++) {
            float z=axles[i];boolean front=i<kind.steeringWheels/2;
            part(p,"axle_"+i,"metal",b(-x,r-.7f,z-.8f,x*2,1.4f,1.6f),b(-2,r-2,z-2,4,4,4));
            for(int side:new int[]{-1,1}) {
                String key=i+"_"+side;float sx=side*x;
                // Stepped profile stays inside its nominal rolling radius, including the corners.
                List<Box> tire=List.of(b(-2.6f,-r*.5f,-r*.86f,5.2f,r,r*1.72f),
                    b(-2.6f,r*.5f,-r*.6f,5.2f,r*.3f,r*1.2f),b(-2.6f,-r*.8f,-r*.6f,5.2f,r*.3f,r*1.2f),
                    b(-2.6f,r*.8f,-r*.19f,5.2f,r*.18f,r*.38f),b(-2.6f,-r*.98f,-r*.19f,5.2f,r*.18f,r*.38f));
                p.add(new Part("wheel_"+key,"rubber",sx,r,z,true,front,tire));
                List<Box> hub=new ArrayList<>();hub.add(b(-2.9f,-2.5f,-2.5f,5.8f,5,5));
                for(int n=0;n<4;n++) {double a=n*Math.PI/2;hub.add(b(side*3-.25f,(float)Math.sin(a)*1.7f-.25f,(float)Math.cos(a)*1.7f-.25f,.5f,.5f,.5f));}
                p.add(new Part("wheel_hub_"+key,"metal",sx,r,z,true,front,hub));
                part(p,"spring_"+key,"dark",b(side<0?-11:9,r+2,z-5,2,1,10));
            }
        }
    }
    private static void seats(List<Part> p,VehicleKind kind,float floor) {
        for(int i=0;i<kind.seats.size();i++) {
            var s=kind.seats.get(i);float x=s.x(),y=s.topY(),z=s.z();
            part(p,"seat_mount_"+i,"metal",b(x-2,floor,z-3,4,y-3-floor,5));
            part(p,"seat_"+i,"seat",b(x-3.8f,y-3,z-4,7.6f,3,8),b(x-3.8f,y,z-4,7.6f,9,1.5f));
            part(p,"seat_belt_"+i,"canvas",b(x-.35f,y+1,z-2.45f,.7f,7,.12f));
        }
    }
    private static void steering(List<Part> p,float x,float y,float z,float floor) {
        part(p,"steering_column","metal",b(x-.4f,floor,z-.4f,.8f,y-floor,.8f));
        p.add(new Part("steering","dark",x,y,z,false,false,List.of(
            b(-2.5f,-.4f,-2.5f,5,.8f,.7f),b(-2.5f,-.4f,1.8f,5,.8f,.7f),
            b(-2.5f,-.4f,-1.8f,.7f,.8f,3.6f),b(1.8f,-.4f,-1.8f,.7f,.8f,3.6f),b(-.3f,-.3f,-1.8f,.6f,.6f,3.6f))));
    }
    private static List<Part> buggy() {
        List<Part> p=new ArrayList<>();
        wheels(p,VehicleKind.BUGGY,13,18,-18);
        part(p,"tube_chassis","dark",b(-9,7,-24,2,3,48),b(7,7,-24,2,3,48),b(-7,8,-20,14,2,2),b(-7,8,0,14,2,2),b(-7,8,18,14,2,2));
        part(p,"skid_plate","metal",b(-9,6,-8,18,.8f,26));
        part(p,"cabin_floor","sand",b(-11,10,-10,22,2,25));
        part(p,"hood","sand",b(-10,13,15,20,2,8));
        part(p,"nose","sand",b(-10,10,23,20,5,2));
        part(p,"radiator","dark",b(-6,10.7f,25.03f,12,3.5f,.25f));
        for(int n=0;n<3;n++) part(p,"hood_vent_"+n,"dark",b(-6+n*4,15.02f,17,2,.25f,5));
        part(p,"front_bumper","metal",b(-13,8,24.5f,26,2,2));
        part(p,"tow_lugs","dark",b(-9,7,25,2,4,2),b(7,7,25,2,4,2));
        part(p,"rear_bumper","metal",b(-13,8,-26,26,2,2));
        for(int side:new int[]{-1,1}) {
            float x=side<0?-16:11;
            part(p,"fenders_"+side,"sand",b(x,12,12,5,1.2f,12),b(x,12,-24,5,1.2f,12));
            part(p,"side_sill_"+side,"dark",b(side<0?-12:10,12,-9,2,2,22));
            part(p,"footstep_"+side,"metal",b(side<0?-14:11,9,-7,3,1,14));
            float post=side<0?-11.5f:10;
            part(p,"roll_posts_"+side,"dark",b(post,12,-8,1.5f,23,1.5f),b(post,12,12,1.5f,23,1.5f));
            part(p,"roll_roof_"+side,"dark",b(post,35,-8,1.5f,1.5f,21.5f));
            part(p,"mirror_arm_"+side,"metal",b(side<0?-15:11,30,12,4,.6f,.6f));
            part(p,"mirror_"+side,"dark",b(side<0?-16:14,28.5f,11,2,3,1));
            part(p,"mirror_face_"+side,"metal",b(side<0?-15.7f:14.3f,28.8f,10.82f,1.4f,2.4f,.15f));
        }
        part(p,"roll_crossbars","dark",b(-11.5f,35,-8,23,1.5f,1.5f),b(-11.5f,35,12,23,1.5f,1.5f),b(-11.5f,23,12,23,1,1.5f));
        part(p,"windscreen","glass",b(-10,24,12.65f,20,10,.13f));
        part(p,"dashboard","dark",b(-9,21,10,18,2,2));
        part(p,"gauges","marking",b(3,21.6f,9.65f,4,1,.3f));
        seats(p,VehicleKind.BUGGY,12);steering(p,6,24,8,12);
        part(p,"pedals","rubber",b(4,12.2f,8,1.5f,.6f,3),b(7,12.2f,8,1.5f,.6f,3));
        part(p,"shifter","metal",b(-.3f,12,5,.6f,8,.6f));
        part(p,"rear_engine","dark",b(-8,12,-22,16,6,10));
        for(int n=0;n<5;n++) part(p,"cooling_fin_"+n,"metal",b(-7,18.04f,-21+n*1.7f,14,.4f,.8f));
        part(p,"fuel_cell","sand",b(-9,10,-24,18,4,5));
        part(p,"rear_rack","metal",b(-10,20,-23,1,1,13),b(9,20,-23,1,1,13),b(-9,20,-22,18,1,1),b(-9,20,-12,18,1,1));
        part(p,"rack_mounts","dark",b(-10,12,-22,1,8,1),b(9,12,-22,1,8,1),b(-10,12,-12,1,8,1),b(9,12,-12,1,8,1));
        part(p,"spare_carrier","metal",b(-1.5f,20,-15,3,9,3));
        part(p,"spare_tire","rubber",b(-4.3f,25.5f,-16,8.6f,5,3),b(-3,30.5f,-16,6,1.5f,3),b(-3,24,-16,6,1.5f,3),b(-.95f,32,-16,1.9f,.9f,3),b(-.95f,23.1f,-16,1.9f,.9f,3));
        part(p,"spare_hub","metal",b(-2.5f,25.5f,-16.3f,5,5,3.6f));
        part(p,"exhaust_stack","metal",b(-10.5f,14,-25,1.5f,1.5f,10),b(-10.5f,14,-17,4,1.5f,1.5f));
        part(p,"headlamp_mounts","dark",b(-10,11,25,3.2f,3.2f,.5f),b(6.8f,11,25,3.2f,3.2f,.5f));
        part(p,"headlamps","light",b(-9.6f,11.4f,25.55f,2.4f,2.4f,.25f),b(7.2f,11.4f,25.55f,2.4f,2.4f,.25f));
        part(p,"tail_light_mounts","dark",b(-10,10,-24.5f,3,4,1),b(7,10,-24.5f,3,4,1));
        part(p,"tail_lights","tail",b(-9.6f,11,-24.8f,2.2f,1.8f,.25f),b(7.4f,11,-24.8f,2.2f,1.8f,.25f));
        return List.copyOf(p);
    }
    private static List<Part> carrier() {
        List<Part> p=new ArrayList<>();
        wheels(p,VehicleKind.CARRIER,19,26,10,-10,-26);
        part(p,"reinforced_frame","dark",b(-12,9,-34,3,4,68),b(9,9,-34,3,4,68),b(-9,10,-29,18,3,3),b(-9,10,-8,18,3,3),b(-9,10,14,18,3,3));
        part(p,"hull_floor","armor",b(-17,12,-34,34,3,67));
        part(p,"lower_nose","dark",b(-16,10,33,32,5,4));
        part(p,"stepped_front_armor","armor",b(-16,15,29,32,8,7),b(-16,23,28,32,6,7));
        part(p,"front_window_frame","armor",b(-15,29,32,30,5,2),b(-15,40,32,30,3,2),b(-17,29,31,3,14,3),b(14,29,31,3,14,3),b(-1,34,32,2,6,2));
        part(p,"front_vision_glass","glass",b(-14,34,33.3f,13,6,.15f),b(1,34,33.3f,13,6,.15f));
        part(p,"vision_armor_lip","dark",b(-17,40,32.5f,34,1,3));
        for(int side:new int[]{-1,1}) {
            float x=side<0?-18:15;
            part(p,"side_hull_"+side,"armor",b(x,15,-33,3,20,64),b(x,39,-33,3,4,64),
                b(x,35,-33,3,4,8),b(x,35,-10,3,4,3),b(x,35,7,3,4,5),b(x,35,27,3,4,4));
            float glass=side<0?-16.6f:16.45f;
            part(p,"side_vision_glass_"+side,"glass",b(glass,35,-25,.15f,4,15),b(glass,35,-7,.15f,4,14),b(glass,35,12,.15f,4,15));
            part(p,"fender_"+side,"armor",b(side<0?-23:16,14,-33,7,1.2f,66));
            part(p,"side_rubrail_"+side,"dark",b(side<0?-18.6f:18,23,-30,.6f,1.2f,56));
            for(int z:new int[]{-28,-11,8,26}) part(p,"armor_rivet_"+side+"_"+z,"metal",b(side<0?-18.3f:18,17,z,.3f,.6f,.6f));
            part(p,"entry_steps_"+side,"metal",b(side<0?-21:18,9,14,3,1,9),b(side<0?-20:18,12,14,2,1,9));
            part(p,"mirror_arm_"+side,"metal",b(side<0?-22:18,37,29,4,.8f,.8f));
            part(p,"mirror_"+side,"dark",b(side<0?-23:21,34,27,2,5,2));
            part(p,"mirror_face_"+side,"metal",b(side<0?-22.7f:21.3f,34.4f,26.8f,1.4f,4.2f,.15f));
        }
        part(p,"armored_roof","armor",b(-18,43,-33,36,2,67));
        part(p,"roof_front_lip","dark",b(-17,45,14,34,1,15));
        part(p,"roof_hatch","dark",b(-7,45,2,14,1.8f,13));
        part(p,"roof_hatch_handle","metal",b(-2,46.8f,7,4,.8f,1));
        part(p,"rear_escape_hatches","dark",b(-13,45,-27,12,.8f,20),b(1,45,-27,12,.8f,20));
        part(p,"escape_hatch_handles","metal",b(-9,45.8f,-17,4,.6f,1),b(5,45.8f,-17,4,.6f,1));
        part(p,"rear_bulkhead","armor",b(-17,15,-35,34,28,2));
        part(p,"rear_ramp","dark",b(-11,16,-35.45f,22,24,.4f));
        part(p,"ramp_handle","metal",b(-3,32,-35.8f,6,1,.3f));
        part(p,"ramp_hinges","metal",b(-10,14,-35.8f,4,2,1),b(6,14,-35.8f,4,2,1));
        part(p,"rear_bumper","dark",b(-20,10,-37,40,2,3));
        part(p,"front_tow_lugs","metal",b(-12,9,36,3,5,2),b(9,9,36,3,5,2));
        part(p,"headlamp_guards","dark",b(-15,18,36.05f,6,5,1),b(9,18,36.05f,6,5,1));
        part(p,"headlamps","light",b(-14.4f,18.6f,37.1f,4.8f,3.8f,.25f),b(9.6f,18.6f,37.1f,4.8f,3.8f,.25f));
        part(p,"tail_lights","tail",b(-16,17,-35.6f,4,3,.4f),b(12,17,-35.6f,4,3,.4f));
        part(p,"rear_identification","marking",b(-5,18,-35.9f,10,2,.15f));
        part(p,"dashboard","dark",b(-13,27,29,26,3,2));
        part(p,"instruments","marking",b(5,28,28.65f,6,1.2f,.3f));
        seats(p,VehicleKind.CARRIER,15);steering(p,8.5f,29,26,15);
        part(p,"pedals","rubber",b(5.5f,15.2f,25,2,.8f,3),b(9.5f,15.2f,25,2,.8f,3));
        part(p,"interior_grab_rail","metal",b(-.5f,36,-27,1,1,48));
        part(p,"grab_rail_supports","metal",b(-.5f,37,-23,1,6,1),b(-.5f,37,17,1,6,1));
        part(p,"exhaust_stack","metal",b(-18.5f,18,-27,1.5f,21,1.5f),b(-18.5f,38,-28,1.5f,1.5f,2.5f));
        part(p,"exhaust_mounts","dark",b(-18.8f,24,-27.2f,2,.8f,2),b(-18.8f,34,-27.2f,2,.8f,2));
        return List.copyOf(p);
    }
}
