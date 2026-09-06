package dev.mine.arsenal.core;

import java.util.*;

/** Original game-only hand grenade mesh. Nose/pull cap points along local -Z. */
public final class GrenadeGeometry {
    private GrenadeGeometry() {}
    public static List<WeaponGeometry.Part> ammunition(Ammo a) {
        if(!a.handGrenade()) return WeaponGeometry.ammunition(a);
        List<WeaponGeometry.Part> parts=new ArrayList<>();
        box(parts,"body",a.smoke?"blue":"olive",5.8,5.8,4.5,4.4,4.4,6);
        box(parts,"shoulder","dark",6.4,6.4,3.5,3.2,3.2,1);
        box(parts,"cap","steel",7,7,2.5,2,2,1);
        box(parts,"lever","steel",9.8,7.5,3.5,.65,1,6.5);
        box(parts,"band",a.smoke?"blue":"orange",5.7,5.7,7,4.6,4.6,.6);
        // Square pull ring; separate bars leave a real opening.
        box(parts,"ring","brass",6.5,7.7,1,3,.5,.4);
        box(parts,"ring","brass",6.5,7.7,2.3,3,.5,.4);
        box(parts,"ring","brass",6.5,7.7,1,.4,.5,1.7);
        box(parts,"ring","brass",9.1,7.7,1,.4,.5,1.7);
        return List.copyOf(parts);
    }
    private static void box(List<WeaponGeometry.Part> parts,String name,String material,double x,double y,double z,double w,double h,double d) {
        parts.add(new WeaponGeometry.Part(name,material,x,y,z,w,h,d));
    }
}
