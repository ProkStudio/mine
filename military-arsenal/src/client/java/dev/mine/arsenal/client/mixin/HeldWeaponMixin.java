package dev.mine.arsenal.client.mixin;

import dev.mine.arsenal.GunItem;
import dev.mine.arsenal.client.ArsenalClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(HeldItemRenderer.class)
public abstract class HeldWeaponMixin {
    @Inject(method="renderFirstPersonItem",at=@At("HEAD"))
    private void arsenal$begin(AbstractClientPlayerEntity player,float delta,float pitch,Hand hand,float swing,ItemStack stack,float equip,MatrixStack matrices,OrderedRenderCommandQueue queue,int light,CallbackInfo ci) {
        if(hand==Hand.MAIN_HAND&&stack.getItem() instanceof GunItem) { matrices.push(); ArsenalClient.transform(matrices,delta,stack); }
    }
    @Inject(method="renderFirstPersonItem",at=@At("RETURN"))
    private void arsenal$end(AbstractClientPlayerEntity player,float delta,float pitch,Hand hand,float swing,ItemStack stack,float equip,MatrixStack matrices,OrderedRenderCommandQueue queue,int light,CallbackInfo ci) {
        if(hand==Hand.MAIN_HAND&&stack.getItem() instanceof GunItem) matrices.pop();
    }
    @Inject(method="shouldSkipHandAnimationOnSwap",at=@At("HEAD"),cancellable=true)
    private void arsenal$noComponentBobbing(ItemStack from,ItemStack to,CallbackInfoReturnable<Boolean> ci) {
        if(from.getItem() instanceof GunItem&&from.isOf(to.getItem())) ci.setReturnValue(true);
    }
}
