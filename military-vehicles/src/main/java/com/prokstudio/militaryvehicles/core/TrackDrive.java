package com.prokstudio.militaryvehicles.core;

/** Differential tracked arcade drive; no camera input and no real-world ballistics/vehicle data. */
public final class TrackDrive {
    public static final double TRACK_SPAN=46.0/16;
    public record State(double left,double right,float yaw) {
        public double speed() { return (left+right)*.5; }
    }
    private TrackDrive() {}
    public static State step(VehicleKind kind,State old,double actualSpeed,int keys,boolean powered,boolean grounded,double traction) {
        if(!kind.tracked()) throw new IllegalArgumentException("Tracked profile required");
        if((keys&~ControlLatch.MASK)!=0||!Double.isFinite(old.left()+old.right()+actualSpeed+traction)||!Float.isFinite(old.yaw())) return new State(0,0,0);
        var h=kind.handling;traction=TruckPhysics.clamp(traction,.25,1);
        int drive=((keys&ControlLatch.FORWARD)!=0?1:0)-((keys&ControlLatch.BACK)!=0?1:0);
        int turn=((keys&ControlLatch.RIGHT)!=0?1:0)-((keys&ControlLatch.LEFT)!=0?1:0);
        // Remove translation that the world collision rejected, retaining differential turn state.
        double correction=TruckPhysics.clamp(actualSpeed,-h.reverse(),h.forward())-old.speed();
        double l=TruckPhysics.clamp(old.left()+correction,-h.reverse(),h.forward());
        double r=TruckPhysics.clamp(old.right()+correction,-h.reverse(),h.forward());
        if((keys&ControlLatch.BRAKE)!=0) {
            l=TruckPhysics.approach(l,0,h.brake());r=TruckPhysics.approach(r,0,h.brake());
        } else if(powered&&grounded) {
            double center=drive*(drive<0?h.reverse():h.forward())*(turn==0?1:.65)*traction;
            double differential=turn*Math.min(.075,h.reverse())*traction;
            l=approach(l,TruckPhysics.clamp(center+differential,-h.reverse(),h.forward()),h,traction);
            r=approach(r,TruckPhysics.clamp(center-differential,-h.reverse(),h.forward()),h,traction);
        } else {
            l=TruckPhysics.approach(l,0,grounded?h.coast():.0006);r=TruckPhysics.approach(r,0,grounded?h.coast():.0006);
        }
        float yaw=old.yaw();
        if(grounded) yaw+=(float)TruckPhysics.clamp(Math.toDegrees((l-r)/TRACK_SPAN),-h.yawRate(),h.yawRate());
        return new State(l,r,TruckPhysics.wrap(yaw));
    }
    private static double approach(double speed,double target,VehicleKind.Handling h,double traction) {
        return speed*target<0?TruckPhysics.approach(speed,0,h.counterBrake()):TruckPhysics.approach(speed,target,h.acceleration()*traction);
    }
    /** Surface pads circulate independently; lower run cancels road travel, upper run returns it. */
    public static double treadOffset(double initialZ,double wheelAngle,boolean upper) {
        if(!Double.isFinite(initialZ+wheelAngle)) return 0;
        double z=initialZ+(upper?1:-1)*wheelAngle*6.5;
        return ((z+27)%54+54)%54-27-initialZ;
    }
}
