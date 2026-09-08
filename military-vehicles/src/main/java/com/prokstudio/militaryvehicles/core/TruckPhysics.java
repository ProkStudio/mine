package com.prokstudio.militaryvehicles.core;
/** One deterministic arcade tick; Minecraft handles world collision. */
public final class TruckPhysics {
    public record Motion(double speed,float yaw,float steer) {}
    private TruckPhysics() {}
    public static Motion step(double actualSpeed,float yaw,int keys,boolean powered,boolean onGround,double traction) {
        if (!Double.isFinite(actualSpeed)||!Float.isFinite(yaw)||!Double.isFinite(traction)) return new Motion(0,0,0);
        traction=clamp(traction,.25,1);
        double speed=clamp(actualSpeed,-TruckSpec.MAX_REVERSE,TruckSpec.MAX_FORWARD);
        int drive=((keys&ControlLatch.FORWARD)!=0?1:0)-((keys&ControlLatch.BACK)!=0?1:0);
        int turn=((keys&ControlLatch.RIGHT)!=0?1:0)-((keys&ControlLatch.LEFT)!=0?1:0);
        if ((keys&ControlLatch.BRAKE)!=0) speed=approach(speed,0,.035);
        else if (powered&&onGround&&drive!=0) {
            double target=drive*(drive>0?TruckSpec.MAX_FORWARD:TruckSpec.MAX_REVERSE)*traction;
            speed=speed*drive<0?approach(speed,0,.024):approach(speed,target,.010*traction);
        } else speed=approach(speed,0,onGround?.006:.0006);
        float steer=powered&&onGround?turn*.48f:0;
        if (Math.abs(speed)>.003&&steer!=0) yaw+=turn*2.1f*(float)clamp(Math.abs(speed)/TruckSpec.MAX_FORWARD,0,1)*Math.signum(speed);
        return new Motion(speed,wrap(yaw),steer);
    }
    public static double signedSpeed(double dx,double dz,float yaw) { double a=Math.toRadians(yaw);return -dx*Math.sin(a)+dz*Math.cos(a); }
    public static double clamp(double v,double lo,double hi) { return Math.max(lo,Math.min(hi,v)); }
    public static double approach(double from,double to,double amount) { return from<to?Math.min(from+amount,to):Math.max(from-amount,to); }
    public static float wrap(float angle) { return (float)(((angle+180)%360+360)%360-180); }
    public static int transferFuel(int tank,int can) { return Math.min(Math.max(0,TruckSpec.TANK-tank),Math.max(0,can)); }
}
