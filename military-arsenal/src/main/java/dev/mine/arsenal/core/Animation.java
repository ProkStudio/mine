package dev.mine.arsenal.core;

/** Shared deterministic curves: no wall-clock or renderer dependencies. */
public final class Animation {
    private Animation() {}
    public static double clamp(double x,double a,double b) { return Math.max(a,Math.min(b,x)); }
    public static double smooth(double x) { x=clamp(x,0,1); return x*x*(3-2*x); }
    public static int shotFrame(long age) { return age>=0 && age<4 ? 1+(int)age : 0; }
    public static int reloadFrame(long age,int duration) { return 10+(int)clamp(8.0*age/Math.max(1,duration),0,7); }
    public static int inspectFrame(long age) { return 20+(int)clamp(age/5.0,0,7); }
    public static double magazineTravel(int frame) {
        if(frame<10 || frame>17) return 0;
        return Math.sin(Math.PI*(frame-10)/7.0);
    }
    public static double boltTravel(int frame) { return frame>=1 && frame<=4 ? Math.sin(Math.PI*frame/5.0) : 0; }
    public static double recoil(double age) { return age<0 || age>9 ? 0 : Math.sin(Math.PI*clamp(age/1.5,0,1)/2)*Math.exp(-age*.4); }
    public static double falloff(double distance,double range) { return clamp(1-.55*Math.max(0,distance)/Math.max(1,range),.45,1); }
    public static double blast(double distance,double radius) { return radius<=0?0:Math.pow(clamp(1-distance/radius,0,1),1.3); }
}
