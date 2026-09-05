package com.harvester.client.mixin;

import com.harvester.client.entity.renderer.VehiclePassengerState;
import com.harvester.vehicle.PassengerPose;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.state.BipedEntityRenderState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Pose at model evaluation time, from the captured state, not from a global current rider. */
@Mixin(BipedEntityModel.class)
public abstract class PassengerBipedModelMixin {
    @Shadow @Final public ModelPart rightArm;
    @Shadow @Final public ModelPart leftArm;
    @Shadow @Final public ModelPart rightLeg;
    @Shadow @Final public ModelPart leftLeg;

    @Inject(method = "setAngles(Lnet/minecraft/client/render/entity/state/BipedEntityRenderState;)V",
            at = @At("TAIL"), require = 1)
    private void harvester$limbs(BipedEntityRenderState state, CallbackInfo callback) {
        PassengerPose.Limbs pose = ((VehiclePassengerState) state).harvester$getSeatPose();
        if (pose == null || !state.hasVehicle) return;
        rightLeg.pitch = leftLeg.pitch = pose.legPitch();
        rightLeg.yaw = pose.legSpread(); leftLeg.yaw = -pose.legSpread();
        rightLeg.roll = .05f; leftLeg.roll = -.05f;
        // Do not suppress eating, aiming, item use or attack animations while seated.
        if (PassengerPose.keepVanillaArms(state.isUsingItem, state.handSwingProgress)) return;
        rightArm.pitch = leftArm.pitch = pose.armPitch();
        rightArm.yaw = -pose.armInward(); leftArm.yaw = pose.armInward();
        rightArm.roll = leftArm.roll = 0;
    }
}
