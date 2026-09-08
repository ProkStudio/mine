package com.prokstudio.militaryvehicles.core;
/** One deterministic arcade tick; Minecraft handles world collision. Legacy overload preserves truck callers. */
public final class TruckPhysics {
    public record Motion(double speed,float yaw,float steer) {}
    private TruckPhysics() {}
    public static Motion step(double actualSpeed,float yaw,int keys,boolean powered,boolean onGround,double traction) {
        return step(VehicleKind.TRUCK,actualSpeed,yaw,keys,powered,onGround,traction);
    }
    public static Motion step(VehicleKind kind,double actualSpeed,float yaw,int keys,boolean powered,boolean onGround,double traction) {
        if (!Double.isFinite(actualSpeed)||!Float.isFinite(yaw)||!Double.isFinite(traction)) return new Motion(0,0,0);
        var h=kind.handling;
        traction=clamp(traction,.25,1);
        double speed=clamp(actualSpeed,-h.reverse(),h.forward());
        int drive=((keys&ControlLatch.FORWARD)!=0?1:0)-((keys&ControlLatch.BACK)!=0?1:0);
        int turn=((keys&ControlLatch.RIGHT)!=0?1:0)-((keys&ControlLatch.LEFT)!=0?1:0);
        if ((keys&ControlLatch.BRAKE)!=0) speed=approach(speed,0,h.brake());
        else if (powered&&onGround&&drive!=0) {
            double target=drive*(drive>0?h.forward():h.reverse())*traction;
            speed=speed*drive<0?approach(speed,0,h.counterBrake()):approach(speed,target,h.acceleration()*traction);
        } else speed=approach(speed,0,onGround?h.coast():.0006);
        float steer=powered&&onGround?turn*h.steer():0;
        if (Math.abs(speed)>.003&&steer!=0) yaw+=turn*h.yawRate()*(float)clamp(Math.abs(speed)/h.forward(),0,1)*Math.signum(speed);
        return new Motion(speed,wrap(yaw),steer);
    }
    public static double signedSpeed(double dx,double dz,float yaw) { double a=Math.toRadians(yaw);return -dx*Math.sin(a)+dz*Math.cos(a); }
    public static double clamp(double v,double lo,double hi) { return Math.max(lo,Math.min(hi,v)); }
    public static double approach(double from,double to,double amount) { return from<to?Math.min(from+amount,to):Math.max(from-amount,to); }
    public static float wrap(float angle) { return (float)(((angle+180)%360+360)%360-180); }
    public static int transferFuel(int tank,int can) { return transferFuel(VehicleKind.TRUCK,tank,can); }
    public static int transferFuel(VehicleKind kind,int tank,int can) {
        if(tank<0||tank>kind.tank||can<0) return 0;
        return Math.min(kind.tank-tank,can);
    }
}
