package dev.mine.arsenal.client.mixin;

import dev.mine.arsenal.client.ArsenalClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public abstract class WeaponFovMixin {
    @Inject(method="getFov",at=@At("RETURN"),cancellable=true)
    private void arsenal$zoom(Camera camera,float delta,boolean worldFov,CallbackInfoReturnable<Float> ci) {
        if(worldFov) ci.setReturnValue(ci.getReturnValue()/ArsenalClient.zoom(delta));
    }
}
