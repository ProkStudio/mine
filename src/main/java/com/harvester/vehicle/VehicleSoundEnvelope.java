package com.harvester.vehicle;

/** Tick-independent engine envelope, separately testable without a Minecraft sound device. */
public final class VehicleSoundEnvelope {
    public record Frame(float volume,float pitch,float rpm) {}
    private double level,rpm,load;
    private static double clean(double n) { return Double.isFinite(n)?n:0; }
    private static double clamp(double n,double lo,double hi) { return Math.max(lo,Math.min(hi,clean(n))); }
    private static double approach(double old,double target,double rate,double dt) { return old+(target-old)*-Math.expm1(-rate*dt); }
    public Frame update(VehicleType type,double ticks,boolean powered,double drive,double speed,boolean working,double gain) {
        double dt=clamp(ticks,0,5),velocity=clamp(Math.abs(clean(speed))/Math.max(.02,type.speed),0,1);
        double target=powered?clamp(Math.abs(clean(drive))*.55+velocity*.30+(working?.15:0)+(type.verticalAircraft()?.18:0),0,1):0;
        load=approach(load,target,.20,dt);
        rpm=approach(rpm,powered?.27+.73*target:0,powered?.18:.085,dt);
        level=approach(level,powered?(.26+.43*target)*clamp(gain,0,1):0,powered?.22:.18,dt);
        double base=switch(type.family) {case DOZER -> .70;case COMBINE -> .78;case MOTORCYCLE -> .90;case PICKUP -> .84;case BOAT -> .80;case PLANE -> .92;case HELICOPTER -> .91;case DRONE -> .96;};
        double span=type.verticalAircraft()?.20:type.family==VehicleType.Family.MOTORCYCLE?.58:.42;
        return new Frame((float)clamp(level,0,1),(float)clamp(base+span*rpm+.05*load,.5,1.65),(float)clamp(rpm,0,1));
    }
}
