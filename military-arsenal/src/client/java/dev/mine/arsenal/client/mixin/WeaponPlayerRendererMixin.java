package dev.mine.arsenal.client.mixin;

import dev.mine.arsenal.GunItem;
import dev.mine.arsenal.client.ArsenalPose;
import net.minecraft.entity.LivingEntity;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.CustomModelDataComponent;
import net.minecraft.util.Arm;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public abstract class WeaponPlayerRendererMixin {
    @Inject(method="updateRenderState(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;F)V",at=@At("TAIL"))
    private void arsenal$state(LivingEntity player,LivingEntityRenderState state,float delta,CallbackInfo ci) {
        if(!(state instanceof ArsenalPose extra)) return;
        var stack=player.getMainHandStack();
        Float frame=stack.getOrDefault(DataComponentTypes.CUSTOM_MODEL_DATA,CustomModelDataComponent.DEFAULT).getFloat(0);
        extra.arsenal$set(stack.getItem() instanceof GunItem gun?gun.weapon:null,player.getMainArm()==Arm.LEFT,GunItem.aiming(stack),frame==null?0:frame.intValue());
    }
}
