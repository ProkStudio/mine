package com.harvester.client.entity.renderer;

import com.harvester.entity.CombineEntity;
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

/** Original meshes and harvester-owned textures. No block-state or vanilla item rendering. */
public class CombineRenderer extends EntityRenderer<CombineEntity,CombineRenderState> {
    private record BakedPart(VehicleGeometry.Part definition,ModelPart model,Identifier texture) {}
    private final Map<VehicleType,List<BakedPart>> models=new EnumMap<>(VehicleType.class);
    private static final int[] PALETTE={0xffffff,0xe0e1dc,0xc78643,0xa65b95,0x81abc0,0xc7b752,0xc98994,0x60696c,
        0xb0b8b9,0x438e93,0x785d9a,0x466c9c,0x826650,0x507246,0xac5145,0x353c40};

    public CombineRenderer(EntityRendererFactory.Context context) {
        super(context); shadowRadius=1;
        for(VehicleType type:VehicleType.values()) {
            List<BakedPart> parts=new ArrayList<>();
            for(VehicleGeometry.Part p:VehicleGeometry.create(type)) {
                ModelData data=new ModelData();
                ModelPartBuilder builder=ModelPartBuilder.create();
                for(var box:p.boxes()) builder.uv(0,0).cuboid(box.x(),box.y(),box.z(),box.w(),box.h(),box.d());
                data.getRoot().addChild("mesh",builder,ModelTransform.origin(0,0,0));
                ModelPart mesh=TexturedModelData.of(data,256,256).createModel();
                parts.add(new BakedPart(p,mesh,Identifier.of("harvester","textures/vehicle/"+p.material()+".png")));
            }
            models.put(type,List.copyOf(parts));
        }
    }
    @Override public CombineRenderState createRenderState() { return new CombineRenderState(); }
    @Override public void updateRenderState(CombineEntity entity,CombineRenderState state,float tickProgress) {
        super.updateRenderState(entity,state,tickProgress);
        state.variant=entity.variant(); state.color=entity.getColor(); state.yaw=entity.getLerpedYaw(tickProgress);
        state.wheels=entity.wheelAngle(); state.rotor=entity.rotorAngle(); state.harvesting=entity.isHarvesting(); state.headerEnabled=entity.isHeaderEnabled();
    }
    @Override public void render(CombineRenderState state,MatrixStack matrices,OrderedRenderCommandQueue queue,CameraRenderState camera) {
        super.render(state,matrices,queue,camera);
        matrices.push();
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-state.yaw));
        for(BakedPart part:models.get(state.variant)) {
            VehicleGeometry.Part def=part.definition();
            matrices.push();
            double raised=!state.headerEnabled && (def.name().startsWith("header") || def.name().equals("reel"))?4:0;
            matrices.translate(def.px()/16.0,(def.py()+raised)/16.0,def.pz()/16.0);
            float angle=def.name().startsWith("wheel")?state.wheels:state.rotor;
            if(def.axis()=='x') matrices.multiply(RotationAxis.POSITIVE_X.rotation(angle));
            if(def.axis()=='y') matrices.multiply(RotationAxis.POSITIVE_Y.rotation(angle));
            if(def.axis()=='z') matrices.multiply(RotationAxis.POSITIVE_Z.rotation(angle));
            int tint=0xffffffff;
            if(def.material().equals("paint")) tint=0xff000000|(state.color==0?VehicleGeometry.paintColor(state.variant):PALETTE[Math.floorMod(state.color,16)]);
            queue.getBatchingQueue(0).submitModelPart(part.model(),matrices,RenderLayers.entityCutoutNoCull(part.texture()),
                    state.light,OverlayTexture.DEFAULT_UV,null,tint,null);
            matrices.pop();
        }
        matrices.pop();
    }
}
