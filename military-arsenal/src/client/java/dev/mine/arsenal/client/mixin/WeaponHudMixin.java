package dev.mine.arsenal.client.mixin;

import dev.mine.arsenal.client.ArsenalClient;
import net.minecraft.client.gui.hud.InGameHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public abstract class WeaponHudMixin {
    @Inject(method="renderCrosshair",at=@At("HEAD"),cancellable=true)
    private void arsenal$crosshair(CallbackInfo ci) { if(ArsenalClient.holdingGun()) ci.cancel(); }
}
