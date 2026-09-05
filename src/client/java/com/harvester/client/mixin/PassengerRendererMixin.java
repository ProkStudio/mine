package com.harvester.client.mixin;

import com.harvester.client.entity.renderer.VehiclePassengerState;
import com.harvester.entity.CombineEntity;
import com.harvester.vehicle.PassengerPose;
import com.harvester.vehicle.VehicleRig;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.Map;
import java.util.WeakHashMap;

@Mixin(EntityRenderer.class)
public abstract class PassengerRendererMixin {
    @Unique private final Map<PlayerEntity,PassengerPose.HeadTracker> harvester$heads=new WeakHashMap<>();
    @Inject(method="getAndUpdateRenderState(Lnet/minecraft/entity/Entity;F)Lnet/minecraft/client/render/entity/state/EntityRenderState;",at=@At("RETURN"),require=1)
    private void harvester$seatFacing(Entity entity,float tickProgress,CallbackInfoReturnable<EntityRenderState> callback) {
        if(!(callback.getReturnValue() instanceof LivingEntityRenderState state)) return;
        VehiclePassengerState extra=(VehiclePassengerState)state;
        extra.harvester$setSeatPose(null); extra.harvester$setBodyPose(VehicleRig.Pose.ZERO); extra.harvester$setSteering(0);
        if(!(entity instanceof PlayerEntity player)) return;
        if(!(player.getVehicle() instanceof CombineEntity vehicle)) { harvester$heads.remove(player); return; }
        int seat=vehicle.getPassengerList().indexOf(player);
        if(seat<0) { harvester$heads.remove(player); return; }
        var rig=vehicle.bodyPose(tickProgress);
        float viewYaw=state.bodyYaw+state.relativeHeadYaw;
        // Head pitch is relative to the tilted torso, not an extra copy of aircraft pitch.
        var target=PassengerPose.facing(vehicle.getLerpedYaw(tickProgress),viewYaw,state.pitch-(float)Math.toDegrees(rig.pitch()));
        long identity=((long)vehicle.getId()<<32)|(seat&0xffffffffL);
        var facing=harvester$heads.computeIfAbsent(player,p->new PassengerPose.HeadTracker()).update(target,player.age+tickProgress,identity);
        state.bodyYaw=facing.bodyYaw(); state.relativeHeadYaw=facing.headYaw(); state.pitch=facing.headPitch(); state.limbSwingAmplitude=0;
        extra.harvester$setSeatPose(PassengerPose.limbs(vehicle.variant(),seat)); extra.harvester$setBodyPose(rig);
        extra.harvester$setSteering(seat==0?vehicle.steeringInput():0);
        // No player rotation, camera roll, hitbox changes or outgoing movement overrides.
    }
}
