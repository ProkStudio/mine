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

@Mixin(BipedEntityModel.class)
public abstract class PassengerBipedModelMixin {
    @Shadow @Final public ModelPart rightArm;
    @Shadow @Final public ModelPart leftArm;
    @Shadow @Final public ModelPart rightLeg;
    @Shadow @Final public ModelPart leftLeg;
    @Inject(method="setAngles(Lnet/minecraft/client/render/entity/state/BipedEntityRenderState;)V",at=@At("TAIL"),require=1)
    private void harvester$limbs(BipedEntityRenderState state,CallbackInfo callback) {
        var extra=(VehiclePassengerState)state; var pose=extra.harvester$getSeatPose();
        if(pose==null || !state.hasVehicle) return;
        rightLeg.pitch=leftLeg.pitch=pose.legPitch(); rightLeg.yaw=pose.legSpread(); leftLeg.yaw=-pose.legSpread();
        rightLeg.roll=.05f; leftLeg.roll=-.05f;
        if(PassengerPose.keepVanillaArms(state.isUsingItem,state.handSwingProgress)) return;
        float steer=extra.harvester$getSteering();
        rightArm.pitch=pose.armPitch()+steer*.12f; leftArm.pitch=pose.armPitch()-steer*.12f;
        rightArm.yaw=-pose.armInward()+steer*.08f; leftArm.yaw=pose.armInward()+steer*.08f;
        rightArm.roll=leftArm.roll=0;
    }
}
