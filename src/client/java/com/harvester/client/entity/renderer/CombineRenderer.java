package com.harvester.client.entity.renderer;

import com.harvester.entity.CombineEntity;
import com.harvester.vehicle.*;
import net.minecraft.client.model.*;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.*;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import java.util.*;

/** Per-type immutable atlas meshes; the deferred queue receives only value snapshots and matrices.
 * Uses vanilla entity layers, not a core-shader replacement or block-atlas coordinate trick.
 */
public class CombineRenderer extends EntityRenderer<CombineEntity,CombineRenderState> {
    private record BakedPart(VehicleGeometry.Part definition,ModelPart model,Identifier texture,double wheelRadius) {}
    private static final class History {
        final VehicleAnimation animation=new VehicleAnimation();
        final VehiclePresentation presentation=new VehiclePresentation();
        final Map<String,Double> targets=new HashMap<>();
        final Map<String,Float> springs=new HashMap<>();
        int sampleAge=-1; double time,sampleTime,x,z,speed;boolean hasPosition; VehicleType type;
    }
    private final Map<VehicleType,List<BakedPart>> models=new EnumMap<>(VehicleType.class);
    private final Map<CombineEntity,History> histories=new WeakHashMap<>();
    private static final int[] PALETTE={0xffffff,0xe0e1dc,0xc78643,0xa65b95,0x81abc0,0xc7b752,0xc98994,0x60696c,0xb0b8b9,0x438e93,0x785d9a,0x466c9c,0x826650,0x507246,0xac5145,0x353c40};
    public CombineRenderer(EntityRendererFactory.Context context) {
        super(context); shadowRadius=1;
        for(VehicleType type:VehicleType.values()) {
            List<BakedPart> parts=new ArrayList<>(); var definitions=VehicleGeometry.create(type);
            var atlas=VehicleAtlas.layout(definitions);
            Identifier texture=Identifier.of("harvester","textures/vehicle/atlas_"+type.id+".png");
            for(var p:definitions) {
                ModelData data=new ModelData(); ModelPartBuilder builder=ModelPartBuilder.create();
                for(int i=0;i<p.boxes().size();i++) {
                    var b=p.boxes().get(i);var uv=atlas.island(p.name(),i);float d=VehicleAtlas.DENSITY;
                    builder.uv(uv.u(),uv.v()).cuboid(b.x()*d,b.y()*d,b.z()*d,b.w()*d,b.h()*d,b.d()*d);
                }
                data.getRoot().addChild("mesh",builder,ModelTransform.origin(0,0,0));
                ModelPart mesh=TexturedModelData.of(data,atlas.size(),atlas.size()).createModel();
                double radius=p.name().startsWith("wheel")?VehicleAnimation.wheelRadius(p,definitions):1;
                parts.add(new BakedPart(p,mesh,texture,radius));
            }
            models.put(type,List.copyOf(parts));
        }
    }
    @Override public CombineRenderState createRenderState() { return new CombineRenderState(); }
    @Override public void updateRenderState(CombineEntity entity,CombineRenderState state,float tickProgress) {
        super.updateRenderState(entity,state,tickProgress);
        state.variant=entity.variant(); state.color=entity.getColor(); state.yaw=entity.getLerpedYaw(tickProgress);
        state.wheels=entity.wheelAngle(); state.harvesting=entity.isWorking(); state.engineActive=entity.isEngineActive();
        state.headerEnabled=entity.isHeaderEnabled(); state.inputSteer=entity.steeringInput(); state.inputDrive=entity.driveInput();
        state.bodyPose=entity.bodyPose(tickProgress); state.controlPitch=(float)Math.toRadians(entity.getPitch());
        state.animationTime=entity.age+tickProgress;
        History h=histories.get(entity);
        if(h==null || h.type!=state.variant) { h=new History();h.type=state.variant;histories.put(entity,h); }
        var position=entity.getLerpedPos(tickProgress);
        var f=h.animation.update(state.animationTime,position.x,position.z,state.yaw,state.engineActive,state.headerEnabled,state.variant.family,state.inputSteer,state.inputDrive);
        var mechanisms=h.animation.mechanisms(state.animationTime,state.yaw,state.harvesting);
        state.wheelTravel=f.wheelTravel(); state.engineRotor=f.engineRotor(); state.headerLift=f.headerLift(); state.steering=f.steering();
        state.rotor=mechanisms.workAngle(); state.workingStrength=mechanisms.workingStrength(); state.yawTravel=mechanisms.yawTravel();
        double sampleDt=state.animationTime-h.sampleTime;
        if(h.hasPosition && sampleDt>0 && sampleDt<=5 && Math.hypot(position.x-h.x,position.z-h.z)<8)
            h.speed=VehicleAnimation.signedDistance(position.x-h.x,position.z-h.z,state.yaw)/sampleDt;
        else if(!h.hasPosition || sampleDt<0 || sampleDt>5 || Math.hypot(position.x-h.x,position.z-h.z)>=8) h.speed=0;
        h.hasPosition=true;h.x=position.x;h.z=position.z;h.sampleTime=state.animationTime;
        state.presentation=h.presentation.update(new VehiclePresentation.Input(state.animationTime,state.variant,state.engineActive,
            state.harvesting,entity.hasPassengers(),entity.isOnGround(),h.speed,
            state.inputDrive,state.inputSteer,state.controlPitch,state.bodyPose.pitch(),state.bodyPose.roll(),
            entity.getFuel()/(double)Math.max(1,entity.stats().tank),entity.getCondition()/(double)Math.max(1,entity.stats().durability)));
        if(h.sampleAge!=entity.age) {
            h.sampleAge=entity.age;
            for(var part:models.get(state.variant)) {
                var d=part.definition(); if(!d.name().startsWith("wheel_") || d.name().startsWith("wheel_hub_")) continue;
                double target=0;
                if(entity.isOnGround()) {
                    var at=entity.localEffect(d.px()/16.0,d.py()/16.0,d.pz()/16.0);
                    var ground=VehicleGround.sample(entity.getEntityWorld(),at.x,at.y+.4,at.z,3);
                    if(ground!=null && !ground.water()) target=VehiclePhysics.clamp(ground.y()+part.wheelRadius()-at.y,-.12,.12);
                }
                h.targets.put(d.name(),target);
            }
        }
        double dt=VehiclePhysics.clamp(state.animationTime-h.time,0,5); h.time=state.animationTime;
        for(var entry:h.targets.entrySet()) h.springs.put(entry.getKey(),(float)VehicleAnimation.smooth(h.springs.getOrDefault(entry.getKey(),0f),entry.getValue(),.4,dt));
        state.suspension=Map.copyOf(h.springs);
    }
    private static float trackSpring(CombineRenderState s,String side) {
        double total=0;int count=0;
        for(var e:s.suspension.entrySet()) if(e.getKey().startsWith("wheel_track"+side+"_")) { total+=e.getValue();count++; }
        return count==0?0:(float)(total/count);
    }
    @Override public void render(CombineRenderState state,MatrixStack matrices,OrderedRenderCommandQueue queue,CameraRenderState camera) {
        super.render(state,matrices,queue,camera);
        matrices.push(); matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-state.yaw));
        var pivot=VehicleGeometry.seat(state.variant,0);
        matrices.translate(pivot.x()/16,pivot.top()/16,pivot.z()/16);
        matrices.multiply(RotationAxis.POSITIVE_Z.rotation(state.bodyPose.roll()));
        matrices.multiply(RotationAxis.POSITIVE_X.rotation(state.bodyPose.pitch()));
        matrices.translate(-pivot.x()/16,-pivot.top()/16,-pivot.z()/16);
        for(BakedPart part:models.get(state.variant)) {
            var def=part.definition();String name=def.name();matrices.push();
            boolean header=def.axis()=='h' || name.equals("reel"),wheel=name.startsWith("wheel");
            boolean frontWheel=name.startsWith("wheel_front") || name.startsWith("wheel_hub_front");
            double travel=state.wheelTravel;
            if(state.variant.family==VehicleType.Family.DOZER) travel+=state.yawTravel*def.px()/16;
            boolean outboard=state.variant.family==VehicleType.Family.BOAT && (name.startsWith("outboard") || name.equals("propeller"));
            boolean bikeFront=state.variant.family==VehicleType.Family.MOTORCYCLE && (frontWheel || name.startsWith("front_") || name.equals("steering"));
            if(bikeFront) {
                matrices.translate(0,5.5/16,12/16.0);
                matrices.multiply(RotationAxis.POSITIVE_Y.rotation(-state.steering));
                matrices.translate(0,-5.5/16,-12/16.0);
            }
            if(outboard) {
                double length=state.variant==VehicleType.BOAT_CARGO?40:34;
                matrices.translate(0,5/16.0,(-length/2-1)/16);
                matrices.multiply(RotationAxis.POSITIVE_Y.rotation(-state.steering));
                matrices.translate(0,-5/16.0,(length/2+1)/16);
            }
            if(def.axis()=='t') {
                String[] fields=name.split("_");int index=Integer.parseInt(fields[2]);
                var at=VehicleGeometry.trackPoint(index*VehicleGeometry.TRACK_PERIMETER/32+travel*16);
                matrices.translate(def.px()/16.0,at.y()/16+trackSpring(state,fields[1]),at.z()/16);
                matrices.multiply(RotationAxis.POSITIVE_X.rotation((float)at.angle()));
            } else {
                double spring=wheel?state.suspension.getOrDefault(name.replace("wheel_hub_","wheel_"),0f):0;
                matrices.translate(def.px()/16.0,(def.py()+(header?state.headerLift:0))/16.0+spring,def.pz()/16.0);
                if(def.restRoll()!=0) matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(def.restRoll()));
                if(def.restYaw()!=0) matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(def.restYaw()));
                if(def.restPitch()!=0) matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(def.restPitch()));
                if(frontWheel && !bikeFront) matrices.multiply(RotationAxis.POSITIVE_Y.rotation(-state.steering));
                if(name.equals("header_teeth")) matrices.translate(Math.sin(state.rotor*6)*.025*state.workingStrength,0,0);
                if(def.axis()=='b') matrices.translate(0,Math.sin(state.rotor*8)*.007*state.workingStrength,0);
                float ratio=name.startsWith("tail_rotor")?4:state.variant.family==VehicleType.Family.DRONE || state.variant.family==VehicleType.Family.PLANE?3:name.equals("cooling_fan")?2:1;
                String directionName=name.startsWith("tip_rotor_")?name.substring(4):name;
                float angle=wheel?VehicleAnimation.wheelPhase(travel,part.wheelRadius()):name.equals("reel")?state.rotor:state.engineRotor*ratio*VehicleAnimation.rotorDirection(directionName);
                var pose=state.presentation;
                switch(def.axis()) {
                    case 'x' -> matrices.multiply(RotationAxis.POSITIVE_X.rotation(angle));
                    case 'y' -> matrices.multiply(RotationAxis.POSITIVE_Y.rotation(angle));
                    case 'z' -> matrices.multiply(RotationAxis.POSITIVE_Z.rotation(angle));
                    case 'u' -> { if(!outboard && !bikeFront) matrices.multiply(RotationAxis.POSITIVE_Y.rotation(-state.steering)); }
                    case 'c' -> matrices.multiply(RotationAxis.POSITIVE_Z.rotation(-state.steering*1.7f));
                    case 'l' -> matrices.multiply(RotationAxis.POSITIVE_X.rotation((float)VehiclePhysics.clamp(pose.throttle()-pose.steering(),-1,1)*.3f));
                    case 'r' -> matrices.multiply(RotationAxis.POSITIVE_X.rotation((float)VehiclePhysics.clamp(pose.throttle()+pose.steering(),-1,1)*.3f));
                    case 'e' -> matrices.multiply(RotationAxis.POSITIVE_X.rotation(name.startsWith("elevator")?pose.elevator():pose.steering()*(name.endsWith("_-1")?.28f:-.28f)));
                    case 'v' -> matrices.multiply(RotationAxis.POSITIVE_Y.rotation(pose.rudder()));
                    case 'n' -> matrices.multiply(RotationAxis.POSITIVE_Z.rotation(switch(name) { case "instrument_fuel" -> pose.fuelNeedle();case "instrument_speed" -> pose.speedNeedle();default -> pose.rpmNeedle(); }));
                    case 'a' -> matrices.multiply(RotationAxis.POSITIVE_X.rotation(pose.throttle()*.45f));
                    case 'k' -> matrices.multiply(RotationAxis.POSITIVE_X.rotation(-pose.rpm()*(.32f+.08f*(float)Math.sin(state.animationTime*2.4))));
                    case 'p' -> matrices.multiply(RotationAxis.POSITIVE_X.rotation((1-pose.parkingStand())*1.4f));
                    case 'g' -> matrices.multiply(RotationAxis.POSITIVE_X.rotation(pose.gimbalPitch()));
                    case 'j' -> { matrices.multiply(RotationAxis.POSITIVE_Z.rotation(pose.swashRoll()));matrices.multiply(RotationAxis.POSITIVE_X.rotation(pose.swashPitch())); }
                    default -> {}
                }
            }
            int tint=0xffffffff;
            if(def.material().equals("paint")) tint=0xff000000|(state.color==0?VehicleGeometry.paintColor(state.variant):PALETTE[Math.floorMod(state.color,16)]);
            float inverseDensity=1f/VehicleAtlas.DENSITY;matrices.scale(inverseDensity,inverseDensity,inverseDensity);
            queue.getBatchingQueue(0).submitModelPart(part.model(),matrices,RenderLayers.entityCutoutNoCull(part.texture()),state.light,OverlayTexture.DEFAULT_UV,null,tint,null);
            matrices.pop();
        }
        matrices.pop();
    }
}
