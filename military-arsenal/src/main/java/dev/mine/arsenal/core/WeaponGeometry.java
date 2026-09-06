package dev.mine.arsenal.core;

import java.util.ArrayList;
import java.util.List;

/** Original editable voxel meshes. Coordinates are Minecraft model units, not engineering dimensions. */
public final class WeaponGeometry {
    public record Part(String name,String material,double x,double y,double z,double w,double h,double d) {}
    private final List<Part> parts=new ArrayList<>();
    private void box(String name,String mat,double x,double y,double z,double w,double h,double d) {
        parts.add(new Part(name,mat,x,y,z,w,h,d));
    }
    private void tube(String name,String mat,double y,double z,double radius,double length) {
        box(name,mat,8-radius*.72,y-radius,z,radius*1.44,radius*2,length);
        box(name,mat,8-radius,y-radius*.72,z,radius*2,radius*1.44,length);
    }
    private void grip(double z,String material) {
        box("grip",material,6.7,1.5,z,2.6,6,3);
        for(int i=0;i<4;i++) box("grip_rib","rubber",6.5,2+i, z-.1,3,.3,3.2);
        box("guard","steel",6.7,4.1,z-4, .45,3.7,4.1);
        box("guard","steel",8.85,4.1,z-4,.45,3.7,4.1);
        box("guard","steel",6.7,3.8,z-4,2.6,.45,4.1);
        box("trigger","dark",7.8,5.3,z-2.4,.4,1.8,.6);
    }
    private void sights(double front,double rear,boolean scope) {
        if(scope) {
            box("scope_mount","dark",6.7,11.8,rear-6,2.6,1.5,7);
            tube("scope","steel",14.3,rear-9,1.35,10);
            tube("lens","glass",14.3,rear-9.08,1.1,.2);
            tube("lens","glass",14.3,rear+.88,1.1,.2);
            box("dial","dark",8.8,14,rear-4,1.7,1.4,2);
        } else {
            box("front_sight","dark",7.7,11.7,front,.6,2,1);
            box("rear_sight","steel",6.5,12,rear,1,1.3,1);
            box("rear_sight","steel",8.5,12,rear,1,1.3,1);
        }
    }
    public static List<Part> create(Weapon w) {
        WeaponGeometry g=new WeaponGeometry();
        if(w.style==Weapon.Style.RPG) {
            g.tube("tube","paint",10,-12,2.45,37);
            g.tube("front_ring","steel",10,-13,2.8,1.6);
            g.tube("muzzle","dark",10,-13.08,1.9,.15);
            g.tube("back_ring","steel",10,24,3,2);
            g.tube("back_opening","dark",10,25.91,2.15,.2);
            g.grip(9,"rubber");
            g.box("shoulder_pad","rubber",5,5,16,6,3,8);
            g.box("sight_mount","steel",9.6,10,1,2,3,8);
            g.sights(-8,8,true);
            for(int i=0;i<5;i++) g.tube("tube_band","rubber",10,14+i*1.5,2.55,.4);
            if(w==Weapon.ATLAS) {
                g.tube("rocket","olive",10,-15,1.6,4);
                g.tube("warhead","brass",10,-16,1.1,1.3);
            } else {
                g.box("fire_control","paint",10,5,3,2.7,4,10);
                g.box("screen","glass",12.72,7,6,.15,1.5,3);
                g.box("top_handle","steel",6.6,15,13,2.8,.6,7);
                g.box("handle_post","steel",6.7,12.5,13,.6,3,.6);
                g.box("handle_post","steel",8.7,12.5,19,.6,3,.6);
            }
        } else if(w.sidearm()) {
            double barrel=w==Weapon.MARSHAL?10:w==Weapon.BASTION?7.5:6;
            g.box("frame","paint",6.3,7,5,3.4,3.5,10);
            g.grip(11,w==Weapon.MARSHAL?"wood":"rubber");
            g.tube("barrel","steel",10.5,5-barrel,.8,barrel+6);
            g.tube("muzzle","dark",10.5,4.9-barrel,.5,.15);
            if(w==Weapon.MARSHAL) {
                g.tube("cylinder","steel",9.8,7,2.1,4.8);
                for(int i=0;i<3;i++) g.box("cylinder_groove","dark",5.85,8.6+i,7,.2,.4,4);
                g.box("hammer","steel",7.5,11.3,13.8,1,1.3,1.3);
            } else {
                g.box("bolt","steel",6.2,9.6,4.5-barrel,3.6,2.4,barrel+10.5);
                g.box("ejection_port","dark",9.82,10.5,6.8,.12,1,3);
                for(int i=0;i<4;i++) g.box("slide_serration","dark",6.08,10.3,11.2+i*.7,.15,1.3,.3);
            }
            g.box("mag","paint",6.7,1.3,11,2.6,.6,3.3);
            g.sights(5-barrel,13.7,false);
        } else {
            double front=w.style==Weapon.Style.SNIPER?-14:w.style==Weapon.Style.DMR?-11:w.style==Weapon.Style.SMG?0:-8;
            double rear=w.style==Weapon.Style.SMG?18:25;
            g.box("receiver","paint",6.15,7.8,4,3.7,4.3,12);
            g.tube("barrel","steel",10.3,front, .8,7-front);
            g.tube("muzzle","dark",10.3,front-.18,.5,.2);
            g.box("handguard","paint",5.9,8,front+3,4.2,3.9,Math.max(2,3-front));
            for(int i=0;i<5;i++) {
                double z=front+3.5+i*Math.max(.7,(2-front)/6);
                g.box("vent","dark",5.78,9.6,z,.18,.8,.55);
                g.box("vent","dark",10.04,9.6,z,.18,.8,.55);
            }
            g.box("bolt","steel",9.85,9.6,8,.45,1.5,4);
            g.box("charging_handle","dark",10.2,9.6,10,1.5,.6,.9);
            g.box("stock_tube","steel",7.1,9.5,16,1.8,1.8,rear-16);
            g.box("stock","paint",6.2,6.9,rear-5,3.6,5.2,5.5);
            g.box("butt_pad","rubber",5.95,6.4,rear+.35,4.1,6,.65);
            g.grip(12,w==Weapon.BOREAL||w==Weapon.BREAKER?"wood":"rubber");
            if(w.style==Weapon.Style.PUMP) {
                g.tube("mag_tube","steel",7.7,front+1,.8,12-front);
                g.box("pump","wood",5.7,6.8,front+4,4.6,2.2,6);
                for(int i=0;i<6;i++) g.box("pump","dark",5.6,6.7,front+4.5+i*.85,4.8,.35,.3);
            } else if(w.style==Weapon.Style.LAUNCHER) {
                g.tube("drum","steel",7,2,3.7,6);
                g.tube("barrel_sleeve","paint",10.3,-5,2.3,8);
                g.tube("muzzle","dark",10.3,-5.1,1.6,.2);
            } else if(w.style==Weapon.Style.LMG || w.style==Weapon.Style.SHOTGUN) {
                g.box("mag","olive",4.5,1.8,4,7,6.2,6);
                for(int i=0;i<3;i++) g.box("mag","steel",5.2+i*1.8,2,3.8,.45,5.5,.3);
            } else {
                double length=w.style==Weapon.Style.SMG?6.5:5.5;
                g.box("mag","steel",6.6,7.5-length,4,2.8,length,4);
                g.box("mag","dark",6.4,7-length,3.8,3.2,.6,4.4);
                for(int i=0;i<3;i++) g.box("mag","dark",6.43,2.8+i*1.3,4.5,.2,.4,2.8);
            }
            g.box("rail","dark",6.5,12.1,2,3,.5,13);
            for(int i=0;i<8;i++) g.box("rail_rib","steel",6.45,12.5,2+i*1.5,3.1,.3,.45);
            g.sights(front+4,13,w.scoped());
            if(w==Weapon.WARDEN) g.tube("suppressor","dark",10.3,-7,1.25,8);
            if(w==Weapon.LONGWATCH || w==Weapon.BULWARK) {
                g.box("bipod","steel",4.5,1,front+6,.7,8,1);
                g.box("bipod","steel",10.8,1,front+6,.7,8,1);
                g.box("bipod_foot","rubber",3.8,.7,front+5.5,2,.6,2);
                g.box("bipod_foot","rubber",10.3,.7,front+5.5,2,.6,2);
            }
            if(w==Weapon.TRIDENT) {
                g.box("carry_handle","paint",6.5,14,5,3,.8,8);
                g.box("handle_post","steel",6.7,12.7,5,2.6,1.5,.7);
                g.box("handle_post","steel",6.7,12.7,12.3,2.6,1.5,.7);
            }
            if(w==Weapon.BASTION) g.box("accent","brass",6,9,8,.1,1,3);
        }
        g.box("selector","steel",5.98,8.5,12,.2,.7,1.5);
        g.box("pin","brass",9.97,8.7,14,.16,.55,.55);
        return List.copyOf(g.parts);
    }
    public static List<Part> ammunition(Ammo a) {
        WeaponGeometry g=new WeaponGeometry();
        if(a.projectile()&&!a.grenade()) {
            g.tube("body",a.piercing?"steel":a.smoke?"blue":a==Ammo.ROCKET_PRACTICE?"orange":"olive",8,0,1.3,16);
            g.tube("warhead","steel",8,-3,1.7,4);
            g.tube("tip","brass",8,-4,.8,1.5);
            g.box("fin","dark",5.2,7.7,12,5.6,.6,4);
            g.box("fin","dark",7.7,5.2,12,.6,5.6,4);
            g.tube("band","brass",8,1,1.5,.7);
        } else if(a.grenade()) {
            g.tube("shell","brass",8,3,2,8);
            g.tube("cap",a.smoke?"blue":"olive",8,1,2.05,3);
            g.tube("base","steel",8,10.5,2.15,.8);
        } else {
            String material=a==Ammo.BUCKSHOT?"red":a==Ammo.SLUG?"blue":"brass";
            double length=a==Ammo.PRECISION?9:a==Ammo.RIFLE||a==Ammo.INTERMEDIATE?7:5;
            g.tube("case",material,8,5,1,length);
            g.tube("rim","brass",8,4.7,1.2,.45);
            g.tube("bullet",a==Ammo.SLUG||a==Ammo.BUCKSHOT?"steel":"copper",8,5+length,.8,2);
            g.tube("tip","copper",8,6.7+length,.4,.6);
        }
        return List.copyOf(g.parts);
    }
}
