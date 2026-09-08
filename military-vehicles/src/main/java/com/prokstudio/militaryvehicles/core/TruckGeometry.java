package com.prokstudio.militaryvehicles.core;
import java.util.*;
/** Original CC0 voxel rig: +Y up, +Z front, 16 model units per block. */
public final class TruckGeometry {
    public record Box(float x,float y,float z,float w,float h,float d) {
        public Box { if(!Float.isFinite(x+y+z+w+h+d)||w<=0||h<=0||d<=0) throw new IllegalArgumentException("Invalid cuboid"); }
    }
    public record Part(String name,String material,float x,float y,float z,boolean wheel,boolean front,List<Box> boxes) {
        public Part { boxes=List.copyOf(boxes); }
    }
    public static final Map<String,Integer> MATERIALS;
    static {
        Map<String,Integer> c=new LinkedHashMap<>();
        c.put("olive",0x68754D);c.put("dark",0x303A31);c.put("metal",0x596369);c.put("rubber",0x252A2D);c.put("glass",0x8FAEB1);
        c.put("seat",0x394538);c.put("light",0xF1D9A1);c.put("tail",0xBA5946);c.put("canvas",0x8A8662);c.put("accent",0xC4B68E);
        MATERIALS=Collections.unmodifiableMap(c);
    }
    private TruckGeometry() {}
    private static Box b(float x,float y,float z,float w,float h,float d) { return new Box(x,y,z,w,h,d); }
    private static void part(List<Part> out,String name,String material,Box... boxes) { out.add(new Part(name,material,0,0,0,false,false,List.of(boxes))); }
    public static List<Part> create() {
        List<Part> p=new ArrayList<>();
        part(p,"ladder_frame","dark",b(-12,9,-35,3,4,68),b(9,9,-35,3,4,68),b(-9,10,-30,18,2,3),b(-9,10,-10,18,2,3),b(-9,10,12,18,2,3),b(-9,10,28,18,2,3));
        for(int i=0;i<3;i++) {
            float z=new float[]{24,-10,-26}[i];
            part(p,"axle_"+i,"metal",b(-17,5,z-1,34,2,2),b(-3,3.5f,z-3,6,5,6));
            part(p,"springs_"+i,"dark",b(-12,8,z-6,2,1,12),b(10,8,z-6,2,1,12));
            for(int side:new int[]{-1,1}) {
                float x=side*17;String key=i+"_"+side;
                p.add(new Part("wheel_"+key,"rubber",x,5.5f,z,true,i==0,List.of(
                    b(-2.5f,-5.5f,-3.5f,5,2,7),b(-2.5f,-3.5f,-5.5f,5,7,11),b(-2.5f,3.5f,-3.5f,5,2,7))));
                List<Box> hub=new ArrayList<>();hub.add(b(-2.8f,-2.5f,-2.5f,5.6f,5,5));
                for(int n=0;n<4;n++) { double a=n*Math.PI/2;hub.add(b(side*3-.35f,(float)Math.sin(a)*1.7f-.35f,(float)Math.cos(a)*1.7f-.35f,.7f,.7f,.7f)); }
                p.add(new Part("wheel_hub_"+key,"metal",x,5.5f,z,true,i==0,hub));
            }
        }
        part(p,"cab_floor","olive",b(-16,12,9,32,3,24));
        part(p,"cab_nose","olive",b(-16,15,30,32,11,3),b(-14,26,31,28,2,2));
        part(p,"front_bumper","dark",b(-18,9,33,36,3,3));
        part(p,"bumper_tow_lugs","metal",b(-12,8,34,2,4,3),b(10,8,34,2,4,3));
        part(p,"radiator","dark",b(-9,16,33.05f,18,7,.3f));
        for(int y=17;y<=22;y+=2) part(p,"grille_"+y,"metal",b(-8,y,33.4f,16,.6f,.5f));
        part(p,"headlamp_housings","dark",b(-15,17,33,5,5,1),b(10,17,33,5,5,1));
        part(p,"headlamps","light",b(-14.4f,17.6f,34.02f,3.8f,3.8f,.25f),b(10.6f,17.6f,34.02f,3.8f,3.8f,.25f));
        part(p,"indicators","accent",b(-15,24,33.05f,3,1.4f,.4f),b(12,24,33.05f,3,1.4f,.4f));
        part(p,"rear_cab_wall","olive",b(-16,15,9,32,26,2));
        part(p,"cab_roof","olive",b(-17,42,8,34,2,26));
        part(p,"cab_pillars","olive",b(-16,26,30,2,16,3),b(14,26,30,2,16,3),b(-1,27,31,2,15,1));
        part(p,"windscreen","glass",b(-14,28,31.5f,13,13,.125f),b(1,28,31.5f,13,13,.125f));
        for(int side:new int[]{-1,1}) {
            float x=side<0?-16:14;
            part(p,"door_"+side,"olive",b(x,15,11,2,13,19),b(x,28,11,2,14,2));
            part(p,"side_window_"+side,"glass",b(side*15.1f,28,13,.125f,13,17));
            part(p,"door_handle_"+side,"metal",b(side*16.1f-.25f,25,14,.5f,1,3));
            part(p,"steps_"+side,"metal",b(side<0?-19:16,7,12,3,1,7),b(side<0?-18:16,10,12,2,1,7));
            part(p,"mirror_arm_"+side,"metal",b(side<0?-21:16,34,28,5,.7f,.7f),b(side*20.5f,32,28,.7f,5,.7f));
            part(p,"mirror_back_"+side,"dark",b(side<0?-22:20.5f,32,26,1.5f,5,2));
            part(p,"mirror_face_"+side,"metal",b(side<0?-21.8f:20.7f,32.3f,25.8f,1.1f,4.4f,.15f));
            float sx=side*9;
            part(p,"seat_pedestal_"+side,"metal",b(sx-3,15,14,6,6,6));
            part(p,"seat_"+side,"seat",b(sx-4,21,12,8,3,9),b(sx-4,24,12,8,10,2));
        }
        part(p,"dashboard","dark",b(-14,26,27,28,3,3));
        part(p,"gauges","accent",b(6,27,26.65f,5,1.5f,.3f),b(-7,27,26.65f,3,1,.3f));
        part(p,"steering_column","metal",b(8.5f,23,24,.8f,6,.8f));
        p.add(new Part("steering","dark",9,29,24,false,false,List.of(b(-3,-.6f,-3,6,1.2f,1),b(-3,-.6f,2,6,1.2f,1),b(-3,-.6f,-2,1,1.2f,4),b(2,-.6f,-2,1,1.2f,4),b(-.5f,-.5f,-2,1,1,4))));
        part(p,"gear_lever","metal",b(1,15,22,.8f,11,.8f));
        part(p,"pedals","rubber",b(6,16,25,2,.7f,3),b(10,16,25,2,.7f,3));
        part(p,"bed_floor","olive",b(-17,14,-35,34,2,42));
        part(p,"bed_sides","olive",b(-17,16,-35,2,10,42),b(15,16,-35,2,10,42),b(-15,16,-35,30,10,2),b(-15,16,5,30,10,2));
        for(int z=-33;z<=3;z+=9) part(p,"bed_ribs_"+z,"dark",b(-17.5f,16,z,.7f,11,1.2f),b(16.8f,16,z,.7f,11,1.2f));
        part(p,"bed_top_rails","accent",b(-17.3f,26,-35,2.6f,.8f,42),b(14.7f,26,-35,2.6f,.8f,42));
        part(p,"tailgate_hinges","metal",b(-12,15,-35.5f,4,1.5f,.7f),b(8,15,-35.5f,4,1.5f,.7f));
        part(p,"rear_bumper","dark",b(-18,9,-37,36,2,3));
        part(p,"rear_lights","tail",b(-16,12,-35.8f,5,2,.6f),b(11,12,-35.8f,5,2,.6f));
        part(p,"fuel_tank","metal",b(-16,8,-3,8,5,10));
        part(p,"tool_box","dark",b(8,8,-3,8,5,9));
        part(p,"exhaust_stack","metal",b(-15,13,5,1.5f,26,1.5f),b(-15,38,3,1.5f,1.5f,2));
        part(p,"spare_tire","rubber",b(-5,18,1,10,10,3));
        part(p,"spare_hub","metal",b(-2,21,.7f,4,4,3.6f));
        part(p,"rear_mudflaps","rubber",b(-20,1,-33,6,9,.7f),b(14,1,-33,6,9,.7f));
        part(p,"roof_hatch","dark",b(-6,44,13,12,.7f,12));
        return List.copyOf(p);
    }
}
