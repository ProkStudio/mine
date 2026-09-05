package com.harvester.client.entity.renderer;

import com.harvester.entity.CombineEntity;
import com.harvester.vehicle.VehicleAnimation;
import com.harvester.vehicle.VehicleGeometry;
import com.harvester.vehicle.VehicleType;
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

/** Immutable baked meshes, per-entity visual histories and value-only queued render states. */
public class CombineRenderer extends EntityRenderer<CombineEntity,CombineRenderState> {
    private record BakedPart(VehicleGeometry.Part definition,ModelPart model,Identifier texture,double wheelRadius) {}
    private final Map<VehicleType,List<BakedPart>> models=new EnumMap<>(VehicleType.class);
    // Values contain no entity/world reference; removed vehicles can be garbage-collected.
    private final Map<CombineEntity,VehicleAnimation> animations=new WeakHashMap<>();
    private static final int[] PALETTE={0xffffff,0xe0e1dc,0xc78643,0xa65b95,0x81abc0,0xc7b752,0xc98994,0x60696c,
        0xb0b8b9,0x438e93,0x785d9a,0x466c9c,0x826650,0x507246,0xac5145,0x353c40};

    public CombineRenderer(EntityRendererFactory.Context context) {
        super(context); shadowRadius=1;
        for(VehicleType type:VehicleType.values()) {
            List<BakedPart> parts=new ArrayList<>();
            List<VehicleGeometry.Part> definitions=VehicleGeometry.create(type);
            for(VehicleGeometry.Part p:definitions) {
                ModelData data=new ModelData();
                ModelPartBuilder builder=ModelPartBuilder.create();
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
        state.wheels=entity.wheelAngle(); state.rotor=entity.rotorAngle(); state.harvesting=entity.isHarvesting(); state.headerEnabled=entity.isHeaderEnabled();
        VehicleAnimation.Frame frame=animations.computeIfAbsent(entity,e->new VehicleAnimation()).update(
            entity.age+tickProgress,entity.getX(),entity.getZ(),state.yaw,state.harvesting,state.headerEnabled,state.variant.family);
        state.wheelTravel=frame.wheelTravel(); state.engineRotor=frame.engineRotor();
        state.headerLift=frame.headerLift(); state.steering=frame.steering();
    }
    @Override public void render(CombineRenderState state,MatrixStack matrices,OrderedRenderCommandQueue queue,CameraRenderState camera) {
        super.render(state,matrices,queue,camera);
        matrices.push();
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-state.yaw));
        for(BakedPart part:models.get(state.variant)) {
            VehicleGeometry.Part def=part.definition();
            matrices.push();
            boolean header=def.name().startsWith("header") || def.name().equals("reel");
            matrices.translate(def.px()/16.0,(def.py()+(header?state.headerLift:0))/16.0,def.pz()/16.0);
            boolean wheel=def.name().startsWith("wheel");
            boolean frontWheel=def.name().startsWith("wheel_front") || def.name().startsWith("wheel_hub_front");
            if(frontWheel) matrices.multiply(RotationAxis.POSITIVE_Y.rotation(-state.steering));
            // Reel still uses the existing work signal; explicit actual-working tracking is a remaining task.
            float angle=wheel?VehicleAnimation.wheelPhase(state.wheelTravel,part.wheelRadius()):
                def.name().equals("reel")?state.rotor:state.engineRotor*VehicleAnimation.rotorDirection(def.name());
            if(def.axis()=='x') matrices.multiply(RotationAxis.POSITIVE_X.rotation(angle));
            if(def.axis()=='y') matrices.multiply(RotationAxis.POSITIVE_Y.rotation(angle));
            if(def.axis()=='z') matrices.multiply(RotationAxis.POSITIVE_Z.rotation(angle));
            int tint=0xffffffff;
            if(def.material().equals("paint")) tint=0xff000000|(state.color==0?VehicleGeometry.paintColor(state.variant):PALETTE[Math.floorMod(state.color,16)]);
            // Transforms are queue-captured matrices. Never mutate the shared baked ModelPart's pose here.
            queue.getBatchingQueue(0).submitModelPart(part.model(),matrices,RenderLayers.entityCutoutNoCull(part.texture()),
                    state.light,OverlayTexture.DEFAULT_UV,null,tint,null);
            matrices.pop();
        }
        matrices.pop();
    }
}
