package com.prokstudio.militaryvehicles;
import com.prokstudio.militaryvehicles.core.*;
import java.util.*;

/** Executable production-core regressions; no stubs for Minecraft and no claims of world/game QA. */
public final class OperationsCases {
    private static long checks;
    private static void check(boolean value,String context) {checks++;if(!value) throw new AssertionError(context);}
    private static void equal(double a,double b) {check(Math.abs(a-b)<1e-7,a+" != "+b);}
    private static void rejects(Runnable r) {boolean failed=false;try {r.run();}catch(IllegalArgumentException e){failed=true;}check(failed,"must reject");}
    private static TrackDrive.State run(VehicleKind k,int keys,int ticks) {var s=new TrackDrive.State(0,0,0);for(int i=0;i<ticks;i++)s=TrackDrive.step(k,s,s.speed(),keys,true,true,1);return s;}
    public static void counterRotatingTracksPivotAtRest() {
        for(var k:List.of(VehicleKind.TANK,VehicleKind.HOWITZER)) {var s=run(k,ControlLatch.RIGHT,20);equal(0,s.speed());check(s.left()>0&&s.right()<0,"counter-rotation");check(s.yaw()>0,"pivot yaw");}
    }
    public static void bothDirectionsRespectPerTrackLimits() {
        for(var k:List.of(VehicleKind.TANK,VehicleKind.HOWITZER)) for(int keys=0;keys<64;keys++) for(boolean power:new boolean[]{false,true}) for(boolean ground:new boolean[]{false,true}) for(double traction:new double[]{.25,.6,1}) {
            var s=new TrackDrive.State(0,0,179);
            for(int tick=0;tick<90;tick++) {var next=TrackDrive.step(k,s,s.speed(),keys,power,ground,traction);
                check(Double.isFinite(next.left()+next.right()),"finite tracks");check(next.left()>=-k.handling.reverse()-1e-9&&next.left()<=k.handling.forward()+1e-9,"left bounds");
                check(next.right()>=-k.handling.reverse()-1e-9&&next.right()<=k.handling.forward()+1e-9,"right bounds");check(next.yaw()>=-180&&next.yaw()<180,"wrapped yaw");
                check(Math.abs(TruckPhysics.wrap(next.yaw()-s.yaw()))<=k.handling.yawRate()+.0001,"yaw rate");s=next;
            }
        }
        for(var k:List.of(VehicleKind.TANK,VehicleKind.HOWITZER)) {equal(k.handling.forward(),run(k,ControlLatch.FORWARD,200).speed());equal(-k.handling.reverse(),run(k,ControlLatch.BACK,200).speed());}
    }
    public static void brakingStopsBothTracksIncludingPivot() {
        for(var k:List.of(VehicleKind.TANK,VehicleKind.HOWITZER)) for(int keys:new int[]{ControlLatch.RIGHT,ControlLatch.FORWARD,ControlLatch.BACK}) {
            var s=run(k,keys,100);for(int i=0;i<20;i++) s=TrackDrive.step(k,s,s.speed(),ControlLatch.BRAKE|ControlLatch.FORWARD|ControlLatch.RIGHT,true,true,1);
            equal(0,s.left());equal(0,s.right());
        }
    }
    public static void oppositeInputCrossesZeroBeforeReversing() {
        var k=VehicleKind.TANK;var s=run(k,ControlLatch.FORWARD,100);boolean zero=false;
        for(int i=0;i<100;i++) {var next=TrackDrive.step(k,s,s.speed(),ControlLatch.BACK,true,true,1);if(next.speed()==0)zero=true;if(next.speed()<0)check(zero,"reverse crossed zero");s=next;}
        equal(-k.handling.reverse(),s.speed());
    }
    public static void unpoweredAndAirCannotAddKineticEnergy() {
        for(var k:List.of(VehicleKind.TANK,VehicleKind.HOWITZER)) for(int keys=0;keys<32;keys++) for(boolean ground:new boolean[]{false,true}) {
            var stopped=TrackDrive.step(k,new TrackDrive.State(0,0,10),0,keys,false,ground,1);equal(0,stopped.speed());equal(10,stopped.yaw());
            var air=TrackDrive.step(k,new TrackDrive.State(.1,.05,10),.075,keys,true,false,1);check(Math.abs(air.left())<=.1&&Math.abs(air.right())<=.05,"no air torque");equal(10,air.yaw());
        }
    }
    public static void collisionCorrectionDoesNotStoreForwardImpulse() {
        var k=VehicleKind.TANK;var old=run(k,ControlLatch.FORWARD,100);
        var result=TrackDrive.step(k,old,0,0,false,true,1);equal(0,result.speed());
        var throttle=TrackDrive.step(k,old,0,ControlLatch.FORWARD,true,true,1);check(throttle.speed()<=k.handling.acceleration()+1e-9,"wall cannot bank thrust");
    }
    public static void invalidValuesFailClosed() {
        for(double bad:new double[]{Double.NaN,Double.POSITIVE_INFINITY,Double.NEGATIVE_INFINITY}) {
            var s=TrackDrive.step(VehicleKind.TANK,new TrackDrive.State(bad,0,1),0,1,true,true,1);equal(0,s.left());equal(0,s.right());
            s=TrackDrive.step(VehicleKind.TANK,new TrackDrive.State(0,0,1),bad,1,true,true,1);equal(0,s.speed());
        }
        equal(0,TrackDrive.step(VehicleKind.TANK,new TrackDrive.State(0,0,Float.NaN),0,1,true,true,1).speed());
        equal(0,TrackDrive.step(VehicleKind.TANK,new TrackDrive.State(.1,.1,1),.1,128,true,true,1).speed());
        rejects(()->TrackDrive.step(VehicleKind.TRUCK,new TrackDrive.State(0,0,0),0,0,true,true,1));
    }
    public static void turningDirectionIsConsistentInReverse() {
        for(var k:List.of(VehicleKind.TANK,VehicleKind.HOWITZER)) {
            var f=run(k,ControlLatch.FORWARD|ControlLatch.RIGHT,10);var r=run(k,ControlLatch.BACK|ControlLatch.RIGHT,10);var l=run(k,ControlLatch.LEFT,10);
            check(f.yaw()>0&&r.yaw()>0&&l.yaw()<0,"differential direction");
            equal(run(k,ControlLatch.RIGHT,10).yaw(),-l.yaw());
        }
    }
    public static void padsCirculateBoundedlyAndIndependently() {
        for(double phase=-100;phase<=100;phase+=.73) for(double z=-25.5;z<=25.5;z+=3) for(boolean upper:new boolean[]{false,true}) {
            double d=TrackDrive.treadOffset(z,phase,upper);check(z+d>=-27&&z+d<27,"track pad wrap");equal(d,TrackDrive.treadOffset(z,phase+54/6.5,upper));
        }
        equal(0,TrackDrive.treadOffset(1,Double.NaN,true));check(TrackDrive.treadOffset(0,.1,true)!=TrackDrive.treadOffset(0,.1,false),"opposite runs");
    }
    public static void driverAndGunnerPrivilegesAreSeparate() {
        for(var k:VehicleKind.values()) for(int seat=-1;seat<=6;seat++) for(int action=0;action<5;action++) {
            boolean expected=action==2?k==VehicleKind.HOWITZER&&seat==0:action==1&&(k.armed()?seat==1:k.support()&&seat==0);
            check(expected==VehicleOperations.authorized(k,seat,action),"crew privileges");
        }
    }
    public static void deploymentLocksFromIntentThroughRetraction() {
        var d=new VehicleOperations.Deployment(0,false);check(!d.locked(),"stowed unlocked");d=d.toggle(true,true);check(d.locked(),"intent locks immediately");
        for(int i=1;i<=60;i++) {d=d.tick(true,true,true);check(d.ready()==(i==60),"full deployment only");check(d.locked(),"deploy locked");}
        d=d.toggle(true,true);check(!d.ready(),"retract cannot fire");
        for(int i=59;i>=0;i--) {d=d.tick(true,true,true);check(d.ticks()==i,"retraction ticks");check(d.locked()==(i>0),"retract lock");}
    }
    public static void deploymentRejectsMotionAirAndFaults() {
        var d=new VehicleOperations.Deployment(0,false);check(d.equals(d.toggle(false,true)),"moving intent rejected");check(d.equals(d.toggle(true,false)),"airborne rejected");
        d=new VehicleOperations.Deployment(60,true);for(var next:List.of(d.tick(false,true,true),d.tick(true,false,true),d.tick(true,true,false)))check(next.ticks()==59&&!next.ready()&&next.locked(),"fault retracts without immediate unlock");
        rejects(()->new VehicleOperations.Deployment(-1,true));rejects(()->new VehicleOperations.Deployment(61,true));
    }
    public static void cancelledDeploymentCannotFire() {
        var d=new VehicleOperations.Deployment(30,true).toggle(true,true);
        check(!VehicleOperations.canFire(VehicleKind.HOWITZER,0,1,true,true,false,d),"partial/cancelled cannot fire");
        check(!VehicleOperations.canFire(VehicleKind.HOWITZER,0,1,true,true,false,new VehicleOperations.Deployment(60,false)),"retracting cannot fire");
    }
    public static void fuelServiceConservesTotalsAndReserve() {
        for(int source=0;source<=9600;source+=23) for(var kind:VehicleKind.values()) for(int target=0;target<=kind.tank;target+=37) {
            int moved=VehicleOperations.transfer(source,target,kind.tank);check(moved>=0&&moved<=600,"bounded transfer");check(source-moved>=Math.min(source,200),"donor reserve");
            check(target+moved<=kind.tank,"recipient capacity");equal(source+target,(source-moved)+(target+moved));
        }
    }
    public static void fuelServiceRejectsInvalidStores() {
        for(int source:new int[]{-1,9601,Integer.MAX_VALUE})equal(0,VehicleOperations.transfer(source,0,2400));
        equal(0,VehicleOperations.transfer(600,-1,2400));equal(0,VehicleOperations.transfer(600,2401,2400));equal(0,VehicleOperations.transfer(600,0,0));
        equal(0,VehicleOperations.transfer(200,0,2400));equal(7,VehicleOperations.transfer(600,2393,2400));
    }
    public static void repairsRequireAStockedKitAndClampToCapacity() {
        for(var k:VehicleKind.values()) for(int condition=0;condition<=k.condition;condition++) {
            equal(0,VehicleOperations.repair(condition,k.condition,0));int restored=VehicleOperations.repair(condition,k.condition,1);
            equal(Math.min(100,k.condition-condition),restored);check(condition+restored<=k.condition,"repair capacity");
        }
        equal(0,VehicleOperations.repair(-1,200,1));equal(0,VehicleOperations.repair(201,200,1));
    }
    public static void gunRequiresAmmoFuelGroundAndCooldown() {
        var deployed=new VehicleOperations.Deployment(60,true);
        for(var k:VehicleKind.values()) for(int cooldown:new int[]{-1,0,1,100}) for(int shells:new int[]{0,1}) for(boolean functional:new boolean[]{false,true}) for(boolean grounded:new boolean[]{false,true}) for(boolean water:new boolean[]{false,true}) {
            boolean expected=k.armed()&&cooldown==0&&shells>0&&functional&&grounded&&!water;
            check(expected==VehicleOperations.canFire(k,cooldown,shells,functional,grounded,water,deployed),"gun readiness");
        }
        check(VehicleOperations.gunCooldown(VehicleKind.HOWITZER)>VehicleOperations.gunCooldown(VehicleKind.TANK),"separate cadence");
    }
    public static void aimIsRateLimitedWrappedAndPitchClamped() {
        equal(-178,VehicleOperations.aimYaw(179,-175));
        for(var k:List.of(VehicleKind.TANK,VehicleKind.HOWITZER,VehicleKind.IFV)) for(int target=-720;target<=720;target++) {
            float yaw=0,pitch=0;for(int i=0;i<60;i++) {float next=VehicleOperations.aimYaw(yaw,target);check(Math.abs(TruckPhysics.wrap(next-yaw))<=3.001,"yaw slew");yaw=next;
                pitch=VehicleOperations.aimPitch(k,pitch,target);check(pitch>=VehicleOperations.minPitch(k)&&pitch<=VehicleOperations.maxPitch(k),"elevation limits");}
        }
        equal(0,VehicleOperations.aimYaw(0,Float.NaN));equal(0,VehicleOperations.aimPitch(VehicleKind.TANK,Float.NaN,0));
    }
    public static void stationParkingRejectsNonFiniteSpeed() {
        for(double v:new double[]{Double.NaN,Double.POSITIVE_INFINITY,-1,.000026,1})check(!VehicleOperations.parked(v),"not safely parked");
        check(VehicleOperations.parked(0)&&VehicleOperations.parked(.000025),"park bounds");
    }
    public static void saveProfilesPreserveFleetAndRejectCrossType() {
        for(var k:VehicleKind.values()) {
            var cargo=new ArrayList<>(Collections.nCopies(k.cargoSlots(),"occupied"));var save=new VehicleSave<>(1,k.id,k.tank,k.condition,9,cargo);cargo.set(0,"mutated");
            check(save.cargo().getFirst().equals("occupied"),"owned save");
            for(var other:VehicleKind.values()) {if(other==k)save.requireType(other);else rejects(()->save.requireType(other));}
            rejects(()->new VehicleSave<>(1,k.id,k.tank+1,k.condition,9,save.cargo()));
        }
    }
    public static void newRigsHaveOwnMissionHardware() {
        Map<VehicleKind,String> required=Map.of(VehicleKind.TANK,"turret_armor",VehicleKind.HOWITZER,"turret_shield",VehicleKind.IFV,"turret_atgm_pod",VehicleKind.TANKER,"faceted_reservoir",VehicleKind.WORKSHOP,"workshop_box",VehicleKind.RECOVERY,"winch_mast");
        Set<List<TruckGeometry.Part>> all=new HashSet<>();
        for(var k:VehicleKind.values()) {var rig=VehicleGeometry.create(k);check(all.add(rig),"distinct rigs");if(required.containsKey(k))check(rig.stream().anyMatch(p->p.name().equals(required.get(k))),"mission hardware");}
        equal(4,VehicleGeometry.create(VehicleKind.HOWITZER).stream().filter(p->p.name().startsWith("outrigger_leg_")).count());
        // The fighting compartment is modelled, not implied: glazed ports, their frames and a boarding ramp.
        equal(4,VehicleGeometry.create(VehicleKind.IFV).stream().filter(p->p.name().startsWith("firing_port_")).count());
        check(VehicleGeometry.create(VehicleKind.IFV).stream().anyMatch(p->p.name().equals("rear_ramp")),"boarding ramp");
        check(VehicleGeometry.create(VehicleKind.TRUCK).equals(TruckGeometry.create()),"truck unchanged");
    }
    private static double[] rotate(double x,double y,double z,double rx,double ry) {
        double yy=y*Math.cos(rx)-z*Math.sin(rx),zz=y*Math.sin(rx)+z*Math.cos(rx);return new double[]{x*Math.cos(ry)+zz*Math.sin(ry),yy,-x*Math.sin(ry)+zz*Math.cos(ry)};
    }
    public static void animatedTrackedAndServiceRigsFitCollision() {
        for(var k:VehicleKind.values()) if(k.tracked()||k.support()) for(var p:VehicleGeometry.create(k)) for(int frame=0;frame<32;frame++) for(var b:p.boxes())
            for(double x:new double[]{b.x(),b.x()+b.w()}) for(double y:new double[]{b.y(),b.y()+b.h()}) for(double z:new double[]{b.z(),b.z()+b.d()}) {
                double spin=frame*Math.PI/16;double[] v={x,y,z};String n=p.name();
                if(p.wheel())v=rotate(x,y,z,spin,p.front()?-k.handling.steer():0);
                else if(n.startsWith("tread_"))v[2]+=TrackDrive.treadOffset(p.z(),spin,n.startsWith("tread_upper_"));
                else if(n.startsWith("gun_"))v=rotate(x,y,z,Math.toRadians(frame%2==0?VehicleOperations.minPitch(k):VehicleOperations.maxPitch(k)),0);
                else if(n.startsWith("outrigger_leg_"))v[1]-=frame%2*5;
                v[0]+=p.x();v[1]+=p.y();v[2]+=p.z();
                if(n.startsWith("turret_")&&!n.equals("turret_ring")||n.startsWith("gun_")) {v=rotate(v[0],v[1]-ExpansionGeometry.TURRET_Y,v[2]-ExpansionGeometry.TURRET_Z,0,spin);v[1]+=ExpansionGeometry.TURRET_Y;v[2]+=ExpansionGeometry.TURRET_Z;}
                check(v[1]>=-1e-5&&v[1]<=k.height*16+1e-5,k+" animated height "+n);
                check(Math.hypot(v[0],v[2])<=k.width*8+1e-5,k+" animated collider "+n);
            }
    }
    public static void newProfilesHaveExpectedCapacitiesAndDriveFamilies() {
        equal(9,VehicleKind.values().length);equal(3,Arrays.stream(VehicleKind.values()).filter(VehicleKind::tracked).count());equal(3,Arrays.stream(VehicleKind.values()).filter(VehicleKind::support).count());
        equal(8,VehicleOperations.maxPitch(VehicleKind.HOWITZER)); // Direct-fire mode needs depression to reach nearby ground targets.
        equal(9600,VehicleKind.TANKER.tank);equal(27,VehicleKind.WORKSHOP.cargoSlots());equal(18,VehicleKind.RECOVERY.cargoSlots());equal(500,VehicleKind.TANK.condition);
        for(var k:List.of(VehicleKind.TANK,VehicleKind.HOWITZER)) {equal(14,k.wheels);equal(0,k.steeringWheels);equal(2,k.seats.size());}
        // The fighting vehicle rides the tracked family but carries a squad, so it trades reach and punch for elevation and room.
        equal(12,VehicleKind.IFV.wheels);equal(0,VehicleKind.IFV.steeringWheels);equal(6,VehicleKind.IFV.seats.size());
        equal(18,VehicleKind.IFV.cargoSlots());equal(2600,VehicleKind.IFV.tank);equal(300,VehicleKind.IFV.condition);
        check(VehicleOperations.minPitch(VehicleKind.IFV)<VehicleOperations.minPitch(VehicleKind.TANK),"autocannon depresses further");
        check(VehicleOperations.maxPitch(VehicleKind.IFV)>VehicleOperations.maxPitch(VehicleKind.TANK),"autocannon elevates higher");
        check(VehicleOperations.gunDamage(VehicleKind.IFV)<VehicleOperations.gunDamage(VehicleKind.TANK),"lighter round");
        check(VehicleOperations.gunRange(VehicleKind.IFV)<VehicleOperations.gunRange(VehicleKind.TANK),"shorter reach");
        equal(VehicleOperations.gunCooldown(VehicleKind.TANK),VehicleOperations.gunCooldown(VehicleKind.IFV));
    }
    public static long runAll() {
        checks=0;
        counterRotatingTracksPivotAtRest();bothDirectionsRespectPerTrackLimits();brakingStopsBothTracksIncludingPivot();oppositeInputCrossesZeroBeforeReversing();unpoweredAndAirCannotAddKineticEnergy();collisionCorrectionDoesNotStoreForwardImpulse();invalidValuesFailClosed();turningDirectionIsConsistentInReverse();padsCirculateBoundedlyAndIndependently();
        driverAndGunnerPrivilegesAreSeparate();deploymentLocksFromIntentThroughRetraction();deploymentRejectsMotionAirAndFaults();cancelledDeploymentCannotFire();fuelServiceConservesTotalsAndReserve();fuelServiceRejectsInvalidStores();repairsRequireAStockedKitAndClampToCapacity();gunRequiresAmmoFuelGroundAndCooldown();aimIsRateLimitedWrappedAndPitchClamped();stationParkingRejectsNonFiniteSpeed();
        saveProfilesPreserveFleetAndRejectCrossType();newRigsHaveOwnMissionHardware();animatedTrackedAndServiceRigsFitCollision();newProfilesHaveExpectedCapacitiesAndDriveFamilies();
        return checks;
    }
}
