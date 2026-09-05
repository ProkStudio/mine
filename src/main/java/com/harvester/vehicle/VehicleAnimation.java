package com.harvester.vehicle;

import java.util.List;

/** Per-vehicle histories with value-only frames. Compatibility overload retains the old pure-test API. */
public final class VehicleAnimation {
    public record Frame(double wheelTravel,float engineRotor,float headerLift,float steering) {}
    public record Mechanisms(float workAngle,float workingStrength,double yawTravel) {}
    private boolean initialized,mechanismsInitialized;
    private double time,x,z,yaw,travel,displayedTravel,rotor,rotorSpeed,header,steering,direction=1;
    private double mechanismTime,mechanismYaw,yawTravel,workAngle,workSpeed;
    public static double smooth(double current,double target,double response,double ticks) {
        double dt=VehiclePhysics.clamp(ticks,0,5);
        double result=VehiclePhysics.finite(current)+(VehiclePhysics.finite(target)-VehiclePhysics.finite(current))*-Math.expm1(-Math.max(0,VehiclePhysics.finite(response))*dt);
        return Math.abs(result-target)<1e-7?target:result;
    }
    public static double signedDistance(double dx,double dz,double yawDegrees) {
        double angle=Math.toRadians(VehiclePhysics.finite(yawDegrees));
        return -VehiclePhysics.finite(dx)*Math.sin(angle)+VehiclePhysics.finite(dz)*Math.cos(angle);
    }
    public static float wheelPhase(double travel,double radiusBlocks) {
        return (float)Math.IEEEremainder(VehiclePhysics.finite(travel)/Math.max(.01,VehiclePhysics.finite(radiusBlocks)),Math.PI*2);
    }
    public static double wheelRadius(VehicleGeometry.Part part,List<VehicleGeometry.Part> all) {
        String tire=part.name().replace("wheel_hub_","wheel_");
        VehicleGeometry.Part definition=all.stream().filter(p->p.name().equals(tire)).findFirst().orElse(part);
        double radius=0;
        for(var box:definition.boxes()) radius=Math.max(radius,Math.max(Math.abs(box.y()),Math.abs(box.y()+box.h())));
        return Math.max(.01,radius/16);
    }
    public static int rotorDirection(String name) { return name.equals("rotor_-1_-1") || name.equals("rotor_1_1")?-1:1; }
    public Frame update(double now,double newX,double newZ,float newYaw,boolean engineActive,boolean headerEnabled,VehicleType.Family family) {
        return updateCore(now,newX,newZ,newYaw,engineActive,headerEnabled,family,Double.NaN,Double.NaN);
    }
    public Frame update(double now,double newX,double newZ,float newYaw,boolean engineActive,boolean headerEnabled,VehicleType.Family family,float steeringCommand,float driveCommand) {
        return updateCore(now,newX,newZ,newYaw,engineActive,headerEnabled,family,steeringCommand,driveCommand);
    }
    private Frame updateCore(double now,double newX,double newZ,float newYaw,boolean engineActive,boolean headerEnabled,VehicleType.Family family,double command,double throttle) {
        now=VehiclePhysics.finite(now); newX=VehiclePhysics.finite(newX); newZ=VehiclePhysics.finite(newZ);
        double nextYaw=VehiclePhysics.wrap(newYaw);
        if(!initialized) { initialized=true; time=now; x=newX; z=newZ; yaw=nextYaw; header=headerEnabled?0:4; return frame(); }
        double dt=now-time;
        if(dt<=0 || dt>5 || Math.hypot(newX-x,newZ-z)>8) {
            if(dt!=0 || newX!=x || newZ!=z) { time=now; x=newX; z=newZ; yaw=nextYaw; }
            return frame();
        }
        double moved=signedDistance(newX-x,newZ-z,nextYaw); travel+=moved;
        displayedTravel=smooth(displayedTravel,travel,.9,dt);
        double turn=VehiclePhysics.wrap((float)(nextYaw-yaw))/dt;
        if(Math.abs(moved)>1e-5) direction=Math.signum(moved);
        double targetSteering=Double.isFinite(command)?VehiclePhysics.clamp(command,-1,1)*22:VehiclePhysics.clamp(turn*6*direction,-22,22);
        steering=smooth(steering,targetSteering,.4,dt);
        double targetSpeed=engineActive?.65:0;
        if(family==VehicleType.Family.BOAT) targetSpeed*=Double.isFinite(throttle)?VehiclePhysics.clamp(throttle,-1,1):direction;
        double previousSpeed=rotorSpeed,response=engineActive?.22:.14;
        rotorSpeed=smooth(rotorSpeed,targetSpeed,response,dt);
        rotor+=targetSpeed*dt+(previousSpeed-targetSpeed)*-Math.expm1(-response*dt)/response;
        rotor=Math.IEEEremainder(rotor,Math.PI*2); header=smooth(header,headerEnabled?0:4,.22,dt);
        time=now; x=newX; z=newZ; yaw=nextYaw; return frame();
    }
    public Mechanisms mechanisms(double now,float newYaw,boolean working) {
        now=VehiclePhysics.finite(now);
        if(!mechanismsInitialized) { mechanismsInitialized=true; mechanismTime=now; mechanismYaw=newYaw; return mechanismFrame(); }
        double dt=now-mechanismTime;
        if(dt<=0 || dt>5) { if(dt!=0) { mechanismTime=now; mechanismYaw=newYaw; } return mechanismFrame(); }
        double change=VehiclePhysics.wrap((float)(newYaw-mechanismYaw));
        if(Math.abs(change)<=90) yawTravel+=Math.toRadians(change);
        double target=working?.8:0,response=working?.35:.20,old=workSpeed;
        workSpeed=smooth(workSpeed,target,response,dt);
        workAngle=Math.IEEEremainder(workAngle+target*dt+(old-target)*-Math.expm1(-response*dt)/response,Math.PI*2);
        mechanismTime=now; mechanismYaw=newYaw; return mechanismFrame();
    }
    private Mechanisms mechanismFrame() { return new Mechanisms((float)workAngle,(float)VehiclePhysics.clamp(workSpeed/.8,0,1),yawTravel); }
    private Frame frame() { return new Frame(displayedTravel,(float)rotor,(float)header,(float)Math.toRadians(steering)); }
}
