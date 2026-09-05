package com.harvester.vehicle;

import java.util.*;

/** Original mod geometry in model pixels (16 = one world block), forward +Z, up +Y. */
public final class VehicleGeometry {
    public record Cuboid(float x,float y,float z,float w,float h,float d) {}
    public record Part(String name,String material,float px,float py,float pz,char axis,List<Cuboid> boxes) {}
    public static final List<String> MATERIALS=List.of("paint","metal","rubber","glass","seat","accent","dark");
    private VehicleGeometry() {}
    public static int paintColor(VehicleType type) {
        return switch(type.family) {
            case COMBINE -> 0x8ea849;
            case DOZER -> 0xd8aa39;
            case PICKUP -> 0x437a98;
            case MOTORCYCLE -> 0xa9473f;
            case BOAT -> 0xe0dfcf;
            case PLANE -> 0xd5d4c9;
            case HELICOPTER -> 0x6b8071;
            case DRONE -> 0xc1c8cd;
        };
    }
    public static int materialColor(String material,VehicleType type) {
        return switch(material) {
            case "paint" -> paintColor(type);
            case "metal" -> 0x909da3;
            case "rubber" -> 0x24282b;
            case "glass" -> 0x608fa5;
            case "seat" -> 0x464e51;
            case "accent" -> 0xd7a242;
            default -> 0x363f44;
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
            String tire=group("wheel_"+id,"rubber",x,y,z,'x');
            String hub=group("wheel_hub_"+id,"metal",x,y,z,'x');
            cylinder(tire,-width/2,width,radius);
            cylinder(hub,-width/2-.15,width+.3,radius*.46);
            for(int i=0;i<8;i++) {
                double a=i*Math.PI/4, yy=Math.sin(a)*radius*.66, zz=Math.cos(a)*radius*.66;
                addTo(tire,-width/2-.2,yy-.45,zz-.45,width+.4,.9,.9);
            }
        }
        void cylinder(String group,double x,double width,double r) {
            int slices=10;
            for(int i=0;i<slices;i++) {
                double y=-r+i*2*r/slices;
                double half=Math.sqrt(Math.max(0,r*r-Math.pow(y+r/slices,2)));
                addTo(group,x,y,-half,width,2*r/slices,half*2);
            }
        }
        void axle(String id,double width,double z,double r) {
            add("metal",-width/2,r-.6,z-.6,width,1.2,1.2);
            wheel(id+"l",-width/2,r,z,3.8,r); wheel(id+"r",width/2,r,z,3.8,r);
        }
        void cabin(double width,double bottom,double height,double z,double length) {
            add("paint",-width/2-.5,bottom+height,z,width+1,1.2,length);
            for(int side:new int[]{-1,1}) {
                add("paint",side*(width/2-.6)-.5,bottom,z,1,height,1);
                add("paint",side*(width/2-.6)-.5,bottom,z+length-1,1,height,1);
                add("glass",side*(width/2-.6)-.15,bottom+1,z+1,.3,height-2,length-2);
                add("metal",side*(width/2+1.5)-.35,bottom+height*.6,z+length-2,.7,.7,3);
                add("dark",side*(width/2+3)-.5,bottom+height*.6,z+length-2,1,3,2);
            }
            add("glass",-width/2+1,bottom+1,z+length-.35,width-2,height-2,.3);
            add("glass",-width/2+1,bottom+1,z+.1,width-2,height-2,.3);
            add("seat",-3,bottom, z+2,6,2,5); add("seat",-3,bottom+2,z+2,6,5,1.2);
            add("dark",-width/2+1,bottom+2,z+length-3,width-2,2,2);
            add("metal",-2,bottom+5,z+length-3,4,.5,.5);
        }
        void vents(double x,double y,double z,int count) {
            for(int i=0;i<count;i++) add("dark",x,y+i*1.5,z,.35,.65,7);
        }
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
                b.add("dark",-15,8,-21,30,3,42);
                b.add("paint",-15,11,-20,30,13,27);
                b.add("paint",-12,24,-18,24,5,15);
                b.add("dark",-10,29,-16,20,.5,11);
                for(int side:new int[]{-1,1}) {
                    b.add("metal",side*12-.4,28,-18,.8,5,15);
                    b.vents(side*15.2,13,-15,6);
                    b.add("paint",side*15-3,18,5,6,1,14);
                }
                b.cabin(21,24,15,2,16);
                b.add("paint",-12,20,2,24,4,19);
                b.axle("front",32,10,8); b.axle("rear",28,-15,5.5);
                for(int i=0;i<5;i++) b.add("metal",-19,5+i*3,1,4,.7,6);
                b.add("metal",-19,4,1,.7,19,.7); b.add("metal",-19,4,6,.7,19,.7);
                b.add("dark",12,24,-12,2,15,2); b.add("metal",11.5,38,-12.5,3,1,3);
                b.add("paint",15,28,-18,2.5,2.5,28); b.add("metal",15,26,8,2.5,4,2.5);
                String head=b.group("header_frame","paint",0,7,26,'h');
                b.addTo(head,-w/2,0,-4,w,3,9);
                b.addTo(head,-w/2,-1,-5,1,5,13); b.addTo(head,w/2-1,-1,-5,1,5,13);
                String teeth=b.group("header_teeth","metal",0,7,26,'h');
                for(double x=-w/2+1;x<w/2-1;x+=2) b.addTo(teeth,x,0,4,.7,.6,4);
                String reel=b.group("reel","dark",0,12,28,'x');
                b.addTo(reel,-w/2+2,-.65,-.65,w-4,1.3,1.3);
                for(int i=0;i<4;i++) {
                    double a=i*Math.PI/2, y=Math.sin(a)*3.2,z=Math.cos(a)*3.2;
                    b.addTo(reel,-w/2+2,y-.45,z-.45,w-4,.9,.9);
                    for(double x=-w/2+3;x<w/2-2;x+=5) b.addTo(reel,x,y-.5,z-.5,.5,2,.5);
                }
                if(cargo) b.add("paint",-12,29,-18,24,3,1);
            }
            case DOZER -> {
                double w=type.width*16;
                b.add("dark",-15,6,-18,30,4,36); b.add("paint",-12,10,-17,24,8,28);
                b.cabin(18,18,14,-10,17);
                b.add("paint",-11,13,7,22,8,9);
                b.vents(-11.3,14,8,4); b.vents(11.1,14,8,4);
                for(int side:new int[]{-1,1}) {
                    b.add("rubber",side*17-3,2,-18,6,3,36);
                    b.add("rubber",side*17-3,11,-18,6,3,36);
                    for(int i=0;i<5;i++) b.wheel("track"+side+"_"+i,side*17,7,-14+i*7,5,4.8);
                    for(int i=0;i<12;i++) {
                        b.add("metal",side*17-3,1.7,-17+i*3,6,.5,1.2);
                        b.add("metal",side*17-3,13.5,-17+i*3,6,.5,1.2);
                    }
                    b.add("metal",side*11-1,8,10,2,2,15);
                    b.add("dark",side*10-1,12,8,2,2,10);
                }
                // Stepped curved blade rather than a single block.
                for(int row=0;row<5;row++) b.add("paint",-w/2,3+row*2,24-Math.abs(row-2)*.7,w,2,1.8);
                b.add("metal",-w/2,2.5,23.5,w,.7,3);
                for(int x=-2;x<=2;x++) b.add("metal",x*w/5-.4,5,25,.8,6,.5);
                b.add("dark",9,20,7,1.6,13,1.6);
            }
            case PICKUP -> {
                double length=cargo?37:32;
                b.add("dark",-11,5,-length/2,22,3,length);
                b.add("paint",-12,8,-length/2,24,5,length);
                b.axle("front",23,11,5); b.axle("rear",23,-11,5);
                b.add("paint",-11,13,7,22,4,9); b.cabin(21,13,11,-5,13);
                b.add("seat",-9,13,-length/2+1,18,1,length/2-6);
                b.add("paint",-12,13,-length/2,2,5,length/2-5); b.add("paint",10,13,-length/2,2,5,length/2-5);
                b.add("paint",-12,13,-length/2,24,5,1.5);
                b.add("metal",-12,7,16,24,2,1.5); b.add("metal",-12,7,-length/2-1,24,2,1.5);
                for(int i=0;i<6;i++) b.add("dark",-5+i*2,11,16.1,1,3,.35);
                for(int side:new int[]{-1,1}) {
                    b.add("accent",side*9-2,12,16.1,4,2,.4);
                    b.add("metal",side*11.9,14,0,.3,.6,2);
                    for(int axle:new int[]{-1,1}) b.add("paint",side*10-2,10,axle*11-6,4,1,12);
                }
                if(cargo) { b.add("metal",-10,18,-15,1,7,1); b.add("metal",9,18,-15,1,7,1); b.add("metal",-10,25,-15,20,1,1); }
            }
            case MOTORCYCLE -> {
                b.wheel("front",0,5.5,12,2.8,5.5); b.wheel("rear",0,5.5,-12,3.5,5.5);
                b.add("metal",-1.5,6,-12,3,2,24); b.add("dark",-3.8,7,-3,7.6,6,8);
                for(int i=0;i<4;i++) b.add("metal",-4,8+i,0,8,.35,4);
                b.add("paint",-4,13,0,8,4,7);
                b.add("seat",-3.5,13,-10,7,2,10); b.add("seat",-3,15,-10,6,1,4);
                for(int side:new int[]{-1,1}) {
                    b.add("metal",side*2-.4,5,10,.8,12,.8);
                    b.add("metal",side*4-.6,7,-12,1.2,1.2,10);
                    b.add("dark",side*6-1,18,8,2,.9,.9);
                }
                b.add("metal",-6,18,8,12,.8,.8); b.add("accent",-2,16,10,4,2,1);
                b.add("paint",-2,11,9,4,1,7); b.add("paint",-2.5,11,-15,5,1,7);
                if(cargo) for(int side:new int[]{-1,1}) { b.add("seat",side*5-2,8,-13,4,6,7); b.add("metal",side*5-2,14,-13,4,.5,7); }
            }
            case BOAT -> {
                double w=type.width*16, length=cargo?40:34;
                for(int layer=0;layer<5;layer++) b.add("paint",-w/2+5-layer,layer*1.3,-length/2+3-layer,w-10+layer*2,1.3,length-6+layer*2);
                b.add("dark",-w/2+2,6,-length/2+2,w-4,.6,length-4);
                for(int side:new int[]{-1,1}) {
                    b.add("paint",side*(w/2-1)-1,6,-length/2,2,5,length-4);
                    b.add("metal",side*(w/2-2)-.3,11,-length/2,.6,.6,length-4);
                    for(int i=0;i<3;i++) b.add("metal",side*(w/2-3)-.3,8,i*7-8,.6,4,.6);
                }
                b.add("paint",-w/2+2,6,length/2-6,w-4,3,6);
                b.add("paint",-w/2+3,8,2,w-6,3,3); b.add("glass",-w/2+3,11,3,w-6,4,.5);
                for(int side:new int[]{-1,1}) { b.add("seat",side*5-2,7,-4,4,2,5); b.add("seat",side*5-2,9,-4,4,4,1); }
                b.add("dark",-3,5,-length/2-3,6,8,4); b.add("metal",-1,0,-length/2-2,2,6,2);
                String prop=b.group("propeller","metal",0,1,-length/2-3,'z');
                b.addTo(prop,-4,-.4,-.4,8,.8,.8); b.addTo(prop,-.4,-4,-.4,.8,8,.8);
            }
            case PLANE -> {
                double span=type.width*16, length=cargo?54:44;
                b.add("paint",-5,8,-length/2,10,8,length);
                b.add("paint",-4,6,-length/2+2,8,2,length-4); b.add("paint",-4,16,-8,8,2,14);
                b.add("glass",-4,17,-4,8,4,9);
                for(int side:new int[]{-1,1}) {
                    b.add("paint",side*(span/4+3)-span/4,10,-5,span/2,1.5,11);
                    b.add("metal",side*(span/2-2)-.3,11,-4,.6,.4,9);
                    b.add("accent",side*(span/2-1)-1,10,1,2,1.6,3);
                    b.add("metal",side*7-.5,3,-1,1,7,1); b.wheel("gear"+side,side*7,3,-1,2,3);
                }
                b.add("paint",-span*.24,12,-length/2,span*.48,1,7);
                b.add("paint",-.7,13,-length/2,1.4,9,7);
                b.add("dark",-3,9,length/2,6,6,2);
                String prop=b.group("propeller","dark",0,12,length/2+2,'z');
                b.addTo(prop,-.6,-9,-.6,1.2,18,1.2); b.addTo(prop,-9,-.6,-.6,18,1.2,1.2);
                b.add("metal",-1,11,length/2+2,2,2,2);
            }
            case HELICOPTER -> {
                b.add("paint",-9,9,-11,18,12,24); b.add("paint",-7,7,-10,14,2,22);
                b.add("glass",-7,13,12,14,7,.5);
                for(int side:new int[]{-1,1}) {
                    b.add("glass",side*8.9,13,1,.4,7,9); b.add("paint",side*8.8,10,-8,.5,11,8);
                    b.add("metal",side*9-.4,12,-2,.8,.6,2);
                    b.add("metal",side*10-.6,3,-15,1.2,1.2,29);
                    for(int z:new int[]{-8,8}) b.add("metal",side*8-.5,4,z,1,5,1);
                }
                b.add("paint",-2,13,-35,4,4,24); b.add("paint",-.8,15,-35,1.6,9,5);
                b.add("paint",-7,15,-29,14,1,4);
                b.add("dark",-3,21,-3,6,3,7); b.add("metal",-.7,24,0,1.4,8,1.4);
                String rotor=b.group("rotor","dark",0,32,0,'y');
                double r=type.width*10;
                b.addTo(rotor,-r,-.3,-1.2,r*2,.6,2.4); b.addTo(rotor,-1.2,-.3,-r,2.4,.6,r*2);
                String tail=b.group("tail_rotor","metal",3,19,-32,'x');
                b.addTo(tail,-.3,-4,-.5,.6,8,1); b.addTo(tail,-.3,-.5,-4,.6,1,8);
                if(cargo) b.add("metal",-3,5,-2,6,2,4);
            }
            case DRONE -> {
                double r=type.width*6;
                b.add("paint",-4,5,-5,8,3,10); b.add("dark",-3,4,-4,6,1,8);
                b.add("glass",-1.7,3,5,3.4,2,1.5);
                for(int a:new int[]{-1,1}) for(int c:new int[]{-1,1}) {
                    b.add("metal",Math.min(0,a*r),6,c*r-.5,r,1,1);
                    b.add("metal",a*r-.5,6,Math.min(0,c*r),1,1,r);
                    b.add("dark",a*r-1.5,6,c*r-1.5,3,2,3);
                    String rotor=b.group("rotor_"+a+"_"+c,"dark",a*r,9,c*r,'y');
                    b.addTo(rotor,-5,-.25,-.6,10,.5,1.2);
                    b.add("metal",a*r-.4,1,c*r-.4,.8,5,.8);
                }
                if(cargo) b.add("seat",-4,1,-4,8,3,8);
            }
        }
        return b.finish();
    }
}
