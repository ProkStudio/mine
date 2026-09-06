package dev.mine.arsenal;

import dev.mine.arsenal.core.*;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.*;
import net.minecraft.entity.damage.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.*;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.hit.*;
import net.minecraft.util.math.*;
import net.minecraft.world.RaycastContext;
import java.util.*;

public final class Combat {
    private record Smoke(ServerWorld world,Vec3d pos,int life) {}
    private static final List<Smoke> SMOKE=new ArrayList<>();
    private Combat() {}
    public static void clear() { SMOKE.clear(); }
    private static Vec3d origin(Entity e) { return new Vec3d(e.getX(),e.getEyeY(),e.getZ()); }
    public static boolean fire(ServerPlayerEntity p,Weapon w,Ammo ammo,boolean aim) {
        ServerWorld world=(ServerWorld)p.getEntityWorld();
        Vec3d start=origin(p),look=p.getRotationVec(1);
        double spread=Math.toRadians(w.spread*(aim?.24:1)+ammo.spread*(aim?.85:1));
        if(ammo.projectile()) {
            ItemStack stack=new ItemStack(Arsenal.AMMO.get(ammo)); NbtCompound n=new NbtCompound();
            n.putDouble("ArsenalDamageScale",w.damageScale); stack.set(DataComponentTypes.CUSTOM_DATA,NbtComponent.of(n));
            ArsenalProjectile projectile=new ArsenalProjectile(Arsenal.PROJECTILE,world);
            projectile.setOwner(p); projectile.setItem(stack);
            // Start on the eye ray. The first swept segment catches even point-blank walls.
            projectile.setPosition(start.x,start.y-.1,start.z);
            Vec3d direction=jitter(look,spread,world.getRandom());
            projectile.setVelocity(direction.x,direction.y,direction.z,(float)ammo.velocity,0);
            return world.spawnEntity(projectile);
        }
        Map<Entity,Double> damage=new HashMap<>();
        Vec3d last=start;
        for(int pellet=0;pellet<ammo.pellets;pellet++) {
            Vec3d direction=jitter(look,spread,world.getRandom());
            Vec3d end=start.add(direction.multiply(w.range));
            var block=world.raycast(new RaycastContext(start,end,RaycastContext.ShapeType.COLLIDER,RaycastContext.FluidHandling.NONE,p));
            Vec3d stop=block.getPos(); Entity target=null; double nearest=start.squaredDistanceTo(stop);
            for(Entity candidate:world.getOtherEntities(p,new Box(start,stop).expand(1),e->e.canHit()&&!e.isSpectator()&&!(e instanceof ArsenalProjectile))) {
                // Friendly entities still stop the ray; they are filtered at damage time.
                Optional<Vec3d> hit=candidate.getBoundingBox().expand(.05).raycast(start,stop);
                if(hit.isPresent() && start.squaredDistanceTo(hit.get())<nearest) {
                    target=candidate; stop=hit.get(); nearest=start.squaredDistanceTo(stop);
                }
            }
            if(target!=null) damage.merge(target,ammo.damage*w.damageScale*Animation.falloff(Math.sqrt(nearest),w.range),Double::sum);
            if(pellet<2) trace(world,start.add(look.multiply(.5)),stop);
            last=stop;
        }
        boolean hit=false;
        // Aggregate pellets once per entity: vanilla hurt immunity cannot eat the remaining pellets.
        for(var entry:damage.entrySet()) hit|=damage(world,p,p,entry.getKey(),entry.getValue(),"kinetic");
        if(hit) hit(p);
        world.spawnParticles(ParticleTypes.CRIT,last.x,last.y,last.z,4,.05,.05,.05,.025);
        return true;
    }
    private static Vec3d jitter(Vec3d look,double spread,net.minecraft.util.math.random.Random random) {
        Vec3d right=look.crossProduct(new Vec3d(0,1,0));
        if(right.lengthSquared()<1e-8) right=new Vec3d(1,0,0); else right=right.normalize();
        Vec3d up=right.crossProduct(look).normalize();
        double angle=random.nextDouble()*Math.PI*2, radius=Math.sqrt(random.nextDouble())*Math.tan(spread);
        return look.add(right.multiply(Math.cos(angle)*radius)).add(up.multiply(Math.sin(angle)*radius)).normalize();
    }
    private static void trace(ServerWorld w,Vec3d start,Vec3d end) {
        Vec3d delta=end.subtract(start); int points=(int)Math.min(24,Math.max(1,delta.length()/3));
        for(int i=1;i<=points;i++) { Vec3d pos=start.add(delta.multiply((double)i/points)); w.spawnParticles(ParticleTypes.CRIT,pos.x,pos.y,pos.z,1,0,0,0,0); }
    }
    private static boolean damage(ServerWorld world,Entity source,Entity owner,Entity target,double amount,String type) {
        if(amount<=0 || target.isRemoved() || target.isSpectator() || target instanceof ArsenalProjectile) return false;
        if(target instanceof PlayerEntity targetPlayer) {
            if(!Arsenal.CONFIG.pvpDamage && target!=owner) return false;
            if(owner instanceof PlayerEntity shooter && targetPlayer!=shooter && !shooter.shouldDamagePlayer(targetPlayer)) return false;
        }
        RegistryKey<DamageType> key=RegistryKey.of(RegistryKeys.DAMAGE_TYPE,Arsenal.id(type));
        DamageSource damage=new DamageSource(world.getRegistryManager().getOrThrow(RegistryKeys.DAMAGE_TYPE).getOrThrow(key),source,owner);
        return target.damage(world,damage,(float)(amount*Arsenal.CONFIG.damageMultiplier));
    }
    public static void impact(ArsenalProjectile projectile,HitResult result) {
        if(!(projectile.getEntityWorld() instanceof ServerWorld world)) return;
        Ammo ammo=Arsenal.ammo(projectile.getStack()); Entity owner=projectile.getOwner();
        // A disconnected shooter's ordnance is inert, rather than bypassing PvP attribution.
        if(!(owner instanceof ServerPlayerEntity player) || !player.isAlive() || player.getEntityWorld()!=world || !Arsenal.CONFIG.enabled) return;
        double scale=projectile.getStack().getOrDefault(DataComponentTypes.CUSTOM_DATA,NbtComponent.DEFAULT).copyNbt().getDouble("ArsenalDamageScale",1);
        scale=Double.isFinite(scale)?Math.clamp(scale,.1,2):1;
        Vec3d point=result.getPos();
        if(result instanceof BlockHitResult block) point=point.add(Vec3d.of(block.getSide().getVector()).multiply(.12));
        boolean successful=false; Entity direct=result instanceof EntityHitResult entityHit?entityHit.getEntity():null;
        if(direct!=null && (Arsenal.CONFIG.explosions || ammo==Ammo.ROCKET_PRACTICE))
            successful=damage(world,projectile,player,direct,ammo.damage*scale,ammo.piercing?"piercing":"blast");
        if(ammo.smoke) {
            long count=SMOKE.stream().filter(s->s.world==world).count();
            if(count<Arsenal.CONFIG.smokeCloudLimit) SMOKE.add(new Smoke(world,point,140));
        } else if(ammo.radius>0 && Arsenal.CONFIG.explosions) {
            for(Entity entity:world.getOtherEntities(projectile,Box.of(point,ammo.radius*2,ammo.radius*2,ammo.radius*2),e->e.canHit()&&!e.isSpectator())) {
                if(entity==direct) continue;
                double distance=entity.getBoundingBox().getCenter().distanceTo(point);
                if(distance>=ammo.radius) continue;
                Vec3d target=entity.getBoundingBox().getCenter();
                var obstruction=world.raycast(new RaycastContext(point,target,RaycastContext.ShapeType.COLLIDER,RaycastContext.FluidHandling.NONE,projectile));
                if(obstruction.getType()!=HitResult.Type.MISS) continue;
                successful|=damage(world,projectile,player,entity,18*scale*Animation.blast(distance,ammo.radius),"blast");
            }
            world.spawnParticles(ParticleTypes.EXPLOSION_EMITTER,point.x,point.y,point.z,1,0,0,0,0);
            world.playSound(null,point.x,point.y,point.z,Arsenal.SOUNDS.get("impact"),SoundCategory.PLAYERS,2,.9f);
        } else world.spawnParticles(ParticleTypes.CLOUD,point.x,point.y,point.z,12,.2,.2,.2,.05);
        // Never changes blocks or creates fire. Region-protection bypasses are not introduced.
        if(successful) hit(player);
    }
    private static void hit(ServerPlayerEntity p) {
        if(ServerPlayNetworking.canSend(p,ArsenalPackets.Feedback.ID)) ServerPlayNetworking.send(p,new ArsenalPackets.Feedback(p.getId(),ArsenalPackets.HIT,6));
    }
    public static void tickSmoke() {
        var it=SMOKE.listIterator();
        while(it.hasNext()) {
            Smoke s=it.next();
            if(s.life<=0) { it.remove(); continue; }
            if(s.life%4==0 && s.world.isChunkLoaded(BlockPos.ofFloored(s.pos)))
                s.world.spawnParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE,s.pos.x,s.pos.y+1,s.pos.z,12,1.5,.8,1.5,.008);
            it.set(new Smoke(s.world,s.pos,s.life-1));
        }
    }
}
