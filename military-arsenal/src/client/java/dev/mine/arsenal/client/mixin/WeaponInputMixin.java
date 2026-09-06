package dev.mine.arsenal.client.mixin;

import dev.mine.arsenal.client.ArsenalClient;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(MinecraftClient.class)
public abstract class WeaponInputMixin {
    @Inject(method="doAttack",at=@At("HEAD"),cancellable=true)
    private void arsenal$fire(CallbackInfoReturnable<Boolean> ci) {
        if(ArsenalClient.ownsMouse()) { ArsenalClient.fireTap(); ci.setReturnValue(false); }
    }
    @Inject(method="handleBlockBreaking",at=@At("HEAD"),cancellable=true)
    private void arsenal$noMining(boolean breaking,CallbackInfo ci) { if(ArsenalClient.ownsMouse()) ci.cancel(); }
    @Inject(method="doItemUse",at=@At("HEAD"),cancellable=true)
    private void arsenal$aim(CallbackInfo ci) { if(ArsenalClient.ownsMouse()) ci.cancel(); }
}
