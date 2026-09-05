package com.harvester.client.mixin;

import com.harvester.client.entity.renderer.VehiclePassengerState;
import com.harvester.vehicle.VehicleRig;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Rotate the whole rendered rider (including equipment) around the hip, never the camera. */
@Mixin(LivingEntityRenderer.class)
public abstract class PassengerBodyTransformMixin {
    @Inject(method="setupTransforms(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;FF)V",at=@At("TAIL"),require=1)
    private void harvester$lean(LivingEntityRenderState state,MatrixStack matrices,float bodyYaw,float baseHeight,CallbackInfo callback) {
        VehiclePassengerState extra=(VehiclePassengerState)state;
        if(extra.harvester$getSeatPose()==null) return;
        var pose=extra.harvester$getBodyPose();
        // Living renderer already applies baseScale; vanilla player scaling happens later.
        matrices.translate(0,VehicleRig.PLAYER_HIP,0);
        // Player local X/Z are reversed by vanilla's 180-bodyYaw orientation.
        matrices.multiply(RotationAxis.POSITIVE_Z.rotation(-pose.roll()));
        matrices.multiply(RotationAxis.POSITIVE_X.rotation(-pose.pitch()));
        matrices.translate(0,-VehicleRig.PLAYER_HIP,0);
    }
}
