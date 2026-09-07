package com.harvester.vehicle;

/** Value-only layered presentation. Phases describe real telemetry, not an inflated animation list.
 * No packets, world changes or gameplay authority are introduced by the presentation layer.
 */
public final class VehiclePresentation {
    public enum Phase { PARKED, STARTING, IDLING, DRIVING, REVERSING, BRAKING, WORKING, FLYING, HOVERING, STOPPING, EMPTY_TANK, BROKEN }
    public record Input(double time,VehicleType type,boolean running,boolean working,boolean occupied,
                        boolean grounded,double signedSpeed,double throttle,double steer,double controlPitch,
                        double bodyPitch,double bodyRoll,double fuelFraction,double conditionFraction) {}
    public record Frame(Phase phase,float rpm,float throttle,float steering,float elevator,float rudder,
                        float fuelNeedle,float speedNeedle,float rpmNeedle,float parkingStand,float gimbalPitch,
                        float swashPitch,float swashRoll) {
        public static final Frame REST=new Frame(Phase.PARKED,0,0,0,0,0,-2.1f,-2.1f,-2.1f,0,0,0,0);
    }
    private boolean initialized;
    private VehicleType type;
    private double time,rpm,throttle,steer,pitch,bodyPitch,bodyRoll,fuel,speed,parking;
    public Frame update(Input in) {
        double now=finite(in.time()),dt=now-time;
        double targetSpeed=clamp(Math.abs(in.signedSpeed())/in.type().speed,0,1);
        double targetRpm=in.running() && in.fuelFraction()>0 && in.conditionFraction()>0?
            clamp(.28+Math.abs(in.throttle())*.55+targetSpeed*.17+(in.working()?.1:0),0,1):0;
        boolean reset=!initialized || type!=in.type() || dt<0 || dt>5;
        if(reset) {
            initialized=true; type=in.type(); rpm=targetRpm; throttle=clamp(in.throttle(),-1,1);
            steer=clamp(in.steer(),-1,1); pitch=clamp(in.controlPitch(),-.8,.8);
            bodyPitch=clamp(in.bodyPitch(),-.8,.8); bodyRoll=clamp(in.bodyRoll(),-.4,.4);
            fuel=clamp(in.fuelFraction(),0,1); speed=targetSpeed;
            parking=in.grounded() && !in.occupied() && targetSpeed<.02?1:0;
        } else if(dt>0) {
            rpm=approach(rpm,targetRpm,in.running()?.24:.12,dt);
            throttle=approach(throttle,clamp(in.throttle(),-1,1),.35,dt);
            steer=approach(steer,clamp(in.steer(),-1,1),.45,dt);
            pitch=approach(pitch,clamp(in.controlPitch(),-.8,.8),.30,dt);
            bodyPitch=approach(bodyPitch,clamp(in.bodyPitch(),-.8,.8),.4,dt);
            bodyRoll=approach(bodyRoll,clamp(in.bodyRoll(),-.4,.4),.4,dt);
            fuel=approach(fuel,clamp(in.fuelFraction(),0,1),.2,dt);
            speed=approach(speed,targetSpeed,.3,dt);
            parking=approach(parking,in.grounded() && !in.occupied() && targetSpeed<.02?1:0,.3,dt);
        }
        time=now;
        Phase phase;
        if(in.conditionFraction()<=0) phase=Phase.BROKEN;
        else if(in.fuelFraction()<=0) phase=Phase.EMPTY_TANK;
        else if(!in.running()) phase=rpm>.015?Phase.STOPPING:targetSpeed>.025?Phase.DRIVING:Phase.PARKED;
        else if(rpm<targetRpm*.65) phase=Phase.STARTING;
        else if(in.working()) phase=Phase.WORKING;
        else if(in.type().aircraft() && !in.grounded()) phase=in.type().verticalAircraft() && targetSpeed<.06?Phase.HOVERING:Phase.FLYING;
        else if(in.signedSpeed()*in.throttle()<-.001) phase=Phase.BRAKING;
        else if(in.signedSpeed()<-.008) phase=Phase.REVERSING;
        else if(targetSpeed>.025) phase=Phase.DRIVING;
        else phase=Phase.IDLING;
        return new Frame(phase,(float)rpm,(float)throttle,(float)steer,(float)clamp(-pitch*.4,-.32,.32),
            (float)(-steer*.35),needle(fuel),needle(speed),needle(rpm),(float)parking,(float)clamp(-bodyPitch,-.35,.35),
            (float)clamp(throttle*.09-bodyPitch*.15,-.13,.13),(float)clamp(-steer*.09-bodyRoll*.15,-.13,.13));
    }
    private static float needle(double normalized) { return (float)(-2.1+clamp(normalized,0,1)*4.2); }
    private static double finite(double v) { return Double.isFinite(v)?v:0; }
    private static double clamp(double v,double min,double max) { return Math.max(min,Math.min(max,finite(v))); }
    private static double approach(double old,double target,double response,double dt) {
        return old+(target-old)*-Math.expm1(-response*clamp(dt,0,5));
    }
}
