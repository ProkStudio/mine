package dev.mine.arsenal.client;

import dev.mine.arsenal.*;
import dev.mine.arsenal.core.*;
import net.minecraft.client.model.*;
import net.minecraft.client.render.*;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.*;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import java.util.*;

/** Flight uses the same original ammunition mesh as inventory, hand and dropped items. */
public final class ProjectileRenderer extends EntityRenderer<ArsenalProjectile,ProjectileRenderer.State> {
    public static final class State extends EntityRenderState { public Ammo ammo=Ammo.ROCKET_PRACTICE; public float yaw,pitch; }
    private record Part(ModelPart mesh,Identifier texture) {}
    private final Map<Ammo,List<Part>> meshes=new EnumMap<>(Ammo.class);
    public ProjectileRenderer(EntityRendererFactory.Context context) {
        super(context); shadowRadius=.12f;
        for(Ammo a:Ammo.values()) {
            List<Part> parts=new ArrayList<>();
            for(var box:WeaponGeometry.ammunition(a)) {
                ModelData data=new ModelData();
                data.getRoot().addChild("mesh",ModelPartBuilder.create().uv(0,0).cuboid((float)box.x()-8,(float)box.y()-8,(float)box.z()-8,(float)box.w(),(float)box.h(),(float)box.d()),ModelTransform.origin(0,0,0));
                parts.add(new Part(TexturedModelData.of(data,32,32).createModel(),Arsenal.id("textures/material/"+box.material()+".png")));
            }
            meshes.put(a,List.copyOf(parts));
        }
    }
    @Override public State createRenderState() { return new State(); }
    @Override public void updateRenderState(ArsenalProjectile entity,State state,float delta) {
        super.updateRenderState(entity,state,delta); state.ammo=Arsenal.ammo(entity.getStack()); state.yaw=entity.getYaw(); state.pitch=entity.getPitch();
    }
    @Override public void render(State state,MatrixStack matrices,OrderedRenderCommandQueue queue,CameraRenderState camera) {
        super.render(state,matrices,queue,camera); matrices.push();
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180-state.yaw)); matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-state.pitch));
        matrices.scale(.6f,.6f,.6f);
        for(Part part:meshes.get(state.ammo)) queue.getBatchingQueue(0).submitModelPart(part.mesh,matrices,RenderLayers.entityCutoutNoCull(part.texture),state.light,OverlayTexture.DEFAULT_UV,null,0xffffffff,null);
        matrices.pop();
    }
}
