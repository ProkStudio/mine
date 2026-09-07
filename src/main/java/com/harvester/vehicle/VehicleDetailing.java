package com.harvester.vehicle;

import java.util.*;
import static com.harvester.vehicle.VehicleGeometry.*;

/** Editable mechanical meshes in model pixels.
 * Static shapes are grouped by material; articulated assemblies have named, local pivots.
 */
public final class VehicleDetailing {
    private VehicleDetailing() {}
    private static final class Mesh {
        final Map<String,Part> parts=new LinkedHashMap<>();
        final Map<String,List<Cuboid>> boxes=new LinkedHashMap<>();
        String part(String name,String material,double x,double y,double z,char axis) {
            return part(name,material,x,y,z,axis,0,0,0);
        }
        String part(String name,String material,double x,double y,double z,char axis,double rx,double ry,double rz) {
            if(parts.containsKey(name)) throw new IllegalArgumentException("Duplicate assembly: "+name);
            parts.put(name,new Part(name,material,(float)x,(float)y,(float)z,axis,List.of(),(float)rx,(float)ry,(float)rz));
            boxes.put(name,new ArrayList<>()); return name;
        }
        String fixed(String material) {
            String key="detail_"+material;
            if(!parts.containsKey(key)) part(key,material,0,0,0,' ');
            return key;
        }
        void add(String material,double x,double y,double z,double w,double h,double d) { box(fixed(material),x,y,z,w,h,d); }
        void box(String key,double x,double y,double z,double w,double h,double d) {
            if(!Double.isFinite(x+y+z+w+h+d) || w<=0 || h<=0 || d<=0) throw new IllegalArgumentException("Invalid box in "+key);
            boxes.get(key).add(new Cuboid((float)x,(float)y,(float)z,(float)w,(float)h,(float)d));
        }
        void rod(String name,String material,double x,double y,double z,double length,double width,char axis,double degrees) {
            String part=part(name,material,x,y,z,' ',axis=='x'?degrees:0,0,axis=='z'?degrees:0);
            box(part,-width/2,-length/2,-width/2,width,length,width);
        }
        void wheel(String id,double x,double y,double z,double r,double width) {
            String tire=part("wheel_"+id,"rubber",x,y,z,'x');
            String rim=part("wheel_hub_"+id,"metal",x,y,z,'x');
            for(int i=0;i<12;i++) {
                double yy=-r+i*r/6,half=Math.sqrt(Math.max(.01,r*r-Math.pow(yy+r/12,2)));
                box(tire,-width/2,yy,-half,width,r/6,2*half);
            }
            for(int side:new int[]{-1,1}) {
                box(rim,side*(width/2+.04)-.1,-r*.55,-r*.17,.2,r*1.1,r*.34);
                box(rim,side*(width/2+.04)-.1,-r*.17,-r*.55,.2,r*.34,r*1.1);
                box(rim,side*(width/2+.20)-.25,-.8,-.8,.5,1.6,1.6);
            }
            for(int i=0;i<12;i++) {
                double a=i*Math.PI/6,yy=Math.sin(a)*r*.9,zz=Math.cos(a)*r*.9;
                box(tire,-width/2-.06,yy-.28,zz-.28,width+.12,.56,.56);
            }
        }
        List<Part> finish() {
            List<Part> result=new ArrayList<>();
            for(var p:parts.values()) if(!boxes.get(p.name()).isEmpty())
                result.add(new Part(p.name(),p.material(),p.px(),p.py(),p.pz(),p.axis(),List.copyOf(boxes.get(p.name())),p.restPitch(),p.restYaw(),p.restRoll()));
            return List.copyOf(result);
        }
    }
    private static void instruments(Mesh m,VehicleType type) {
        Seat s=seat(type,0); boolean bike=type.family==VehicleType.Family.MOTORCYCLE;
        double forward=switch(type.family) { case PICKUP,PLANE,MOTORCYCLE,DRONE -> 9;case COMBINE -> 9.5;case DOZER -> 10.5;default -> 11; };
        double y=s.top()+(bike?4.6:5.5),z=s.z()+forward;
        double scale=bike?.62:type.family==VehicleType.Family.DRONE?.7:1;
        m.add("dark",-4*scale,y-2*scale,z-.1,8*scale,3.4*scale,bike?.65:1.3);
        String[] channels={"fuel","rpm","speed"};
        for(int i=0;i<3;i++) {
            double x=(i-1)*2.45*scale;
            m.add("brass",x-1.1*scale,y-1.1*scale,z-.3,2.2*scale,2.2*scale,.2);
            m.add("gauge",x-.9*scale,y-.9*scale,z-.42,1.8*scale,1.8*scale,.1);
            String needle=m.part("instrument_"+channels[i],"trim",x,y,z-.5,'n');
            m.box(needle,-.10*scale,0,-.04,.20*scale,.74*scale,.08);
        }
        if(!bike) {
            String throttle=m.part("throttle_lever","brass",-3.8,s.top()+2.5,s.z()+5,'a');
            m.box(throttle,-.16,0,-.16,.32,3.5,.32);
            m.box(throttle,-.6,2.8,-.45,1.2,.8,.9);
            for(int i=0;i<3;i++) m.add(i==0?"red":"metal",-3+i*1.6,y-1.9,z-.55,.7,.4,.25);
        }
    }
    /** Open-cockpit biplane; variants retain their existing registry/seat IDs. */
    public static List<Part> biplane(VehicleType type) {
        Mesh m=new Mesh(); boolean cargo=type==VehicleType.PLANE_CARGO;
        double span=type.blueprintWidth*16,tail=cargo?-28:-24,nose=22;
        String hull=m.part("fuselage","paint",0,0,0,' ');
        // Stepped taper and chamfered belly, with a deliberately empty cockpit.
        m.box(hull,-4.5,8,-10,9,3,3);m.box(hull,-4.5,8,-7,9,1,16);m.box(hull,-4.5,8,9,9,3,11);
        for(int side:new int[]{-1,1}) m.box(hull,side<0?-5.5:4.3,9,-7,1.2,2,16);
        m.add("metal",-1.5,9,-6.5,3,2,3);
        m.box(hull,-3.5,6.8,-8,7,1.2,25);
        m.box(hull,-5.5,11,4.5,1.5,8.5,15.5);m.box(hull,4,11,4.5,1.5,8.5,15.5);
        m.box(hull,-4,11,9,8,8.5,11);m.box(hull,-4,14.5,4.5,8,5,4.5);
        m.box(hull,-4.5,19.5,5.5,9,1.2,13);
        for(int side:new int[]{-1,1}) {
            m.box(hull,side<0?-5.5:4.3,11,-10,1.2,5,14.5);
            m.add("seat",side<0?-5.65:4.15,15.7,-10,1.5,.6,14.5);
            m.add("metal",side*5.7-.12,12,-9,.24,.4,12);
            m.add("trim",side*5.64-.12,12,-8,.24,.6,9);
            m.add("paint",side<0?-6.2:5,9.5,-7,1.2,.8,10);
        }
        double tailLength=-10-tail;
        for(int i=0;i<4;i++) {
            double w=4.8-i*.9,z=-10-(i+1)*tailLength/4;
            m.box(hull,-w,10+i*.7,z,w*2,6-i*.75,tailLength/4);
        }
        double bandZ=tail+5;
        m.add("trim",-3.05,11.32,bandZ,6.1,4.64,1.8);
        m.add("metal",-1,9.6,tail+1,2,1.2,6);
        m.rod("tail_skid","dark",0,7.5,tail+2,6,.7,'x',22.5);
        m.wheel("tail",0,3.5,tail+3,1.9,1.4);
        for(int level=0;level<2;level++) {
            double y=level==0?12.8:30.5,half=span/2-(level==0?1.2:0),z=level==0?5:4;
            for(int side:new int[]{-1,1}) {
                String wing=m.part("wing_"+(level==0?"lower":"upper")+"_"+side,"paint",0,0,0,' ');
                double start=5.3,end=half-.8,step=(end-start)/3;
                for(int panel=0;panel<3;panel++) {
                    double a=start+panel*step,b=a+step;
                    m.box(wing,side<0?-b:a,y,z,b-a,1.3,10.5);
                    m.box(wing,side<0?-b:a,y+.4,z+10.5,b-a,.5,1.2);
                }
                m.box(wing,side<0?-half:half-.8,y+.2,z+.8,.8,.9,9);
                double bandX=half-4;
                m.add("trim",side<0?-bandX-1.6:bandX,y-.05,z-.05,1.6,1.42,11.80);
                m.add("dark",side<0?-half+.6:half-1,y+.48,z+2,.4,.4,6);
                m.add(side<0?"red":"lamp",side<0?-half-.2:half-.4,y+.35,z+7,.6,.55,1.1);
                if(level==1) {
                    String aileron=m.part("aileron_"+side,"paint",side*(half*.64),y+.65,z,'e');
                    m.box(aileron,-half*.26,-.6,-2.2,half*.52,1.2,2.15);
                    String hinge=m.part("aileron_hinge_"+side,"metal",side*(half*.64),y+.65,z,'e');
                    for(int k=-1;k<=1;k++) m.box(hinge,k*half*.19-.18,-.3,-.5,.36,.6,1.1);
                }
            }
            if(level==1) {
                String bridge=m.part("wing_upper_center","paint",0,0,0,' ');
                m.box(bridge,-5.3,y,8,10.6,1.3,7.7);
                m.add("trim",-1.2,y+1.32,9.5,2.4,.15,4.5);
            }
        }
        for(int side:new int[]{-1,1}) {
            double x=side*(span/2-6);
            m.rod("interplane_front_"+side,"dark",x,21.65,12,16.4,1.1,'x',0);
            m.rod("interplane_rear_"+side,"dark",x,21.65,6.5,17.6,.7,'x',-22.5);
            m.add("metal",x-1.1,13.8,11.2,2.2,.65,2);
            m.add("metal",x-1.1,29.5,11.2,2.2,.65,2);
            m.rod("cabane_"+side,"dark",side*6.6,25.5,12,10.6,.9,'z',-side*22.5);
            m.rod("gear_leg_"+side,"metal",side*6.6,7.6,11,10.5,1.1,'z',side*22.5);
            m.rod("gear_brace_"+side,"dark",side*7,8.4,6.7,9,.6,'x',-45);
            m.add("dark",side*8.3-1.7,6.8,7,3.4,.65,8);
            m.wheel("gear"+side,side*8.3,4.1,10.2,4.1,2.7);
            for(int cylinder=0;cylinder<3;cylinder++) {
                double zz=11+cylinder*2.7;
                m.add("dark",side<0?-7.9:5.5,12.1,zz,2.4,4.5,2.2);
                for(int fin=0;fin<4;fin++) m.add("metal",side<0?-8.15:5.5,12.5+fin*.9,zz-.12,2.65,.27,2.44);
            }
            m.add("metal",side<0?-7.5:6.7,11.2,10,.8,.85,11.8);
            m.add("dark",side<0?-7.6:6.65,10.1,9.8,.95,1.5,1);
            m.add("brass",side*5.65-.3,17.5,18,.6,.7,.8);
        }
        m.add("metal",-9.2,3.8,9.75,18.4,.7,.9);
        m.add("dark",-4.9,10.1,nose-2,9.8,9,2.1);
        m.add("trim",-5.5,9.7,nose,11,1.2,1.2);
        m.add("trim",-5.5,19.3,nose,11,1.2,1.2);
        m.add("trim",-5.5,10.9,nose,1.2,8.4,1.2);
        m.add("trim",4.3,10.9,nose,1.2,8.4,1.2);
        for(int i=0;i<6;i++) m.add("metal",-3.7+i*1.35,11,nose+.16,.32,7.8,.35);
        String prop=m.part("propeller","wood",0,15.1,nose+2,'z');
        m.box(prop,-1.2,-11,-.35,2.4,8.8,.7); m.box(prop,-1.2,2.2,-.35,2.4,8.8,.7);
        m.box(prop,-.65,-3,-.45,1.3,6,.9);
        String tips=m.part("propeller_tips","trim",0,15.1,nose+2,'z');
        m.box(tips,-1.24,-11.02,-.39,2.48,1.54,.78); m.box(tips,-1.24,9.48,-.39,2.48,1.54,.78);
        m.add("metal",-1.25,13.85,nose+2.3,2.5,2.5,1.5);
        m.add("brass",-.5,14.6,nose+3.81,1,1,.24);
        m.add("paint",-11.2,14.5,tail+1.5,22.4,1.1,4.8);
        for(int side:new int[]{-1,1}) m.add("trim",side<0?-10.6:8.8,14.45,tail+1.5,1.8,1.2,4.8);
        String elevator=m.part("elevator","paint",0,15.05,tail+1.5,'e');
        m.box(elevator,-11.2,-.55,-2.1,22.4,1.1,2.05);
        for(int i=0;i<3;i++) m.add("paint",-.65,15.6+i*3,tail+2+i,1.3,3,5.5-i*1.1);
        String rudder=m.part("rudder","paint",0,20.1,tail+2,'v');
        m.box(rudder,-.65,-4.5,-2,1.3,9,1.95);
        String rudderTrim=m.part("rudder_trim","trim",0,20.1,tail+2,'v');
        m.box(rudderTrim,-.69,-4.5,-2.08,1.38,9,.45);
        Seat seat=seat(type,0);
        String cushion=m.part("seat_0","seat",seat.x(),seat.top(),seat.z(),' ');
        m.box(cushion,-3,-2,-3,6,2,6); m.box(cushion,-3,0,-3,6,6,1);
        m.add("wood",-3.35,seat.top()+5.5,seat.z()-3.2,6.7,.7,1.3);
        m.add("dark",-1.2,9.02,-4,2.4,.2,4.8);
        for(int side:new int[]{-1,1}) m.add("metal",side*2-.9,9.1,4.5,1.8,.3,2.5);
        String steering=m.part("steering","metal",0,seat.top()+5,seat.z()+8,'c');
        m.box(steering,-3,-.35,-.3,6,.7,.6); m.box(steering,-3,-.5,-1,1.1,1,1.7); m.box(steering,1.9,-.5,-1,1.1,1,1.7);
        m.box(steering,-.25,-3,-.25,.5,3,.5);
        m.add("dark",-4.65,19.2,5.7,9.3,.7,.8);
        m.add("glass",-4.05,19.9,5.9,8.1,5.4,.22);
        m.add("metal",-4.45,19.8,5.7,.5,5.9,.6); m.add("metal",3.95,19.8,5.7,.5,5.9,.6);
        m.add("metal",-4.45,25.2,5.7,8.9,.5,.6);
        if(cargo) {
            m.add("seat",-3.5,16.5,-18,7,3.3,5.8);
            for(int z:new int[]{-17,-14}) m.add("trim",-3.6,16.4,z,7.2,3.6,.45);
            m.add("metal",-3.7,20,-17.5,7.4,.55,.65);
        }
        instruments(m,type);
        return m.finish();
    }
    /** Enclosed utility helicopter, with head clearance and a faceted, sloping canopy. */
    public static List<Part> helicopter(VehicleType type) {
        Mesh m=new Mesh();boolean cargo=type==VehicleType.HELICOPTER_CARGO;double radius=type.blueprintWidth*10;
        m.add("paint",-8,7,-13,16,3,29);m.add("dark",-6,5.5,-11,12,1.5,25);
        m.add("paint",-8,10,-13,16,18,7);
        m.add("paint",-7,28,-12,14,4,6);m.add("paint",-6,32,-11,12,1.5,6);
        m.add("paint",-8.5,33,-8,17,1.5,21);m.add("trim",-7.9,34.5,-7.4,15.8,.45,19.8);
        for(int side:new int[]{-1,1}) {
            double x=side<0?-9:7.8;
            m.add("paint",x,10,-6,1.2,8,21);m.add("trim",side*9.05-.14,12,-8,.28,.9,23);
            m.add("dark",side*8.45-.45,18,-5,.9,15,1);
            m.add("dark",side*8.45-.45,18,11,.9,15,1);
            m.add("dark",side*8.45-.4,31.9,-5,.8,1.1,17);
            m.add("glass",side*8.8-.12,18.7,-3.8,.24,13,14.5);
            m.add("metal",side*9.17-.14,16,-2,.28,.6,2.6);
            m.add("metal",side*9.25-.18,17.8,-5,.36,.35,16);
            for(int z:new int[]{-4,8}) m.rod("skid_leg_"+side+"_"+z,"metal",side*8.8,5,z,8.5,1.1,'z',side*22.5);
            m.add("metal",side*10-.65,.3,-16,1.3,1.1,35);
            m.rod("skid_curve_"+side,"metal",side*10,2.1,20,4.7,1.1,'x',45);
            m.add("rubber",side*10-.75,.1,-8,1.5,.5,10);
            m.add("metal",side<0?-10.5:8,9.7,3,2.5,.6,8);
            m.add("dark",side*4.2-1.5,29,-12,3,3.4,5.2);
            for(int fin=0;fin<5;fin++) m.add("metal",side*4.2-1.4,29.3+fin*.55,-12.15,2.8,.22,.35);
            m.add("metal",side*4.2-1.2,29.6,-13,2.4,2.4,1.3);
            m.add("dark",side*4.2-.75,30,-13.12,1.5,1.5,.2);
            m.add(side<0?"red":"lamp",side*8.8-.45,32,7,.9,.75,1.2);
        }
        m.add("paint",-8,10,15,16,3.8,4);m.add("paint",-6.8,10.3,19,13.6,3,2.2);
        m.add("paint",-5.5,10.8,21.2,11,2,1.2);
        String glass=m.part("canopy_front","glass",0,23.4,16.2,' ',-22.5,0,0);
        m.box(glass,-7.75,-9.5,-.15,7.35,19,.3);m.box(glass,.4,-9.5,-.15,7.35,19,.3);
        String frame=m.part("canopy_frame","dark",0,23.4,16.2,' ',-22.5,0,0);
        m.box(frame,-8.4,-9.8,-.45,.65,19.6,.9);m.box(frame,7.75,-9.8,-.45,.65,19.6,.9);
        m.box(frame,-.35,-9.8,-.43,.7,19.6,.86);
        m.box(frame,-8.4,-9.8,-.45,16.8,.65,.9);m.box(frame,-8.4,9.15,-.45,16.8,.65,.9);
        m.add("lamp",-1.9,11.25,22.41,3.8,1,.35);
        for(int i=0;i<4;i++) { double half=2.6-i*.45;m.add("paint",-half,18+i*.55,-19-i*5.5,half*2,4-i*.5,6); }
        for(int i=0;i<3;i++) m.add("paint",-.7,22+i*2.8,-35.5+i*.7,1.4,2.8,5.2-i*1.2);
        m.add("trim",-.76,29,-34.2,1.52,.85,2.8);
        m.add("paint",-8,21.1,-30,16,1,4.5);m.add("metal",-.6,20.8,-32,4.7,1.2,1.2);
        for(int side:new int[]{-1,1}) m.add("trim",side<0?-8:6.4,21.05,-30.05,1.6,1.1,4.6);
        String tail=m.part("tail_rotor","metal",3.6,25,-33,'x');
        m.box(tail,-.4,-4.8,-.48,.8,9.6,.96);m.box(tail,-.4,-.48,-4.8,.8,.96,9.6);
        m.add("brass",4.02,24.3,-33.7,.5,1.4,1.4);
        m.add("red",-.6,30.4,-32.7,1.2,.8,1.2);
        m.add("metal",-.8,34.9,-4.8,1.6,5,1.6);
        String plate=m.part("swashplate","metal",0,37,-4,'j');m.box(plate,-2,-.4,-2,4,.8,4);
        for(int side:new int[]{-1,1}) m.add("brass",side*1.5-.2,35.2,-4.2,.4,3.8,.4);
        String rotor=m.part("rotor","dark",0,40,-4,'y');
        m.box(rotor,-radius+3,-.3,-1.3,radius-4,.6,2.6);m.box(rotor,1,-.3,-1.3,radius-4,.6,2.6);
        m.box(rotor,-1.3,-.3,-radius+3,2.6,.6,radius-4);m.box(rotor,-1.3,-.3,1,2.6,.6,radius-4);
        String tips=m.part("rotor_tips","trim",0,40,-4,'y');
        m.box(tips,-radius,-.3,-1.3,3,.6,2.6);m.box(tips,radius-3,-.3,-1.3,3,.6,2.6);
        m.box(tips,-1.3,-.3,-radius,2.6,.6,3);m.box(tips,-1.3,-.3,radius-3,2.6,.6,3);
        String hub=m.part("rotor_head","metal",0,40,-4,'y');m.box(hub,-2,-.8,-2,4,1.6,4);
        Seat s=seat(type,0);String cushion=m.part("seat_0","seat",s.x(),s.top(),s.z(),' ');
        m.box(cushion,-3,-2,-3,6,2,6);m.box(cushion,-3,0,-3,6,7,1);m.add("seat",-2.4,20,s.z()-3,4.8,2,1.1);
        for(int side:new int[]{-1,1}) {
            String lever=m.part(side<0?"lever_left":"lever_right","metal",side*3,s.top()+2,s.z()+7,side<0?'l':'r');
            m.box(lever,-.3,0,-.3,.6,4,.6);m.box(lever,-1,3,-.6,2,1,1.2);
        }
        if(cargo) {
            m.add("seat",-4,2.5,-6,8,3,6);
            for(int side:new int[]{-1,1}) m.add("metal",side*3-.25,5.5,-4,.5,1.5,1);
            for(int z:new int[]{-5,-2}) m.add("trim",-4.1,2.4,z,8.2,3.2,.4);
        }
        instruments(m,type);return m.finish();
    }
    private static void pickupCabin(Mesh m) {
        m.add("paint",-11.3,32.8,-6.2,22.6,1.1,16.8);
        m.add("trim",-10.8,33.9,-5.7,21.6,.4,15.8);
        m.add("paint",-10,17,10,20,3.2,3.9);
        m.add("paint",-10.5,13,-6,21,7,1);
        m.add("glass",-9.5,20,-5.55,19,12.5,.25);
        m.add("metal",-1.5,6,-2,3,4,3);
        for(int side:new int[]{-1,1}) {
            double x=side<0?-10.5:9.5;
            m.add("paint",x,13,-5,1,7,15);
            m.add("paint",x,20,-5.9,1,12.8,1);
            m.add("paint",x,31.9,-5,1,.9,14.5);
            m.add("glass",side*10-.13,20.2,-4.9,.26,11.7,13.3);
            m.add("metal",side*10.6-.16,18.4,-2,.32,.55,2.7);
            m.add("metal",side*12-.35,24,6,.7,.7,3.2);
            m.add("dark",side*13.5-.5,24,6,1,3.3,2.3);
        }
        String pane=m.part("pickup_windshield","glass",0,26.3,11,' ',-22.5,0,0);
        m.box(pane,-9.55,-6.7,-.13,19.1,13.4,.26);
        String frame=m.part("pickup_windshield_frame","paint",0,26.3,11,' ',-22.5,0,0);
        for(int side:new int[]{-1,1}) m.box(frame,side*10-.45,-6.9,-.42,.9,13.8,.84);
        m.box(frame,-10.45,-6.9,-.42,20.9,.6,.84);m.box(frame,-10.45,6.3,-.42,20.9,.6,.84);
        m.box(frame,-.16,-6.5,-.44,.32,12.9,.88);
    }
    public static List<Part> enhance(VehicleType type,List<Part> original) {
        Mesh m=new Mesh(); instruments(m,type);
        switch(type.family) {
            case COMBINE -> {
                for(int side:new int[]{-1,1}) {
                    m.add("trim",side*15.2-.2,20,-17,.4,1.6,20);
                    m.add("dark",side*9-2,45,16,4,1.7,1.8); m.add("lamp",side*9-1.6,45.3,17.8,3.2,1.1,.3);
                    m.add("red",side*11-1,18,-20.3,2,2,.5);
                    m.rod("header_ram_"+side,"metal",side*10,11,20,11,1.1,'x',45);
                    for(int j=0;j<3;j++) m.add("dark",side*13-1,26,-15+j*3,2,.4,1);
                }
                String cap=m.part("exhaust_cap","dark",12.5,39,-12.5,'k'); m.box(cap,-1,0,-.3,2,.4,2.4);
                String pulley=m.part("cooling_fan","metal",15.4,16,-7,'x'); m.box(pulley,-.2,-2,-.35,.4,4,.7); m.box(pulley,-.2,-.35,-2,.4,.7,4);
                double w=type.blueprintWidth*16;
                for(int i=0;i<8;i++) m.add("brass",-w/2+3+i*(w-6)/7,8.2,22,.55,.55,.3);
            }
            case DOZER -> {
                double w=type.blueprintWidth*16;
                String edge=m.part("blade_cutting_edge","metal",0,7,24,'b'); m.box(edge,-w/2,-4.6,-.6,w,.65,3.2);
                for(int i=0;i<9;i++) m.box(edge,-w/2+2+i*(w-4)/8,-3.6,2,.65,.65,.3);
                for(int side:new int[]{-1,1}) {
                    m.rod("blade_ram_"+side,"metal",side*11,11.4,16,11,1.3,'x',45);
                    m.add("dark",side*11-.95,13.2,10,1.9,2,6);
                    m.add("lamp",side*7-1.5,39,6.8,3,1.5,.6);
                    m.add("red",side*9-1,16,-17.4,2,1,.6);
                    for(int step=0;step<3;step++) m.add("metal",side<0?-14:11,8+step*3,-7,3,.6,5);
                }
                String cap=m.part("exhaust_cap","dark",9.6,33,7,'k'); m.box(cap,-1,0,-.3,2,.4,2.2);
            }
            case PICKUP -> {
                pickupCabin(m);
                double rear=type==VehicleType.PICKUP_CARGO?-18.5:-16;
                for(int side:new int[]{-1,1}) {
                    m.add("trim",side*12.1-.12,11.8,-5,.24,.7,19);
                    m.add("metal",side*12.2-.15,14.4,-2,.3,.5,2.6);
                    m.add("dark",side*10-2.5,14,16.2,5,2.3,.4);
                    m.add("lamp",side*10-2.1,14.3,16.62,4.2,1.7,.25);
                    m.add("red",side*10-1,12.8,rear-.4,2,3,.5);
                    m.add("rubber",side*11-2,5,-15,4,4,.5);
                    m.add("metal",side<0?-13:11,7.9,-4,2,.55,11);
                    for(int z:new int[]{-11,11}) {
                        m.add("paint",side<0?-14:11,8,z-6.2,3,2,1.2);
                        m.add("paint",side<0?-14:11,8,z+5,3,2,1.2);
                    }
                }
                for(int row=0;row<3;row++) m.add("metal",-5,11.2+row*.9,16.53,10,.32,.24);
                m.add("brass",-1,12.1,16.8,2,1.1,.2);
                m.add("metal",-2,16.1,rear-.25,4,.6,.35);
                m.add("dark",-9,4.5,rear-1.3,1.5,1.5,5);
                if(type==VehicleType.PICKUP_CARGO) for(int i=0;i<3;i++) m.add("wood",-8+i*5.5,14,-16,4.7,3.2,6);
            }
            case MOTORCYCLE -> {
                m.add("paint",-2.3,16.1,3.5,4.6,.7,5); m.add("metal",-.7,16.85,5,1.4,.3,1.6);
                for(int side:new int[]{-1,1}) {
                    m.add("dark",side<0?-5:4,6.5,-12,1,1,13);
                    m.add("metal",side<0?-5.2:4,6.4,-10,1.2,1.2,8);
                    m.add("dark",side*3.6-.45,7.5,-9,.9,5.5,.9);
                    for(int i=0;i<5;i++) m.add("metal",side*3.6-.7,8+i*.9,-9.25,1.4,.3,1.4);
                    for(int passenger=0;passenger<2;passenger++) {
                        Seat s=seat(type,passenger);m.add("metal",side<0?-7.4:4.9,s.top()-3.6,s.z()+8.8,2.5,.5,3);
                    }
                    m.add("metal",side*4.5-.3,16.1,-13,.6,.6,4);
                    for(int z:new int[]{-12,-10}) m.add("metal",side*4.5-.3,13,z,.6,3.1,.6);
                    m.add("metal",side<0?-4.5:2.7,13,-12,1.8,.6,2);
                }
                String lamp=m.part("front_lamp","lamp",0,5.5,12,'u'); m.box(lamp,-1.6,11.1,-.5,3.2,2.5,.8);
                String mount=m.part("front_lamp_rim","metal",0,5.5,12,'u'); m.box(mount,-2,10.7,-1,4,3.3,.45);
                m.add("seat",-3,13,-8.5,6,2,6);m.add("paint",-3,11.8,-14,6,1.1,10);
                m.add("red",-1.5,13,-15.3,3,1.1,.4);
                String stand=m.part("parking_stand","metal",-2,8,-2,'p'); m.box(stand,-.3,-7.7,-.3,.6,7.7,.6); m.box(stand,-1.2,-8,-.8,2,.4,1.6);
                if(type==VehicleType.MOTORCYCLE_TOURING) {
                    m.add("trim",-2.5,23,-15,5,.5,1); m.add("metal",-2.5,16,-15,.5,7,.5); m.add("metal",2,16,-15,.5,7,.5);
                    for(int side:new int[]{-1,1}) m.add("brass",side*5-.7,12,-13.2,1.4,.7,.3);
                }
            }
            case BOAT -> {
                double w=type.blueprintWidth*16,back=-boatLength(type)/2;
                m.add("metal",-1.5,6.6,-3,3,2.4,3);
                m.rod("steering_column","metal",0,12.5,8.2,7.8,.6,'x',-22.5);
                for(int side:new int[]{-1,1}) {
                    m.add("trim",side*(w/2-1)-.4,5.1,back+2,.8,.9,-back+7);
                    m.add("rubber",side*(w/2+.05)-.45,7.2,-6,.9,2.4,3.5);
                    m.add("metal",side*(w/2-3)-1,10,-11,2,.5,1.2);
                    m.add(side<0?"red":"lamp",side*(w/2-3)-.6,10,5,1.2,.8,1.6);
                }
                for(int i=0;i<7;i++) m.add("wood",-w/2+4+i*(w-8)/7,6.65,back+3,(w-8)/7-.2,.2,9);
                String cover=m.part("outboard_cover","paint",0,5,back-1,'u'); m.box(cover,-2.6,6.5,-1.8,5.2,2,3.6);
                String badge=m.part("outboard_badge","trim",0,5,back-1,'u'); m.box(badge,-2.1,6.8,-2.08,4.2,.6,.2);
                for(int step=0;step<3;step++) m.add("metal",w/2-6,4-step*2,back-1.2,4,.5,.6);
            }
            case DRONE -> {
                double r=type.blueprintWidth*6;
                for(int side:new int[]{-1,1}) {
                    m.add("paint",side*3-1.3,17,5,2.6,1.4,4);
                    m.rod("control_support_"+side,"metal",side*3,16.5,4.75,5.2,.6,'x',45);
                    m.add("metal",side*2.5-.2,18.4,8.9,.4,1.7,.4);
                }
                for(int x:new int[]{-1,1}) for(int z:new int[]{-1,1}) {
                    m.add("metal",x*r-1.2,7.5,z*r-1.2,2.4,.45,2.4);
                    m.add(x<0?"red":"lamp",x*r-.55,5.1,z*r-.55,1.1,.7,1.1);
                    String tip=m.part("tip_rotor_"+x+"_"+z,"trim",x*r,9,z*r,'y'); m.box(tip,-5,-.28,-.62,1.4,.56,1.24); m.box(tip,3.6,-.28,-.62,1.4,.56,1.24);
                    m.add("rubber",x*r-.8,.6,z*r-.8,1.6,.5,1.6);
                }
                m.add("metal",-2.3,3.8,4.7,4.6,.5,2.2);
                String camera=m.part("camera_gimbal","dark",0,2.6,5.5,'g'); m.box(camera,-1.5,-1.1,-.6,3,2.2,1.6);
                String lens=m.part("camera_lens","glass",0,2.6,5.5,'g'); m.box(lens,-.75,-.75,1.05,1.5,1.5,.3);
                for(int i=0;i<4;i++) m.add("dark",-2.8+i*1.6,7.8,-4,.6,.4,2.4);
                m.add("brass",-.7,8.1,1,1.4,.3,1.4);
            }
            default -> {}
        }
        List<Part> result=new ArrayList<>(original); result.addAll(m.finish()); return List.copyOf(result);
    }
}
