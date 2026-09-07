package com.harvester.vehicle;

/** Driver/passenger pose layers in vanilla's rigid-limb skeleton, not fabricated elbow bones.
 * Hand directions target moving controls in vehicle-local pixels. Hip stays on the seat;
 * equipment inherits the whole-rider lean. Item use/attack arbitration is handled by the mixin.
 */
public final class PassengerAnimation {
    public record Joint(float pitch,float yaw,float roll) {}
    public record Pose(Joint rightArm,Joint leftArm,Joint rightLeg,Joint leftLeg,float leanPitch,float leanRoll,float weight) {}
    public record Input(VehicleType type,int seat,long identity,double time,double steer,double drive,double speed,boolean working) {}
    public record Grip(double x,double y,double z) {}
    public record Sample(double x,double z,double time,long identity,double speed) {}
    private boolean initialized;
    private long identity;
    private VehicleType type;
    private int seat;
    private double time,steer,drive,speed,acceleration,work,weight;
    public Pose update(Input in) {
        double now=finite(in.time()),dt=now-time;
        if(!initialized || identity!=in.identity() || type!=in.type() || seat!=in.seat() || dt<0 || dt>5) {
            initialized=true;identity=in.identity();type=in.type();seat=in.seat();
            steer=clamp(in.steer(),-1,1);drive=clamp(in.drive(),-1,1);speed=clamp(in.speed(),-.65,.65);
            acceleration=0;work=in.working()?1:0;weight=0;
        } else if(dt>0) {
            steer=approach(steer,clamp(in.steer(),-1,1),.45,dt);
            drive=approach(drive,clamp(in.drive(),-1,1),.35,dt);
            double old=speed;speed=approach(speed,clamp(in.speed(),-.65,.65),.3,dt);
            acceleration=approach(acceleration,clamp((speed-old)/dt,-.035,.035),.25,dt);
            work=approach(work,in.working()?1:0,.3,dt);
            weight=approach(weight,1,.55,dt);
        }
        time=now;
        boolean bike=type.family==VehicleType.Family.MOTORCYCLE,driver=seat==0;
        double motion=clamp(Math.abs(speed)/type.speed,0,1);
        double leanPitch=clamp((bike?(driver?.055:.025):0)-acceleration*2.5,-.10,.10);
        double leanRoll=clamp(steer*motion*(bike?.028:-.018),-.04,.04);
        Joint right,left;
        if(driver) {
            right=arm(-1,grip(type,-1,steer,drive),leanPitch,leanRoll);
            left=arm(1,grip(type,1,steer,drive),leanPitch,leanRoll);
        } else {
            // Pillion grips the rear support, never the driver's moving handlebar.
            right=new Joint(.16f,-.04f,.04f);left=new Joint(.16f,.04f,-.04f);
        }
        var base=PassengerPose.limbs(type,seat);
        double press=driver && !bike?Math.max(0,drive)*.055:0;
        double brake=driver && !bike?Math.max(0,-drive)*.085:0;
        Joint rightLeg=new Joint((float)(base.legPitch()-press),(float)base.legSpread(),.05f);
        Joint leftLeg=new Joint((float)(base.legPitch()-brake),(float)-base.legSpread(),-.05f);
        // Work resistance is a small torso response; hands are solved again around that torso.
        if(type.worker() && driver) {
            leanPitch+=work*.012;
            right=arm(-1,grip(type,-1,steer,drive),leanPitch,leanRoll);
            left=arm(1,grip(type,1,steer,drive),leanPitch,leanRoll);
        }
        return new Pose(right,left,rightLeg,leftLeg,(float)leanPitch,(float)leanRoll,(float)weight);
    }
    /** Hand target relative to the seat/hip. Same pivot distances as VehicleGeometry controls. */
    public static Grip grip(VehicleType type,int side,double steering,double drive) {
        steering=clamp(steering,-1,1);drive=clamp(drive,-1,1);side=side<0?-1:1;
        if(type.family==VehicleType.Family.MOTORCYCLE) {
            double a=-steering*Math.toRadians(22),x=side*5;
            return new Grip(x*Math.cos(a),5,7.5-x*Math.sin(a));
        }
        if(type.family==VehicleType.Family.DOZER || type.verticalAircraft()) {
            double a=clamp(drive+(side<0?-steering:steering),-1,1)*.3;
            return new Grip(side*3,2+3.5*Math.cos(a),7+3.5*Math.sin(a));
        }
        double a=-steering*Math.toRadians(22)*1.7,x=side*2.75;
        return new Grip(x*Math.cos(a),5+x*Math.sin(a),8);
    }
    private static Joint arm(int side,Grip grip,double pitch,double roll) {
        // Undo the extra whole-rider lean before solving the shoulder-to-control direction.
        double cr=Math.cos(-roll),sr=Math.sin(-roll),cp=Math.cos(-pitch),sp=Math.sin(-pitch);
        double x=grip.x()*cr-grip.y()*sr,y=grip.x()*sr+grip.y()*cr;
        double yy=y*cp-grip.z()*sp,z=y*sp+grip.z()*cp;
        double dx=x-side*4.6875,down=9.375-yy;
        return new Joint((float)clamp(-Math.atan2(Math.hypot(dx,z),down),-1.65,.4),
            (float)clamp(-Math.atan2(dx,z),-.85,.85),0);
    }
    public static float blend(float original,float target,float weight) {
        return (float)(finite(original)+(finite(target)-finite(original))*clamp(weight,0,1));
    }
    private static double finite(double v) { return Double.isFinite(v)?v:0; }
    private static double clamp(double v,double min,double max) { return Math.max(min,Math.min(max,finite(v))); }
    private static double approach(double old,double target,double response,double dt) { return old+(target-old)*-Math.expm1(-response*clamp(dt,0,5)); }
}
