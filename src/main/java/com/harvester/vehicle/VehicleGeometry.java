package com.harvester.vehicle;

import java.util.*;

/** Original geometry: model pixels, +Y up, +Z forward. Seats are the single attachment source. */
public final class VehicleGeometry {
    public record Cuboid(float x,float y,float z,float w,float h,float d) {}
    public record Part(String name,String material,float px,float py,float pz,char axis,List<Cuboid> boxes) {}
    public record Seat(double x,double top,double z) {}
    public record TrackPoint(double y,double z,double angle) {}
    public static final double TRACK_PERIMETER=56+12*Math.PI;
    public static final List<String> MATERIALS=List.of("paint","metal","rubber","glass","seat","accent","dark");
    private VehicleGeometry() {}
    public static Seat seat(VehicleType type,int index) {
        return switch(type.family) {
            case COMBINE -> new Seat(0,26,6.5);
            case DOZER -> new Seat(0,20,-5.5);
            case PICKUP -> new Seat(0,15,-.5);
            case MOTORCYCLE -> index<=0?new Seat(0,15,.5):new Seat(0,16,-11.5);
            case BOAT -> new Seat(0,9,-1.5);
            case PLANE -> new Seat(0,13,-5);
            case HELICOPTER -> new Seat(0,13,3.5);
            case DRONE -> new Seat(0,14,0);
        };
    }
    public static TrackPoint trackPoint(double distance) {
        double t=((distance%TRACK_PERIMETER)+TRACK_PERIMETER)%TRACK_PERIMETER;
        if(t<28) return new TrackPoint(13,-14+t,0);
        t-=28;
        if(t<6*Math.PI) { double a=t/6; return new TrackPoint(7+6*Math.cos(a),14+6*Math.sin(a),a); }
        t-=6*Math.PI;
        if(t<28) return new TrackPoint(1,14-t,Math.PI);
        double a=(t-28)/6;
        return new TrackPoint(7-6*Math.cos(a),-14-6*Math.sin(a),Math.PI+a);
    }
    public static int paintColor(VehicleType type) {
        return switch(type.family) {
            case COMBINE -> 0x8ea849; case DOZER -> 0xd8aa39; case PICKUP -> 0x437a98;
            case MOTORCYCLE -> 0xa9473f; case BOAT -> 0xe0dfcf; case PLANE -> 0xd5d4c9;
            case HELICOPTER -> 0x6b8071; case DRONE -> 0xc1c8cd;
        };
    }
    public static int materialColor(String material,VehicleType type) {
        return switch(material) {
            case "paint" -> paintColor(type); case "metal" -> 0x909da3; case "rubber" -> 0x24282b;
            case "glass" -> 0x608fa5; case "seat" -> 0x464e51; case "accent" -> 0xd7a242; default -> 0x363f44;
        };
    }
    private static final class Builder {
        final Map<String,List<Cuboid>> boxes=new LinkedHashMap<>();
        final Map<String,Part> parts=new LinkedHashMap<>();
        String group(String name,String material,double x,double y,double z,char axis) {
            parts.putIfAbsent(name,new Part(name,material,(float)x,(float)y,(float)z,axis,List.of()));
            boxes.computeIfAbsent(name,k->new ArrayList<>()); return name;
        }
        void add(String material,double x,double y,double z,double w,double h,double d) {
            addTo(group(material,material,0,0,0,' '),x,y,z,w,h,d);
        }
        void addTo(String group,double x,double y,double z,double w,double h,double d) {
            if(w<=0||h<=0||d<=0) throw new IllegalArgumentException("Nonpositive cuboid");
            boxes.get(group).add(new Cuboid((float)x,(float)y,(float)z,(float)w,(float)h,(float)d));
        }
        void wheel(String id,double x,double y,double z,double width,double radius) {
            String tire=group("wheel_"+id,"rubber",x,y,z,'x'),hub=group("wheel_hub_"+id,"metal",x,y,z,'x');
            cylinder(tire,-width/2,width,radius); cylinder(hub,-width/2-.15,width+.3,radius*.46);
            for(int i=0;i<8;i++) { double a=i*Math.PI/4,yy=Math.sin(a)*radius*.66,zz=Math.cos(a)*radius*.66; addTo(tire,-width/2-.2,yy-.45,zz-.45,width+.4,.9,.9); }
        }
        void cylinder(String group,double x,double width,double r) {
            for(int i=0;i<10;i++) { double y=-r+i*2*r/10,half=Math.sqrt(Math.max(0,r*r-Math.pow(y+r/10,2))); addTo(group,x,y,-half,width,2*r/10,half*2); }
        }
        void axle(String id,double width,double z,double r) {
            add("metal",-width/2,r-.6,z-.6,width,1.2,1.2);
            wheel(id+"l",-width/2,r,z,3.8,r); wheel(id+"r",width/2,r,z,3.8,r);
        }
        void cabin(double width,double bottom,double height,double z,double length) {
            // Open roof: side rails never cross the player's head or first-person camera.
            for(int side:new int[]{-1,1}) {
                add("paint",side*(width/2-.6)-.5,bottom,z,1,height,1);
                add("paint",side*(width/2-.6)-.5,bottom,z+length-1,1,height,1);
                add("paint",side*(width/2-.6)-.5,bottom+height,z,1,1,length);
                add("glass",side*(width/2-.6)-.15,bottom+1,z+1,.3,height-2,length-2);
                add("metal",side*(width/2+1.5)-.35,bottom+height*.6,z+length-2,.7,.7,3);
                add("dark",side*(width/2+3)-.5,bottom+height*.6,z+length-2,1,3,2);
            }
            add("glass",-width/2+1,bottom+1,z+length-.35,width-2,height-2,.3);
            add("glass",-width/2+1,bottom+1,z+.1,width-2,height-2,.3);
            add("dark",-width/2+1,bottom+2,z+length-3,width-2,2,2);
        }
        void vents(double x,double y,double z,int count) { for(int i=0;i<count;i++) add("dark",x,y+i*1.5,z,.35,.65,7); }
        List<Part> finish() {
            List<Part> result=new ArrayList<>();
            for(var e:parts.entrySet()) { Part p=e.getValue(); result.add(new Part(p.name,p.material,p.px,p.py,p.pz,p.axis,List.copyOf(boxes.get(e.getKey())))); }
            return List.copyOf(result);
        }
    }
    public static List<Part> create(VehicleType type) {
        Builder b=new Builder();
        boolean cargo=type.slots>switch(type.family) { case COMBINE,DOZER,PICKUP -> 27; case MOTORCYCLE,DRONE -> 9; default -> 18; };
        switch(type.family) {
            case COMBINE -> {
                double w=type.width*16;
                b.add("dark",-15,8,-21,30,3,42); b.add("paint",-15,11,-20,30,13,27);
                b.add("paint",-12,24,-18,24,5,15); b.add("dark",-10,29,-16,20,.5,11);
                for(int side:new int[]{-1,1}) { b.add("metal",side*12-.4,28,-18,.8,5,15); b.vents(side*15.2,13,-15,6); b.add("paint",side*15-3,18,5,6,1,14); }
                b.cabin(21,24,15,2,16); b.add("paint",-12,20,2,24,4,19);
                b.axle("front",32,10,8); b.axle("rear",28,-15,5.5);
                for(int i=0;i<5;i++) b.add("metal",-19,5+i*3,1,4,.7,6);
                b.add("metal",-19,4,1,.7,19,.7); b.add("metal",-19,4,6,.7,19,.7);
                b.add("dark",12,24,-12,2,15,2); b.add("metal",11.5,38,-12.5,3,1,3);
                b.add("paint",15,28,-18,2.5,2.5,28); b.add("metal",15,26,8,2.5,4,2.5);
                String head=b.group("header_frame","paint",0,7,26,'h'); b.addTo(head,-w/2,0,-4,w,3,9);
                b.addTo(head,-w/2,-1,-5,1,5,13); b.addTo(head,w/2-1,-1,-5,1,5,13);
                String teeth=b.group("header_teeth","metal",0,7,26,'h');
                for(double x=-w/2+1;x<w/2-1;x+=2) b.addTo(teeth,x,0,4,.7,.6,4);
                String reel=b.group("reel","dark",0,12,28,'x'); b.addTo(reel,-w/2+2,-.65,-.65,w-4,1.3,1.3);
                for(int i=0;i<4;i++) { double a=i*Math.PI/2,y=Math.sin(a)*3.2,z=Math.cos(a)*3.2; b.addTo(reel,-w/2+2,y-.45,z-.45,w-4,.9,.9); for(double x=-w/2+3;x<w/2-2;x+=5) b.addTo(reel,x,y-.5,z-.5,.5,2,.5); }
                if(cargo) b.add("paint",-12,29,-18,24,3,1);
            }
            case DOZER -> {
                double w=type.width*16;
                b.add("dark",-15,6,-18,30,4,36); b.add("paint",-12,10,-17,24,8,28);
                b.cabin(18,18,14,-10,17); b.add("paint",-11,13,7,22,8,9);
                b.vents(-11.3,14,8,4); b.vents(11.1,14,8,4);
                for(int side:new int[]{-1,1}) {
                    b.add("rubber",side*17-3,2,-18,6,3,36); b.add("rubber",side*17-3,11,-18,6,3,36);
                    for(int i=0;i<5;i++) b.wheel("track"+side+"_"+i,side*17,7,-14+i*7,5,4.8);
                    for(int i=0;i<32;i++) { var p=trackPoint(i*TRACK_PERIMETER/32); String t=b.group("track_"+side+"_"+i,"metal",side*17,p.y,p.z,'t'); b.addTo(t,-3,-.25,-.6,6,.5,1.2); }
                    b.add("metal",side*11-1,8,10,2,2,15); b.add("dark",side*10-1,12,8,2,2,10);
                }
                String blade=b.group("working_blade","paint",0,7,24,'b');
                for(int row=0;row<5;row++) b.addTo(blade,-w/2,-4+row*2,-Math.abs(row-2)*.7,w,2,1.8);
                b.addTo(blade,-w/2,-4.5,-.5,w,.7,3);
                for(int x=-2;x<=2;x++) b.addTo(blade,x*w/5-.4,-2,1,.8,6,.5);
                b.add("dark",9,20,7,1.6,13,1.6);
            }
            case PICKUP -> {
                double length=cargo?37:32;
                b.add("dark",-11,5,-length/2,22,3,length); b.add("paint",-12,8,-length/2,24,5,length);
                b.axle("front",23,11,5); b.axle("rear",23,-11,5);
                b.add("paint",-11,13,7,22,4,9); b.cabin(21,13,11,-5,13);
                b.add("seat",-9,13,-length/2+1,18,1,length/2-6);
                b.add("paint",-12,13,-length/2,2,5,length/2-5); b.add("paint",10,13,-length/2,2,5,length/2-5); b.add("paint",-12,13,-length/2,24,5,1.5);
                b.add("metal",-12,7,16,24,2,1.5); b.add("metal",-12,7,-length/2-1,24,2,1.5);
                for(int i=0;i<6;i++) b.add("dark",-5+i*2,11,16.1,1,3,.35);
                for(int side:new int[]{-1,1}) { b.add("accent",side*9-2,12,16.1,4,2,.4); b.add("metal",side*11.9,14,0,.3,.6,2); for(int axle:new int[]{-1,1}) b.add("paint",side*10-2,10,axle*11-6,4,1,12); }
                if(cargo) { b.add("metal",-10,18,-15,1,7,1); b.add("metal",9,18,-15,1,7,1); b.add("metal",-10,25,-15,20,1,1); }
            }
            case MOTORCYCLE -> {
                b.wheel("front",0,5.5,12,2.8,5.5); b.wheel("rear",0,5.5,-12,3.5,5.5);
                b.add("metal",-1.5,6,-12,3,2,24); b.add("dark",-3.8,7,-3,7.6,6,8);
                for(int i=0;i<4;i++) b.add("metal",-4,8+i,0,8,.35,4);
                b.add("paint",-2.8,13,3,5.6,3,6);
                String fork=b.group("front_fork","metal",0,5.5,12,'u');
                for(int side:new int[]{-1,1}) { b.addTo(fork,side*2-.4,-.5,-2,.8,14,.8); b.add("metal",side*4-.6,7,-12,1.2,1.2,10); b.add("metal",side*5-.7,12,5,1.4,.7,5); }
                b.add("accent",-2,16,10,4,2,1); b.add("paint",-2,11,9,4,1,7); b.add("paint",-2.5,11,-15,5,1,7);
                if(cargo) for(int side:new int[]{-1,1}) { b.add("seat",side*5-2,8,-13,4,6,7); b.add("metal",side*5-2,14,-13,4,.5,7); }
            }
            case BOAT -> {
                double w=type.width*16,length=cargo?40:34;
                for(int layer=0;layer<5;layer++) b.add("paint",-w/2+5-layer,layer*1.3,-length/2+3-layer,w-10+layer*2,1.3,length-6+layer*2);
                b.add("dark",-w/2+2,6,-length/2+2,w-4,.6,length-4);
                for(int side:new int[]{-1,1}) { b.add("paint",side*(w/2-1)-1,6,-length/2,2,5,length-4); b.add("metal",side*(w/2-2)-.3,11,-length/2,.6,.6,length-4); for(int i=0;i<3;i++) b.add("metal",side*(w/2-3)-.3,8,i*7-8,.6,4,.6); }
                b.add("paint",-w/2+2,6,length/2-6,w-4,3,6); b.add("paint",-w/2+3,8,2,w-6,3,3); b.add("glass",-w/2+3,11,3,w-6,4,.5);
                String motor=b.group("outboard","dark",0,5,-length/2-1,'u'); b.addTo(motor,-3,0,-2,6,8,4); b.addTo(motor,-1,-5,-1,2,6,2);
                String prop=b.group("propeller","metal",0,1,-length/2-3,'z'); b.addTo(prop,-4,-.4,-.4,8,.8,.8); b.addTo(prop,-.4,-4,-.4,.8,8,.8);
            }
            case PLANE -> {
                double span=type.width*16,length=cargo?54:44;
                b.add("paint",-5,8,-length/2,10,8,length); b.add("paint",-4,6,-length/2+2,8,2,length-4);
                for(int side:new int[]{-1,1}) {
                    b.add("paint",side*(span/4+3)-span/4,10,-3,span/2,1.5,9);
                    String flap=b.group("aileron_"+side,"paint",side*(span/4+3),10.75,-3,'e'); b.addTo(flap,-span/4,-.75,-2,span/2,1.5,2);
                    b.add("metal",side*(span/2-2)-.3,11,-4,.6,.4,9); b.add("accent",side*(span/2-1)-1,10,1,2,1.6,3);
                    b.add("metal",side*7-.5,3,-1,1,7,1); b.wheel("gear"+side,side*7,3,-1,2,3);
                }
                b.add("paint",-span*.24,12,-length/2+2,span*.48,1,5);
                String elevator=b.group("elevator","paint",0,12.5,-length/2+2,'e'); b.addTo(elevator,-span*.24,-.5,-2,span*.48,1,2);
                b.add("paint",-.7,13,-length/2+2,1.4,9,5);
                String rudder=b.group("rudder","paint",0,17.5,-length/2+2,'v'); b.addTo(rudder,-.7,-4.5,-2,1.4,9,2);
                b.add("dark",-3,9,length/2,6,6,2);
                String prop=b.group("propeller","dark",0,12,length/2+2,'z'); b.addTo(prop,-.6,-9,-.6,1.2,18,1.2); b.addTo(prop,-9,-.6,-.6,18,1.2,1.2);
                b.add("metal",-1,11,length/2+2,2,2,2);
            }
            case HELICOPTER -> {
                b.add("paint",-9,9,-11,18,12,28); b.add("paint",-7,7,-10,14,2,26);
                for(int side:new int[]{-1,1}) { b.add("glass",side*8.9,13,1,.4,7,9); b.add("paint",side*8.8,10,-8,.5,11,8); b.add("metal",side*9-.4,12,-2,.8,.6,2); b.add("metal",side*10-.6,3,-15,1.2,1.2,33); for(int z:new int[]{-8,8}) b.add("metal",side*8-.5,4,z,1,5,1); }
                b.add("paint",-2,13,-35,4,4,24); b.add("paint",-.8,15,-35,1.6,9,5); b.add("paint",-7,15,-29,14,1,4);
                String rotor=b.group("rotor","dark",0,40,-4,'y'); double r=type.width*10;
                b.addTo(rotor,-r,-.3,-1.2,r*2,.6,2.4); b.addTo(rotor,-1.2,-.3,-r,2.4,.6,r*2);
                String tail=b.group("tail_rotor","metal",3,19,-32,'x'); b.addTo(tail,-.3,-4,-.5,.6,8,1); b.addTo(tail,-.3,-.5,-4,.6,1,8);
                if(cargo) b.add("metal",-3,5,-2,6,2,4);
            }
            case DRONE -> {
                double r=type.width*6;
                b.add("paint",-4,5,-5,8,3,10); b.add("dark",-3,4,-4,6,1,8); b.add("glass",-1.7,3,5,3.4,2,1.5);
                b.add("metal",-3,8,-3,6,4,6);
                for(int side:new int[]{-1,1}) b.add("metal",side*4-.6,11.5,5,1.2,.6,7);
                for(int a:new int[]{-1,1}) for(int c:new int[]{-1,1}) {
                    b.add("metal",Math.min(0,a*r),6,c*r-.5,r,1,1); b.add("metal",a*r-.5,6,Math.min(0,c*r),1,1,r);
                    b.add("dark",a*r-1.5,6,c*r-1.5,3,2,3); String rotor=b.group("rotor_"+a+"_"+c,"dark",a*r,9,c*r,'y');
                    b.addTo(rotor,-5,-.25,-.6,10,.5,1.2); b.add("metal",a*r-.4,1,c*r-.4,.8,5,.8);
                }
                if(cargo) b.add("seat",-4,1,-4,8,3,8);
            }
        }
        // Cut actual empty cockpits out of the old solid fuselages, not an opaque overlay.
        List<Part> hull=b.finish();
        if(type.family==VehicleType.Family.PLANE) hull=cut(hull,-4.2,11,-8,4.2,60,6);
        if(type.family==VehicleType.Family.HELICOPTER) hull=cut(hull,-8,11,-9,8,60,17);
        Builder cabin=new Builder();
        for(int i=0;i<type.seats;i++) {
            Seat s=seat(type,i); String group=cabin.group("seat_"+i,"seat",s.x,s.top,s.z,' ');
            cabin.addTo(group,-3,-2,-3,6,2,6); cabin.addTo(group,-3,0,-3,6,6,1);
        }
        Seat s=seat(type,0);
        if(type.family==VehicleType.Family.PLANE) cabin.add("glass",-4.2,16,6,8.4,7,.35);
        if(type.family==VehicleType.Family.HELICOPTER) {
            cabin.add("glass",-7,13,17,14,10,.35); cabin.add("dark",-3,18,-10,6,6,7); cabin.add("metal",-.7,24,-4,1.4,16,1.4);
        }
        if(type.family==VehicleType.Family.DOZER || type.verticalAircraft()) {
            for(int side:new int[]{-1,1}) { String lever=cabin.group(side<0?"lever_left":"lever_right","metal",side*3,s.top+2,s.z+7,side<0?'l':'r'); cabin.addTo(lever,-.3,0,-.3,.6,4,.6); cabin.addTo(lever,-1,3,-.6,2,1,1.2); }
        } else {
            boolean bike=type.family==VehicleType.Family.MOTORCYCLE;
            String control=cabin.group("steering","metal",0,s.top+5,s.z+9,bike?'u':'c');
            if(bike) { cabin.addTo(control,-6,-.4,-.4,12,.8,.8); cabin.addTo(control,-6,-.6,-1,2,1.2,2); cabin.addTo(control,4,-.6,-1,2,1.2,2); }
            else { cabin.addTo(control,-3,-2,-.3,6,.5,.6); cabin.addTo(control,-3,1.5,-.3,6,.5,.6); cabin.addTo(control,-3,-2,-.3,.5,4,.6); cabin.addTo(control,2.5,-2,-.3,.5,4,.6); cabin.addTo(control,-.25,-2,-.3,.5,4,.6); }
        }
        List<Part> result=new ArrayList<>(hull); result.addAll(cabin.finish()); return List.copyOf(result);
    }
    /** Axis-aligned subtraction into disjoint slabs; keep all moving parts outside this operation. */
    private static List<Part> cut(List<Part> parts,double x0,double y0,double z0,double x1,double y1,double z1) {
        List<Part> out=new ArrayList<>();
        for(Part p:parts) {
            if(p.axis!=' ') { out.add(p); continue; }
            List<Cuboid> boxes=new ArrayList<>();
            for(Cuboid b:p.boxes) {
                double ax=b.x,bx=b.x+b.w,ay=b.y,by=b.y+b.h,az=b.z,bz=b.z+b.d;
                double lx=Math.max(ax,x0),hx=Math.min(bx,x1),ly=Math.max(ay,y0),hy=Math.min(by,y1),lz=Math.max(az,z0),hz=Math.min(bz,z1);
                if(lx>=hx||ly>=hy||lz>=hz) { boxes.add(b); continue; }
                slab(boxes,ax,ay,az,lx,by,bz); slab(boxes,hx,ay,az,bx,by,bz);
                slab(boxes,lx,ay,az,hx,ly,bz); slab(boxes,lx,hy,az,hx,by,bz);
                slab(boxes,lx,ly,az,hx,hy,lz); slab(boxes,lx,ly,hz,hx,hy,bz);
            }
            if(!boxes.isEmpty()) out.add(new Part(p.name,p.material,p.px,p.py,p.pz,p.axis,List.copyOf(boxes)));
        }
        return List.copyOf(out);
    }
    private static void slab(List<Cuboid> out,double x,double y,double z,double xx,double yy,double zz) {
        if(xx-x>.001 && yy-y>.001 && zz-z>.001) out.add(new Cuboid((float)x,(float)y,(float)z,(float)(xx-x),(float)(yy-y),(float)(zz-z)));
    }
}
