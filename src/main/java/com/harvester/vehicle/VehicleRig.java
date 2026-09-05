package com.harvester.vehicle;

/** Shared by server seats/effects and client matrices. Angles are radians, geometry uses pixels. */
public final class VehicleRig {
    public static final double PLAYER_HIP=0.7040625; // (1.501 - 12/16) * vanilla player model scale .9375
    public record Pose(float pitch,float roll) {
        public static final Pose ZERO=new Pose(0,0);
    }
    public record Point(double x,double y,double z) {}
    private VehicleRig() {}
    public static int pack(Pose pose) {
        int pitch=(int)Math.round(VehiclePhysics.clamp(Math.toDegrees(pose.pitch()),-48,48)*2);
        int roll=(int)Math.round(VehiclePhysics.clamp(Math.toDegrees(pose.roll()),-24,24)*4);
        return (pitch&255)|((roll&255)<<8);
    }
    public static Pose unpack(int bits) {
        return new Pose((float)Math.toRadians((byte)bits*.5),(float)Math.toRadians((byte)(bits>>8)*.25));
    }
    public static Pose lerp(Pose a,Pose b,float delta) {
        float t=(float)VehiclePhysics.clamp(delta,0,1);
        return new Pose(a.pitch()+(b.pitch()-a.pitch())*t,a.roll()+(b.roll()-a.roll())*t);
    }
    public static Pose approach(Pose old,double pitchDegrees,double rollDegrees) {
        double p=Math.toRadians(VehiclePhysics.clamp(pitchDegrees,-45,45));
        double r=Math.toRadians(VehiclePhysics.clamp(rollDegrees,-20,20));
        return new Pose((float)(old.pitch()+(p-old.pitch())*.22),(float)(old.roll()+(r-old.roll())*.22));
    }
    /** Matrix order: translate pivot, rotate Z, rotate X, translate -pivot. */
    public static Point transform(VehicleType type,Pose pose,double x,double y,double z) {
        var pivot=VehicleGeometry.seat(type,0);
        double px=pivot.x()/16,py=pivot.top()/16,pz=pivot.z()/16;
        x-=px; y-=py; z-=pz;
        double cp=Math.cos(pose.pitch()),sp=Math.sin(pose.pitch()),cr=Math.cos(pose.roll()),sr=Math.sin(pose.roll());
        double yy=y*cp-z*sp,zz=y*sp+z*cp;
        return new Point(px+x*cr-yy*sr,py+x*sr+yy*cr,pz+zz);
    }
}
