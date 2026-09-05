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

/** Baked immutable meshes; matrices and value snapshots are captured by the deferred render queue. */
public class CombineRenderer extends EntityRenderer<CombineEntity,CombineRenderState> {
    private record BakedPart(VehicleGeometry.Part definition,ModelPart model,Identifier texture,double wheelRadius) {}
    private static final class History {
        final VehicleAnimation animation=new VehicleAnimation();
        final Map<String,Double> targets=new HashMap<>();
        final Map<String,Float> springs=new HashMap<>();
        int sampleAge=-1; double time; VehicleType type;
    }
    private final Map<VehicleType,List<BakedPart>> models=new EnumMap<>(VehicleType.class);
    private final Map<CombineEntity,History> histories=new WeakHashMap<>();
    private static final int[] PALETTE={0xffffff,0xe0e1dc,0xc78643,0xa65b95,0x81abc0,0xc7b752,0xc98994,0x60696c,0xb0b8b9,0x438e93,0x785d9a,0x466c9c,0x826650,0x507246,0xac5145,0x353c40};
    public CombineRenderer(EntityRendererFactory.Context context) {
        super(context); shadowRadius=1;
        for(VehicleType type:VehicleType.values()) {
            List<BakedPart> parts=new ArrayList<>(); List<VehicleGeometry.Part> definitions=VehicleGeometry.create(type);
            for(var p:definitions) {
                ModelData data=new ModelData(); ModelPartBuilder builder=ModelPartBuilder.create();
                for(var box:p.boxes()) builder.uv(0,0).cuboid(box.x(),box.y(),box.z(),box.w(),box.h(),box.d());
                data.getRoot().addChild("mesh",builder,ModelTransform.origin(0,0,0));
                ModelPart mesh=TexturedModelData.of(data,256,256).createModel();
                double radius=p.name().startsWith("wheel")?VehicleAnimation.wheelRadius(p,definitions):1;
                parts.add(new BakedPart(p,mesh,Identifier.of("harvester","textures/vehicle/"+p.material()+".png"),radius));
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
        History h=histories.computeIfAbsent(entity,e->new History());
        var f=h.animation.update(state.animationTime,entity.getX(),entity.getZ(),state.yaw,state.engineActive,state.headerEnabled,state.variant.family,state.inputSteer,state.inputDrive);
        var mechanisms=h.animation.mechanisms(state.animationTime,state.yaw,state.harvesting);
        state.wheelTravel=f.wheelTravel(); state.engineRotor=f.engineRotor(); state.headerLift=f.headerLift(); state.steering=f.steering();
        state.rotor=mechanisms.workAngle(); state.workingStrength=mechanisms.workingStrength(); state.yawTravel=mechanisms.yawTravel();
        if(h.type!=state.variant) { h.targets.clear(); h.springs.clear(); h.sampleAge=-1; h.type=state.variant; }
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
        h.targets.forEach((name,target)->h.springs.put(name,(float)VehicleAnimation.smooth(h.springs.getOrDefault(name,0f),target,.4,dt)));
        state.suspension=Map.copyOf(h.springs);
    }
    private static float trackSpring(CombineRenderState s,String side) {
        double total=0; int count=0;
        for(var e:s.suspension.entrySet()) if(e.getKey().startsWith("wheel_track"+side+"_")) { total+=e.getValue(); count++; }
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
            var def=part.definition(); String name=def.name(); matrices.push();
            boolean header=name.startsWith("header") || name.equals("reel");
            boolean wheel=name.startsWith("wheel");
            boolean frontWheel=name.startsWith("wheel_front") || name.startsWith("wheel_hub_front");
            double travel=state.wheelTravel;
            if(state.variant.family==VehicleType.Family.DOZER) travel+=state.yawTravel*def.px()/16;
            boolean outboard=state.variant.family==VehicleType.Family.BOAT && (name.equals("outboard") || name.equals("propeller"));
            if(outboard) {
                double length=state.variant==VehicleType.BOAT_CARGO?40:34;
                matrices.translate(0,5/16.0,(-length/2-1)/16);
                matrices.multiply(RotationAxis.POSITIVE_Y.rotation(-state.steering));
                matrices.translate(0,-5/16.0,(length/2+1)/16);
            }
            if(def.axis()=='t') {
                String[] fields=name.split("_"); int index=Integer.parseInt(fields[2]);
                var at=VehicleGeometry.trackPoint(index*VehicleGeometry.TRACK_PERIMETER/32+travel*16);
                matrices.translate(def.px()/16.0,at.y()/16+trackSpring(state,fields[1]),at.z()/16);
                matrices.multiply(RotationAxis.POSITIVE_X.rotation((float)at.angle()));
            } else {
                double spring=wheel?state.suspension.getOrDefault(name.replace("wheel_hub_","wheel_"),0f):0;
                matrices.translate(def.px()/16.0,(def.py()+(header?state.headerLift:0))/16.0+spring,def.pz()/16.0);
                if(frontWheel) matrices.multiply(RotationAxis.POSITIVE_Y.rotation(-state.steering));
                if(name.equals("header_teeth")) matrices.translate(Math.sin(state.rotor*6)*.025*state.workingStrength,0,0);
                if(name.equals("working_blade")) matrices.translate(0,Math.sin(state.rotor*8)*.007*state.workingStrength,0);
                if(name.equals("dark") && state.engineActive) matrices.translate(0,Math.sin(state.animationTime*3.1)*.002,0);
                float ratio=name.equals("tail_rotor")?4:state.variant.family==VehicleType.Family.DRONE?3:state.variant.family==VehicleType.Family.PLANE?3:1;
                float angle=wheel?VehicleAnimation.wheelPhase(travel,part.wheelRadius()):name.equals("reel")?state.rotor:state.engineRotor*ratio*VehicleAnimation.rotorDirection(name);
                switch(def.axis()) {
                    case 'x' -> matrices.multiply(RotationAxis.POSITIVE_X.rotation(angle));
                    case 'y' -> matrices.multiply(RotationAxis.POSITIVE_Y.rotation(angle));
                    case 'z' -> matrices.multiply(RotationAxis.POSITIVE_Z.rotation(angle));
                    case 'u' -> { if(!outboard) matrices.multiply(RotationAxis.POSITIVE_Y.rotation(-state.steering)); }
                    case 'c' -> matrices.multiply(RotationAxis.POSITIVE_Z.rotation(-state.steering*1.7f));
                    case 'l' -> matrices.multiply(RotationAxis.POSITIVE_X.rotation((float)VehiclePhysics.clamp(state.inputDrive-state.inputSteer,-1,1)*.3f));
                    case 'r' -> matrices.multiply(RotationAxis.POSITIVE_X.rotation((float)VehiclePhysics.clamp(state.inputDrive+state.inputSteer,-1,1)*.3f));
                    case 'e' -> {
                        float control=name.equals("elevator")?-state.controlPitch*.4f:state.inputSteer*(name.endsWith("_-1")?.28f:-.28f);
                        matrices.multiply(RotationAxis.POSITIVE_X.rotation(control));
                    }
                    case 'v' -> matrices.multiply(RotationAxis.POSITIVE_Y.rotation(-state.inputSteer*.35f));
                    default -> {}
                }
            }
            int tint=0xffffffff;
            if(def.material().equals("paint")) tint=0xff000000|(state.color==0?VehicleGeometry.paintColor(state.variant):PALETTE[Math.floorMod(state.color,16)]);
            queue.getBatchingQueue(0).submitModelPart(part.model(),matrices,RenderLayers.entityCutoutNoCull(part.texture()),state.light,OverlayTexture.DEFAULT_UV,null,tint,null);
            matrices.pop();
        }
        matrices.pop();
    }
}
