package com.harvester.client.mixin;

import com.harvester.client.entity.renderer.VehiclePassengerState;
import com.harvester.vehicle.PassengerPose;
import com.harvester.vehicle.PassengerAnimation;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.state.BipedEntityRenderState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BipedEntityModel.class)
public abstract class PassengerBipedModelMixin {
    @Shadow @Final public ModelPart rightArm;
    @Shadow @Final public ModelPart leftArm;
    @Shadow @Final public ModelPart rightLeg;
    @Shadow @Final public ModelPart leftLeg;
    @Unique private static void harvester$apply(ModelPart part,PassengerAnimation.Joint joint,float weight) {
        part.pitch=PassengerAnimation.blend(part.pitch,joint.pitch(),weight);
        part.yaw=PassengerAnimation.blend(part.yaw,joint.yaw(),weight);
        part.roll=PassengerAnimation.blend(part.roll,joint.roll(),weight);
    }
    @Inject(method="setAngles(Lnet/minecraft/client/render/entity/state/BipedEntityRenderState;)V",at=@At("TAIL"),require=1)
    private void harvester$limbs(BipedEntityRenderState state,CallbackInfo callback) {
        var extra=(VehiclePassengerState)state;var pose=extra.harvester$getAnimatedPose();
        if(pose==null || !state.hasVehicle || extra.harvester$getSeatPose()==null) return;
        harvester$apply(rightLeg,pose.rightLeg(),pose.weight());
        harvester$apply(leftLeg,pose.leftLeg(),pose.weight());
        if(PassengerPose.keepVanillaArms(state.isUsingItem,state.handSwingProgress)) return;
        harvester$apply(rightArm,pose.rightArm(),pose.weight());
        harvester$apply(leftArm,pose.leftArm(),pose.weight());
        // PlayerEntityModel copies these transforms to sleeves/trousers after super.setAngles.
    }
}
