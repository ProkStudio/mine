package com.prokstudio.militaryvehicles;
import com.prokstudio.militaryvehicles.core.*;
import java.util.*;
/** Standalone executed checks, not a replacement for JUnit/Fabric compilation or Minecraft acceptance. */
public final class FleetSmoke {
    private static int assertions;
    private static void check(boolean okay,String detail) { assertions++;if(!okay) throw new AssertionError(detail); }
    public static void main(String[] args) {
        Set<String> ids=new HashSet<>();Set<List<TruckGeometry.Part>> rigs=new HashSet<>();
        for(var k:VehicleKind.values()) {
            check(ids.add(k.id),"unique ids");check(VehicleKind.require(k.id)==k,"lookup");
            var rig=VehicleGeometry.create(k);check(rigs.add(rig),"distinct rigs");
            var tires=rig.stream().filter(p->p.wheel()&&p.material().equals("rubber")).toList();
            check(tires.size()==k.wheels,"wheel count");check(tires.stream().filter(TruckGeometry.Part::front).count()==k.steeringWheels,"steering wheel count");
            Set<String> names=new HashSet<>();
            for(var p:rig) {
                check(names.add(p.name()),"part names");check(VehicleGeometry.MATERIALS.containsKey(p.material()),"material");
                for(var b:p.boxes()) for(float x:new float[]{p.x()+b.x(),p.x()+b.x()+b.w()}) for(float y:new float[]{p.y()+b.y(),p.y()+b.y()+b.h()}) for(float z:new float[]{p.z()+b.z(),p.z()+b.z()+b.d()}) {
                    check(y>=-1e-5&&y/16<=k.height,k+" height "+p.name());check(Math.hypot(x,z)/16<=k.width/2,k+" collider "+p.name());
                }
                if(k!=VehicleKind.TRUCK&&p.wheel()) for(int frame=0;frame<32;frame++) for(float steer:new float[]{-k.handling.steer(),0,k.handling.steer()})
                    for(var b:p.boxes()) for(float x:new float[]{b.x(),b.x()+b.w()}) for(float y:new float[]{b.y(),b.y()+b.h()}) for(float z:new float[]{b.z(),b.z()+b.d()}) {
                        double spin=frame*Math.PI/16,turn=p.front()?-steer:0;
                        double yy=y*Math.cos(spin)-z*Math.sin(spin),zz=y*Math.sin(spin)+z*Math.cos(spin);
                        double xx=x*Math.cos(turn)+zz*Math.sin(turn),zzz=-x*Math.sin(turn)+zz*Math.cos(turn);
                        check(p.y()+yy>=-1e-5,k+" animated ground "+p.name());check(Math.hypot(p.x()+xx,p.z()+zzz)/16<=k.width/2,k+" animated collider "+p.name());
                    }
            }
            for(int keys=0;keys<32;keys++) for(boolean powered:new boolean[]{false,true}) for(boolean ground:new boolean[]{false,true}) for(double traction:new double[]{.25,.6,1}) {
                double speed=0;float yaw=0;
                for(int tick=0;tick<50;tick++) {
                    var m=TruckPhysics.step(k,speed,yaw,keys,powered,ground,traction);speed=m.speed();yaw=m.yaw();
                    check(Double.isFinite(speed)&&speed>=-k.handling.reverse()-1e-9&&speed<=k.handling.forward()+1e-9,"speed");
                    check(Float.isFinite(yaw)&&yaw>=-180&&yaw<180,"yaw");check(Math.abs(m.steer())<=k.handling.steer(),"steering");
                }
            }
            double forward=0,reverse=0;
            for(int i=0;i<300;i++) {forward=TruckPhysics.step(k,forward,0,ControlLatch.FORWARD,true,true,1).speed();reverse=TruckPhysics.step(k,reverse,0,ControlLatch.BACK,true,true,1).speed();}
            check(forward==k.handling.forward(),"forward limit");check(reverse==-k.handling.reverse(),"reverse limit");
            var list=new ArrayList<>(Collections.nCopies(k.cargoSlots(),"cargo"));var state=new VehicleSave<>(1,k.id,k.tank,k.condition,9,list);list.set(0,"changed");check(state.cargo().getFirst().equals("cargo"),"own cargo list");
            for(var target:VehicleKind.values()) {
                boolean rejected=false;try {state.requireType(target);}catch(IllegalArgumentException ex){rejected=true;}
                check(rejected==(k!=target),"cross-type rejection");
            }
            check(TruckPhysics.transferFuel(k,k.tank-7,600)==7,"fuel capacity");
            var stack=rig.stream().filter(p->p.name().equals("exhaust_stack")).findFirst().orElseThrow();var outlet=stack.boxes().stream().min(Comparator.comparingDouble(TruckGeometry.Box::z)).orElseThrow();
            check(Math.abs(stack.x()+outlet.x()+outlet.w()/2-k.exhaust.x())<1e-5,"exhaust x");check(Math.abs(stack.y()+outlet.y()+outlet.h()/2-k.exhaust.y())<1e-5,"exhaust y");check(Math.abs(stack.z()+outlet.z()-.5-k.exhaust.z())<1e-5,"exhaust z");
        }
        check(VehicleGeometry.create(VehicleKind.TRUCK).equals(TruckGeometry.create()),"legacy rig unchanged");
        check(VehicleKind.BUGGY.handling.forward()>VehicleKind.TRUCK.handling.forward()&&VehicleKind.TRUCK.handling.forward()>VehicleKind.CARRIER.handling.forward(),"distinct speed roles");
        System.out.println("FLEET_SMOKE_PASS assertions="+assertions);
    }
}
