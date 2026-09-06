package dev.mine.arsenal.client.mixin;

import dev.mine.arsenal.client.ArsenalPose;
import dev.mine.arsenal.core.*;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.state.BipedEntityRenderState;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BipedEntityModel.class)
public abstract class WeaponArmsMixin {
    @Shadow @Final public ModelPart rightArm;
    @Shadow @Final public ModelPart leftArm;
    @Shadow @Final public ModelPart head;
    @Inject(method="setAngles(Lnet/minecraft/client/render/entity/state/BipedEntityRenderState;)V",at=@At("TAIL"))
    private void arsenal$arms(BipedEntityRenderState state,CallbackInfo ci) {
        // Do not overwrite transport passenger poses or unrelated entity render states.
        if(!(state instanceof ArsenalPose pose)||pose.arsenal$weapon()==null||state.hasVehicle) return;
        Weapon w=pose.arsenal$weapon(); ModelPart main=pose.arsenal$left()?leftArm:rightArm, support=pose.arsenal$left()?rightArm:leftArm;
        float sign=pose.arsenal$left()?-1:1,reload=(float)Animation.magazineTravel(pose.arsenal$frame());
        main.pitch=-1.35f+head.pitch; main.yaw=head.yaw-sign*.15f; main.roll=0;
        if(w.style==Weapon.Style.RPG) main.pitch=-1.65f+head.pitch;
        if(!w.sidearm()||pose.arsenal$aim()||reload>0) {
            support.pitch=-1.25f+head.pitch+reload*.7f; support.yaw=head.yaw+sign*.5f; support.roll=sign*reload*.3f;
        }
    }
}
