package com.harvester.vehicle;

/** Value-only secondary mechanisms; all distances here are blueprint pixels unless stated. */
public final class VehicleMechanics {
    public record Frame(float wiper,float accelerator,float brake,boolean brakeLamp,boolean reverseLamp) {
        public static final Frame REST=new Frame(1.15f,0,0,false,false);
    }
    public record Hydraulic(double deltaPitch,double length,double pistonOffset,double pistonScale) {}
    private double time,cycle;
    private boolean initialized;
    private VehicleType type;
    private static double finite(double n) { return Double.isFinite(n)?n:0; }
    private static double clamp(double n,double lo,double hi) { return Math.max(lo,Math.min(hi,finite(n))); }
    public Frame update(double now,VehicleType type,boolean rain,boolean occupied,double rpm,double drive,double signedSpeed) {
        now=finite(now); double dt=now-time;
        if(!initialized || this.type!=type || dt<0 || dt>5) { initialized=true;this.type=type;cycle=0;dt=0; }
        boolean wipe=rain && occupied && rpm>.03;
        if(dt>0 && (wipe || cycle>0)) {
            double next=cycle+.22*dt;
            cycle=!wipe && next>=Math.PI*2?0:next%(Math.PI*2);
        }
        time=now;
        double command=clamp(drive,-1,1),speed=finite(signedSpeed);
        return new Frame((float)(.25+.9*(.5+.5*Math.cos(cycle))),
            occupied?(float)Math.max(0,command):0,occupied?(float)Math.max(0,-command):0,
            occupied && !type.aircraft() && speed*command<-.002,
            occupied && !type.aircraft() && speed<-.008 && command<=0);
    }
    /** Fixed barrel plus telescoping rod. Both endpoints stay attached for the entire stroke. */
    public static Hydraulic hydraulic(boolean header,double headerLift,double workAngle,double strength) {
        double lift=header?clamp(headerLift,0,4):Math.sin(finite(workAngle)*8)*.007*16*clamp(strength,0,1);
        double dy=-8+lift,dz=8,rest=Math.sqrt(128),length=Math.hypot(dy,dz),offset=rest*.5;
        return new Hydraulic(Math.atan2(-dy,dz)-Math.PI/4,length,offset,(length-offset)/(rest-offset));
    }
    public static float springScale(double worldOffset) {
        return (float)clamp(1-clamp(worldOffset,-.12,.12)*16/(VehicleType.MODEL_SCALE*4),.2,1.8);
    }
    /** A sphere around the entity origin encloses any body rotation about the driver hip. */
    public static double renderRadius(VehicleType type,java.util.List<VehicleGeometry.Part> parts) {
        var s=VehicleGeometry.seat(type,0);double radius=0;
        for(var p:parts) for(var b:p.boxes()) for(int corner=0;corner<8;corner++) {
            double x=b.x()+((corner&1)==0?0:b.w()),y=b.y()+((corner&2)==0?0:b.h()),z=b.z()+((corner&4)==0?0:b.d());
            double rx=Math.toRadians(p.restPitch()),ry=Math.toRadians(p.restYaw()),rz=Math.toRadians(p.restRoll());
            double yy=y*Math.cos(rx)-z*Math.sin(rx),zz=y*Math.sin(rx)+z*Math.cos(rx);
            double xx=x*Math.cos(ry)+zz*Math.sin(ry);zz=-x*Math.sin(ry)+zz*Math.cos(ry);
            x=p.px()+xx*Math.cos(rz)-yy*Math.sin(rz);y=p.py()+xx*Math.sin(rz)+yy*Math.cos(rz);z=p.pz()+zz;
            radius=Math.max(radius,Math.sqrt(Math.pow(x-s.x(),2)+Math.pow(y-s.top(),2)+Math.pow(z-s.z(),2)));
        }
        return (radius+6+Math.sqrt(s.x()*s.x()+s.top()*s.top()+s.z()*s.z()))/16*VehicleType.MODEL_SCALE;
    }
}
