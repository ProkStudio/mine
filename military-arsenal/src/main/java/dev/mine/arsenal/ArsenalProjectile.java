package dev.mine.arsenal;

import dev.mine.arsenal.core.Ammo;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.projectile.thrown.ThrownItemEntity;
import net.minecraft.item.Item;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.*;
import net.minecraft.world.World;

/** Vanilla swept projectile collision, owner attribution and stack synchronization. */
public final class ArsenalProjectile extends ThrownItemEntity {
    private boolean detonated;
    public ArsenalProjectile(EntityType<? extends ArsenalProjectile> type,World world) { super(type,world); }
    @Override protected Item getDefaultItem() { return Arsenal.AMMO.get(Ammo.ROCKET_PRACTICE); }
    @Override protected double getGravity() { return Arsenal.ammo(getStack()).gravity(); }
    @Override public void tick() {
        if(getEntityWorld() instanceof ServerWorld world) {
            Vec3d next=new Vec3d(getX(),getY(),getZ()).add(getVelocity());
            if(age>100 || !world.isChunkLoaded(BlockPos.ofFloored(next))) { discard(); return; }
            if(Arsenal.ammo(getStack()).grenade() && age>=40) {
                onCollision(BlockHitResult.createMissed(new Vec3d(getX(),getY(),getZ()),Direction.UP,getBlockPos())); return;
            }
            world.spawnParticles(Arsenal.ammo(getStack()).grenade()?ParticleTypes.SMOKE:ParticleTypes.SMALL_FLAME,getX(),getY(),getZ(),1,0,0,0,0);
        }
        super.tick();
    }
    @Override protected void onCollision(HitResult result) {
        if(detonated || getEntityWorld().isClient()) return;
        detonated=true; Combat.impact(this,result); discard();
    }
}
