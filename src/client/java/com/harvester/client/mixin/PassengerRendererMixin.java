package com.harvester.client.mixin;

import com.harvester.client.entity.renderer.VehiclePassengerState;
import com.harvester.entity.CombineEntity;
import com.harvester.vehicle.*;
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
    @Unique private final Map<PlayerEntity,PassengerAnimation> harvester$motions=new WeakHashMap<>();
    @Unique private final Map<PlayerEntity,PassengerAnimation.Sample> harvester$positions=new WeakHashMap<>();
    @Inject(method="getAndUpdateRenderState(Lnet/minecraft/entity/Entity;F)Lnet/minecraft/client/render/entity/state/EntityRenderState;",at=@At("RETURN"),require=1)
    private void harvester$seatFacing(Entity entity,float tickProgress,CallbackInfoReturnable<EntityRenderState> callback) {
        if(!(callback.getReturnValue() instanceof LivingEntityRenderState state)) return;
        VehiclePassengerState extra=(VehiclePassengerState)state;
        extra.harvester$setSeatPose(null);extra.harvester$setAnimatedPose(null);
        extra.harvester$setBodyPose(VehicleRig.Pose.ZERO);extra.harvester$setSteering(0);
        if(!(entity instanceof PlayerEntity player)) return;
        if(!(player.getVehicle() instanceof CombineEntity vehicle)) { harvester$clear(player);return; }
        int seat=vehicle.getPassengerList().indexOf(player);
        if(seat<0) { harvester$clear(player);return; }
        double now=player.age+tickProgress;long identity=((long)vehicle.getId()<<32)|(seat&0xffffffffL);
        var at=vehicle.getLerpedPos(tickProgress);double speed=0;
        var before=harvester$positions.get(player);
        if(before!=null && before.identity()==identity) {
            double dt=now-before.time(),distance=Math.hypot(at.x-before.x(),at.z-before.z());
            if(dt>0 && dt<=5 && distance<8) speed=VehicleAnimation.signedDistance(at.x-before.x(),at.z-before.z(),vehicle.getLerpedYaw(tickProgress))/dt;
            else if(dt==0) speed=before.speed();
        }
        harvester$positions.put(player,new PassengerAnimation.Sample(at.x,at.z,now,identity,speed));
        var motion=harvester$motions.computeIfAbsent(player,p->new PassengerAnimation()).update(new PassengerAnimation.Input(
            vehicle.variant(),seat,identity,now,vehicle.steeringInput(),vehicle.driveInput(),speed,vehicle.isWorking()));
        var vehicleRig=vehicle.bodyPose(tickProgress);
        var rig=new VehicleRig.Pose(vehicleRig.pitch()+motion.leanPitch()*motion.weight(),vehicleRig.roll()+motion.leanRoll()*motion.weight());
        float viewYaw=state.bodyYaw+state.relativeHeadYaw;
        var target=PassengerPose.facing(vehicle.getLerpedYaw(tickProgress),viewYaw,state.pitch-(float)Math.toDegrees(rig.pitch()));
        var facing=harvester$heads.computeIfAbsent(player,p->new PassengerPose.HeadTracker()).update(target,now,identity);
        state.bodyYaw=facing.bodyYaw();state.relativeHeadYaw=facing.headYaw();state.pitch=facing.headPitch();state.limbSwingAmplitude=0;
        extra.harvester$setSeatPose(PassengerPose.limbs(vehicle.variant(),seat));extra.harvester$setAnimatedPose(motion);
        extra.harvester$setBodyPose(rig);extra.harvester$setSteering(seat==0?vehicle.steeringInput():0);
        // No player yaw/pitch mutation, camera roll, hitbox change, or outgoing movement packet.
    }
    @Unique private void harvester$clear(PlayerEntity player) {
        harvester$heads.remove(player);harvester$motions.remove(player);harvester$positions.remove(player);
    }
}
