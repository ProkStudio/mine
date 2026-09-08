package com.prokstudio.militaryvehicles.client;
import com.prokstudio.militaryvehicles.core.*;
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
/** Per-entity differential tracks, crew-controlled turret/elevation and deployment, with cached meshes. */
public final class TruckRenderer extends EntityRenderer<TruckEntity,TruckRenderer.State> {
    public static final class State extends EntityRenderState { public float yaw,wheel,steer,turret,pitch,deployment,recoil;public double left,right; }
    private record Mesh(TruckGeometry.Part definition,ModelPart part,Identifier texture) {}
    private final List<Mesh> meshes=new ArrayList<>();
    private final VehicleKind kind;
    public TruckRenderer(EntityRendererFactory.Context context) { this(context,VehicleKind.TRUCK); }
    public TruckRenderer(EntityRendererFactory.Context context,VehicleKind kind) {
        super(context);this.kind=kind;shadowRadius=kind.width*.31f;
        for(var definition:VehicleGeometry.create(kind)) {
            ModelData data=new ModelData();ModelPartBuilder builder=ModelPartBuilder.create();
            for(var b:definition.boxes()) builder.uv(0,0).cuboid(b.x(),b.y(),b.z(),b.w(),b.h(),b.d());
            data.getRoot().addChild("mesh",builder,ModelTransform.origin(0,0,0));
            meshes.add(new Mesh(definition,TexturedModelData.of(data,128,128).createModel(),MilitaryContent.id("textures/item/material/"+definition.material()+".png")));
        }
    }
    @Override public State createRenderState() { return new State(); }
    @Override public void updateRenderState(TruckEntity entity,State state,float delta) {
        super.updateRenderState(entity,state,delta);state.yaw=entity.getLerpedYaw(delta);state.wheel=entity.wheelAngle(delta);state.steer=entity.steerAngle();
        state.left=entity.trackAngle(true,delta);state.right=entity.trackAngle(false,delta);state.turret=entity.turretYaw(delta);state.pitch=entity.gunPitch(delta);
        state.deployment=entity.deployment();state.recoil=entity.recoil();
    }
    @Override public void render(State state,MatrixStack matrices,OrderedRenderCommandQueue queue,CameraRenderState camera) {
        super.render(state,matrices,queue,camera);matrices.push();matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-state.yaw));
        for(Mesh mesh:meshes) {
            var d=mesh.definition();matrices.push();
            if(d.name().startsWith("turret_")&&!d.name().equals("turret_ring")||d.name().startsWith("gun_")) {
                matrices.translate(0,ExpansionGeometry.TURRET_Y/16.0,ExpansionGeometry.TURRET_Z/16.0);
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-state.turret));
                matrices.translate(0,-ExpansionGeometry.TURRET_Y/16.0,-ExpansionGeometry.TURRET_Z/16.0);
            }
            matrices.translate(d.x()/16.0,d.y()/16.0,d.z()/16.0);
            if(d.wheel()) {
                if(d.front()) matrices.multiply(RotationAxis.POSITIVE_Y.rotation(-state.steer));
                double angle=kind.tracked()?(d.x()>0?state.left:state.right):state.wheel;
                matrices.multiply(RotationAxis.POSITIVE_X.rotation((float)(angle%(Math.PI*2))));
            } else if(d.name().equals("steering")) matrices.multiply(RotationAxis.POSITIVE_Y.rotation(-state.steer*1.7f));
            else if(d.name().startsWith("tread_")) matrices.translate(0,0,TrackDrive.treadOffset(d.z(),d.x()>0?state.left:state.right,d.name().startsWith("tread_upper_"))/16);
            else if(d.name().startsWith("gun_")) {matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(state.pitch));matrices.translate(0,0,-state.recoil*2/16);}
            else if(d.name().startsWith("outrigger_leg_")) matrices.translate(0,-state.deployment*5/16,0);
            queue.getBatchingQueue(0).submitModelPart(mesh.part(),matrices,RenderLayers.entityCutoutNoCull(mesh.texture()),state.light,OverlayTexture.DEFAULT_UV,null,0xffffffff,null);
            matrices.pop();
        }
        matrices.pop();
    }
}
