package com.prokstudio.militaryvehicles.client;
import com.prokstudio.militaryvehicles.core.TruckGeometry;
import com.prokstudio.militaryvehicles.entity.TruckEntity;
import com.prokstudio.militaryvehicles.init.MilitaryContent;
import net.minecraft.client.model.*;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.*;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import java.util.*;
/** Cached immutable meshes; animation values belong to one entity, not global state. */
public final class TruckRenderer extends EntityRenderer<TruckEntity,TruckRenderer.State> {
    public static final class State extends EntityRenderState { public float yaw,wheel,steer; }
    private record Mesh(TruckGeometry.Part definition,ModelPart part,Identifier texture) {}
    private final List<Mesh> meshes=new ArrayList<>();
    public TruckRenderer(EntityRendererFactory.Context context) {
        super(context);shadowRadius=1.6f;
        for(var definition:TruckGeometry.create()) {
            ModelData data=new ModelData();ModelPartBuilder builder=ModelPartBuilder.create();
            for(var b:definition.boxes()) builder.uv(0,0).cuboid(b.x(),b.y(),b.z(),b.w(),b.h(),b.d());
            data.getRoot().addChild("mesh",builder,ModelTransform.origin(0,0,0));
            meshes.add(new Mesh(definition,TexturedModelData.of(data,128,128).createModel(),MilitaryContent.id("textures/item/material/"+definition.material()+".png")));
        }
    }
    @Override public State createRenderState() { return new State(); }
    @Override public void updateRenderState(TruckEntity entity,State state,float delta) {
        super.updateRenderState(entity,state,delta);state.yaw=entity.getLerpedYaw(delta);state.wheel=entity.wheelAngle(delta);state.steer=entity.steerAngle();
    }
    @Override public void render(State state,MatrixStack matrices,OrderedRenderCommandQueue queue,CameraRenderState camera) {
        super.render(state,matrices,queue,camera);matrices.push();matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-state.yaw));
        for(Mesh mesh:meshes) {
            var d=mesh.definition();matrices.push();matrices.translate(d.x()/16.0,d.y()/16.0,d.z()/16.0);
            if(d.wheel()) { if(d.front()) matrices.multiply(RotationAxis.POSITIVE_Y.rotation(-state.steer));matrices.multiply(RotationAxis.POSITIVE_X.rotation(state.wheel)); }
            else if(d.name().equals("steering")) matrices.multiply(RotationAxis.POSITIVE_Y.rotation(-state.steer*1.7f));
            queue.getBatchingQueue(0).submitModelPart(mesh.part(),matrices,RenderLayers.entityCutoutNoCull(mesh.texture()),state.light,OverlayTexture.DEFAULT_UV,null,0xffffffff,null);
            matrices.pop();
        }
        matrices.pop();
    }
}
