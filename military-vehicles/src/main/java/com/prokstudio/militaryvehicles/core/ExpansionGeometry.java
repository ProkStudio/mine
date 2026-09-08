package com.prokstudio.militaryvehicles.core;

import java.util.*;
import com.prokstudio.militaryvehicles.core.TruckGeometry.Box;
import com.prokstudio.militaryvehicles.core.TruckGeometry.Part;

/** Original CC0 tracked chassis and purpose-built service bodies, not recoloured cargo beds. */
public final class ExpansionGeometry {
    public static final float TURRET_Y=28,TURRET_Z=-5,GUN_Y=34,GUN_Z=3;
    private ExpansionGeometry() {}
    private static Box b(float x,float y,float z,float w,float h,float d) { return new Box(x,y,z,w,h,d); }
    private static void part(List<Part> p,String name,String material,Box... boxes) { p.add(new Part(name,material,0,0,0,false,false,List.of(boxes))); }
    private static void pivot(List<Part> p,String name,String material,float x,float y,float z,Box... boxes) { p.add(new Part(name,material,x,y,z,false,false,List.of(boxes))); }
    public static List<Part> create(VehicleKind kind) {
        if(kind.tracked()) return tracked(kind);
        if(!kind.support()) throw new IllegalArgumentException("No expansion rig for "+kind);
        List<Part> p=new ArrayList<>();
        for(var old:TruckGeometry.create()) {
            String n=old.name();
            if(n.startsWith("bed_")&&!n.equals("bed_floor")||n.startsWith("tailgate_")||n.startsWith("spare_")) continue;
            // These new derivatives use circular rolling envelopes. The original truck stays byte-for-byte unchanged.
            p.add(old.wheel()&&old.material().equals("rubber")?roundWheel(old):old);
        }
        switch(kind) {case TANKER->tanker(p);case WORKSHOP->workshop(p);case RECOVERY->recovery(p);default->throw new AssertionError(kind);}
        return List.copyOf(p);
    }
    private static Part roundWheel(Part p) {
        float r=p.y();
        return new Part(p.name(),p.material(),p.x(),p.y(),p.z(),true,p.front(),List.of(
            b(-2.5f,-r*.5f,-r*.86f,5,r,r*1.72f),b(-2.5f,r*.5f,-r*.6f,5,r*.3f,r*1.2f),
            b(-2.5f,-r*.8f,-r*.6f,5,r*.3f,r*1.2f),b(-2.5f,r*.8f,-r*.19f,5,r*.18f,r*.38f),b(-2.5f,-r*.98f,-r*.19f,5,r*.18f,r*.38f)));
    }
    private static void tanker(List<Part> p) {
        part(p,"tank_cradles","dark",b(-13,16,-28,26,2,4),b(-13,16,-5,26,2,4));
        part(p,"faceted_reservoir","sand",b(-9,18,-33,18,3,37),b(-13,21,-33,26,11,37),b(-9,32,-33,18,4,37));
        for(int z:new int[]{-28,-5}) part(p,"reservoir_strap_"+z,"metal",b(-13.5f,20,z,.4f,13,1),b(13.1f,20,z,.4f,13,1),b(-9,36.05f,z,18,.45f,1));
        part(p,"reservoir_fill_neck","dark",b(-3,36,-14,6,1.2f,6));
        part(p,"reservoir_cap","metal",b(-3.5f,37.2f,-14.5f,7,.8f,7));
        part(p,"pump_box","dark",b(-12,17,-36,9,8,3));
        part(p,"pump_gauge","marking",b(-10,21,-36.25f,3,2,.2f));
        part(p,"pipework","metal",b(9,17,-35,2,8,2),b(-3,17,-36,14,2,2));
        part(p,"valve","accent",b(7,22,-35.4f,6,1,.6f),b(9.5f,20,-35.4f,1,5,.6f));
        part(p,"hose_spool_mount","metal",b(-2,18,-36,4,12,2));
        part(p,"hose_spool","rubber",b(-5,23,-38,10,6,2),b(-3.5f,21.5f,-38,7,1.5f,2),b(-3.5f,29,-38,7,1.5f,2));
        part(p,"hose_spool_hub","metal",b(-1.5f,24.5f,-38.3f,3,3,2.6f));
        part(p,"hose_nozzle","metal",b(12,18,-36,1.3f,6,1.3f));
        for(int side:new int[]{-1,1}) part(p,"tank_level_"+side,"marking",b(side<0?-13.3f:13.1f,24,-18,.2f,5,6));
    }
    private static void workshop(List<Part> p) {
        part(p,"workshop_floor","metal",b(-16,16,-35,32,2,40));
        part(p,"workshop_box","armor",b(-16,18,-35,2,22,40),b(14,18,-35,2,22,40),b(-14,18,-35,28,22,2),b(-14,18,3,28,22,2));
        part(p,"workshop_roof","armor",b(-16,40,-35,32,2,40));
        part(p,"roof_rack","metal",b(-15,42,-32,1,2,31),b(14,42,-32,1,2,31),b(-14,42,-32,28,1,1),b(-14,42,-2,28,1,1));
        for(int side:new int[]{-1,1}) {
            float x=side<0?-16.3f:16.05f;
            part(p,"tool_shutter_"+side,"dark",b(x,21,-28,.25f,15,24));
            for(int y=23;y<36;y+=3) part(p,"shutter_slat_"+side+"_"+y,"metal",b(side<0?-16.6f:16.3f,y,-27,.2f,.6f,22));
            part(p,"service_mark_"+side,"marking",b(side<0?-16.7f:16.6f,27,-20,.12f,3,9),b(side<0?-16.7f:16.6f,24,-17,.12f,9,3));
        }
        part(p,"rear_cabinet","dark",b(-12,20,-35.4f,24,16,.3f));
        part(p,"cabinet_seam","metal",b(-.35f,20,-35.75f,.7f,16,.3f));
        part(p,"cabinet_handles","accent",b(-3,26,-36,1,5,.3f),b(2,26,-36,1,5,.3f));
        part(p,"folded_workbench","metal",b(-12,17,-36,24,2,1),b(-10,12,-36,1,5,1),b(9,12,-36,1,5,1));
        part(p,"utility_case","dark",b(-8,43,-25,16,3,14));
        part(p,"utility_case_latch","accent",b(-2,44,-25.3f,4,1,.2f));
    }
    private static void recovery(List<Part> p) {
        part(p,"reinforced_deck","metal",b(-17,16,-35,34,2,39));
        part(p,"winch_mast","armor",b(-11,18,-12,4,20,5),b(7,18,-12,4,20,5),b(-7,34,-12,14,4,5));
        part(p,"rear_boom","armor",b(-3,35,-39,6,4,31));
        part(p,"boom_rail","metal",b(-2,39,-38,4,1,27));
        part(p,"winch_base","dark",b(-10,18,-31,20,3,15));
        part(p,"winch_bearings","metal",b(-10,21,-25,2,9,5),b(8,21,-25,2,9,5));
        pivot(p,"winch_drum","rubber",0,25,-22.5f,b(-8,-3,-3,16,6,6));
        part(p,"winch_axle","metal",b(-11,24,-23.5f,22,2,2));
        part(p,"guide_rollers","metal",b(-5,20,-35,2,7,2),b(3,20,-35,2,7,2),b(-3,20,-35,6,2,2));
        part(p,"boom_cable","dark",b(-.3f,24,-39,.6f,11,.6f));
        part(p,"recovery_hook","metal",b(-1.8f,21,-39.5f,1,4,1),b(-.8f,21,-39.5f,3,1,1),b(1.2f,22,-39.5f,1,1.5f,1));
        part(p,"stowed_recovery_blade","dark",b(-21,5,-39,42,6,2));
        part(p,"blade_mounts","metal",b(-12,8,-38,2,3,6),b(10,8,-38,2,3,6));
        for(int side:new int[]{-1,1}) part(p,"recovery_tool_case_"+side,"sand",b(side<0?-17:11,18,-4,6,7,8));
        part(p,"hazard_marks","accent",b(-18,7,-39.2f,5,1,.15f),b(-8,7,-39.2f,5,1,.15f),b(3,7,-39.2f,5,1,.15f),b(13,7,-39.2f,5,1,.15f));
    }
    private static List<Part> tracked(VehicleKind kind) {
        List<Part> p=new ArrayList<>();boolean artillery=kind==VehicleKind.HOWITZER;
        String skin=artillery?"sand":"armor";
        for(int side:new int[]{-1,1}) {
            float x=side*23;
            part(p,"track_band_"+side,"rubber",b(x-3,0,-27,6,.8f,54),b(x-3,12.2f,-27,6,.8f,54),
                b(x-3,1.5f,-31,6,10,1.2f),b(x-3,1.5f,29.8f,6,10,1.2f),b(x-3,.6f,-29.8f,6,1,3),b(x-3,.6f,26.8f,6,1,3),b(x-3,11.4f,-29.8f,6,1,3),b(x-3,11.4f,26.8f,6,1,3));
            for(boolean upper:new boolean[]{false,true}) for(int i=0;i<18;i++)
                pivot(p,"tread_"+(upper?"upper_":"lower_")+side+"_"+i,"metal",x,upper?13:.4f,-25.5f+i*3,b(-3.3f,-.35f,-1.15f,6.6f,.7f,2.3f));
            for(int i=0;i<7;i++) {
                float z=-27+i*9;String key=side+"_"+i;
                p.add(new Part("wheel_"+key,"rubber",x,6.5f,z,true,false,List.of(b(-2.5f,-2.6f,-4.4f,5,5.2f,8.8f),b(-2.5f,2.6f,-3,5,1.5f,6),b(-2.5f,-4.1f,-3,5,1.5f,6))));
                p.add(new Part("wheel_hub_"+key,"metal",x,6.5f,z,true,false,List.of(b(-2.8f,-2.3f,-2.3f,5.6f,4.6f,4.6f),b(side*3-.2f,-.8f,-.8f,.4f,1.6f,1.6f))));
                part(p,"suspension_arm_"+key,"dark",b(side<0?-23:16,6,z-1,7,2,2));
            }
            part(p,"track_fender_"+side,skin,b(side<0?-28:18,14,-33,10,1.2f,66));
            for(int z:new int[]{-25,-7,11}) part(p,"side_skirt_"+side+"_"+z,skin,b(side<0?-28.5f:28,10,z,.5f,5,14));
        }
        part(p,"lower_hull","dark",b(-18,10,-34,36,4,68));
        part(p,"hull_deck",skin,b(-19,14,-33,38,8,65),b(-17,22,-31,34,3,60));
        part(p,"stepped_glacis",skin,b(-18,14,32,36,5,4),b(-16,19,30,32,4,5));
        part(p,"driver_hatch","dark",b(4,25,18,10,1,11));
        part(p,"driver_periscope","glass",b(6,26,25,6,2,1.5f));
        part(p,"front_tow_lugs","metal",b(-14,12,35,3,4,2),b(11,12,35,3,4,2));
        part(p,"front_light_guards","dark",b(-18,21,30,5,4,2),b(13,21,30,5,4,2));
        part(p,"headlamps","light",b(-17.5f,21.5f,32.1f,4,3,.25f),b(13.5f,21.5f,32.1f,4,3,.25f));
        part(p,"tail_lights","tail",b(-17,17,-34.4f,4,2,.3f),b(13,17,-34.4f,4,2,.3f));
        for(int i=0;i<6;i++) part(p,"engine_grille_"+i,"dark",b(-11+i*4,25.05f,-30,2,.5f,10));
        part(p,"exhaust_stack","metal",b(-19,18,-33,1.5f,8,1.5f),b(-19,25,-35,1.5f,1.5f,3.5f));
        part(p,"exhaust_guard","dark",b(-19.3f,20,-33.3f,2.1f,.7f,2.1f));
        part(p,"turret_ring","dark",b(-11,25,-16,22,3,22));
        for(int i=0;i<kind.seats.size();i++) {
            var s=kind.seats.get(i);part(p,"seat_"+i,"seat",b(s.x()-3.5f,s.topY()-3,s.z()-4,7,3,8),b(s.x()-3.5f,s.topY(),s.z()-4,7,7,1));
        }
        part(p,"driver_levers","metal",b(4,24,23,.7f,7,.7f),b(13,24,23,.7f,7,.7f));
        if(artillery) {
            pivot(p,"turret_platform","dark",0,TURRET_Y,TURRET_Z,b(-14,-1,-13,28,2,25));
            pivot(p,"turret_shield",skin,0,TURRET_Y,TURRET_Z,b(-14,1,9,10,14,2),b(4,1,9,10,14,2),b(-14,1,-6,2,11,15),b(12,1,-6,2,11,15));
            pivot(p,"turret_trunnions","metal",0,TURRET_Y,TURRET_Z,b(-5,2,5,2,8,5),b(3,2,5,2,8,5));
            for(int side:new int[]{-1,1}) for(int z:new int[]{-28,27}) {
                part(p,"outrigger_mount_"+side+"_"+z,"metal",b(side<0?-32:18,13,z-1,14,2,3),b(side*31-1.7f,8,z-1.7f,3.4f,6,3.4f));
                pivot(p,"outrigger_leg_"+side+"_"+z,"dark",side*31,10,z,b(-1.3f,-4,-1.3f,2.6f,7,2.6f),b(-3,-5,-3,6,1,6));
            }
            part(p,"shell_lockers",skin,b(-18,25,-29,10,5,8),b(8,25,-29,10,5,8));
        } else {
            pivot(p,"turret_armor",skin,0,TURRET_Y,TURRET_Z,b(-13,0,-11,26,7,20),b(-11,7,-10,22,2,18),b(-10,0,9,20,6,3));
            pivot(p,"turret_cupola","dark",0,TURRET_Y,TURRET_Z,b(-4,9,-8,8,2,8));
            pivot(p,"turret_hatch_handle","metal",0,TURRET_Y,TURRET_Z,b(-2,11,-6,4,.7f,1));
            pivot(p,"turret_optics","glass",0,TURRET_Y,TURRET_Z,b(7,6,8.2f,3,2,.3f));
            pivot(p,"turret_storage","dark",0,TURRET_Y,TURRET_Z,b(-14,0,-15,28,5,3));
            pivot(p,"turret_antenna","metal",0,TURRET_Y,TURRET_Z,b(10,9,-8,.4f,13,.4f));
        }
        pivot(p,"gun_breech","dark",0,GUN_Y,GUN_Z,b(-2.5f,-2.5f,-7,5,5,10));
        pivot(p,"gun_barrel","metal",0,GUN_Y,GUN_Z,b(-1.2f,-1.2f,3,2.4f,2.4f,artillery?31:27));
        pivot(p,"gun_sleeve",skin,0,GUN_Y,GUN_Z,b(-1.6f,-1.6f,4,3.2f,3.2f,9));
        pivot(p,"gun_muzzle","dark",0,GUN_Y,GUN_Z,b(-1.8f,-1.8f,artillery?34:30,3.6f,3.6f,3));
        return List.copyOf(p);
    }
}
