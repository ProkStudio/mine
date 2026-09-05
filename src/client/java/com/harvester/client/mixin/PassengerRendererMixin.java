package com.harvester.client.mixin;

import com.harvester.client.entity.renderer.VehiclePassengerState;
import com.harvester.entity.CombineEntity;
import com.harvester.vehicle.PassengerPose;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Runs AFTER the complete virtual updateRenderState, including player-specific overrides. */
@Mixin(EntityRenderer.class)
public abstract class PassengerRendererMixin {
    @Inject(method = "getAndUpdateRenderState(Lnet/minecraft/entity/Entity;F)Lnet/minecraft/client/render/entity/state/EntityRenderState;",
            at = @At("RETURN"), require = 1)
    private void harvester$seatFacing(Entity entity, float tickProgress,
                                     CallbackInfoReturnable<EntityRenderState> callback) {
        EntityRenderState result = callback.getReturnValue();
        if (!(result instanceof LivingEntityRenderState state)) return;
        VehiclePassengerState extra = (VehiclePassengerState) state;
        // Render states can be reused: explicitly clear on every non-vehicle update/dismount.
        extra.harvester$setSeatPose(null);
        if (!(entity instanceof PlayerEntity player)
                || !(player.getVehicle() instanceof CombineEntity vehicle)) return;
        int seat = vehicle.getPassengerList().indexOf(player);
        if (seat < 0) return;
        // Capture the vanilla interpolated head direction before replacing only visual body yaw.
        float viewYaw = state.bodyYaw + state.relativeHeadYaw;
        var facing = PassengerPose.facing(vehicle.getLerpedYaw(tickProgress), viewYaw, state.pitch);
        state.bodyYaw = facing.bodyYaw();
        state.relativeHeadYaw = facing.headYaw();
        state.pitch = facing.headPitch();
        state.limbSwingAmplitude = 0;
        extra.harvester$setSeatPose(PassengerPose.limbs(vehicle.variant(), seat));
        // Deliberately no player.setYaw/setPitch/setBodyYaw, no camera or movement packet writes.
    }
}
