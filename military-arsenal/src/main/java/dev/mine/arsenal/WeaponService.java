package dev.mine.arsenal;

import dev.mine.arsenal.core.*;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import java.util.*;
import static dev.mine.arsenal.ArsenalPackets.*;

/** All writes happen on the server thread. One coalesced input snapshot per simulation tick. */
public final class WeaponService {
    private static final class Session {
        final Trigger trigger=new Trigger();
        final ArrayDeque<Long> projectiles=new ArrayDeque<>();
        ItemStack stack=ItemStack.EMPTY;
        Object world;
        int input,previous,latched;
        long nextGrenade;
        long received=-1000,shot=-1000,reload=-1,inspect=-1,lastAction=-1000,dry=-1000;
    }
    private final Map<UUID,Session> sessions=new HashMap<>();
    private long clock;
    public void accept(ServerPlayerEntity p,byte keys) {
        int input=Byte.toUnsignedInt(keys);
        if((input&~63)!=0 || !p.isAlive() || p.isSpectator()) return;
        if(!(p.getMainHandStack().getItem() instanceof GunItem) && !sessions.containsKey(p.getUuid())) return;
        Session s=sessions.computeIfAbsent(p.getUuid(),id->new Session());
        // Edges are latched once even if a press/release both arrive between ticks.
        s.latched|=(input&~s.input)&(RELOAD|MODE|AMMO|INSPECT);
        s.input=input; s.received=clock;
    }
    public void remove(ServerPlayerEntity p) {
        Session s=sessions.remove(p.getUuid());
        if(s!=null && s.stack.getItem() instanceof GunItem) GunItem.pose(s.stack,0,false,false);
    }
    public void clear() { sessions.clear(); Combat.clear(); clock=0; }
    public void tick(MinecraftServer server) {
        clock++;
        for(ServerPlayerEntity player:server.getPlayerManager().getPlayerList()) {
            if(player.getMainHandStack().getItem() instanceof GunItem || sessions.containsKey(player.getUuid())) tickPlayer(player);
        }
        Combat.tickSmoke();
    }
    private void cancel(ServerPlayerEntity p,Session s) {
        boolean active=s.reload>=0||s.inspect>=0;
        s.reload=s.inspect=-1; s.trigger.interrupt();
        if(s.stack.getItem() instanceof GunItem) GunItem.pose(s.stack,0,false,false);
        if(active) feedback(p,STOP,0);
    }
    private void tickPlayer(ServerPlayerEntity p) {
        Session s=sessions.computeIfAbsent(p.getUuid(),id->new Session());
        ItemStack held=p.getMainHandStack();
        if(held!=s.stack || s.world!=p.getEntityWorld()) {
            cancel(p,s); s.stack=held; s.world=p.getEntityWorld(); s.trigger.equip(clock); s.previous=s.input; s.latched=0;
        }
        int input=clock-s.received<=Arsenal.CONFIG.inputTimeoutTicks?s.input:0;
        int actions=s.latched; s.latched=0;
        if(!Arsenal.CONFIG.enabled || !p.isAlive() || p.isSpectator() || !(held.getItem() instanceof GunItem gun)
            || p.currentScreenHandler!=p.playerScreenHandler) {
            cancel(p,s); s.previous=input;
            if(!(held.getItem() instanceof GunItem) && clock-s.received>200 && clock>s.trigger.nextShot()) sessions.remove(p.getUuid());
            return;
        }
        if(clock-s.received>Arsenal.CONFIG.inputTimeoutTicks) { actions=0; cancel(p,s); }
        Weapon w=gun.weapon; Magazine m=gun.magazine(held);
        boolean pressed=(input&FIRE)!=0 && (s.previous&FIRE)==0;
        boolean aim=(input&AIM)!=0 && !p.isSprinting();
        if(actions!=0 && clock-s.lastAction>=4) {
            s.lastAction=clock;
            if((actions&MODE)!=0) { m=m.cycleMode(w); gun.magazine(held,m); s.trigger.interrupt(); message(p,"mode.arsenal."+m.mode().name().toLowerCase(Locale.ROOT)); }
            if((actions&AMMO)!=0) {
                if(w.ammunition.size()==1) message(p,"message.arsenal.single_ammo");
                else {
                    cancel(p,s);
                    Ammo oldAmmo=m.ammo(); int returned=m.rounds();
                    m=new Magazine(0,oldAmmo,m.mode()).cycleAmmo(w);
                    // Commit the empty magazine first. Return its original type, never the new type.
                    gun.magazine(held,m);
                    if(!p.isCreative() && returned>0) returnAmmo(p,oldAmmo,returned);
                    p.sendMessage(Text.translatable("message.arsenal.ammo_selected",Text.translatable("item.arsenal."+m.ammo().id)),true);
                }
            }
            if((actions&RELOAD)!=0 && s.reload<0 && m.rounds()<w.capacity) {
                if(p.isCreative()||available(p,m.ammo())>0) startReload(p,s,w);
                else { message(p,"message.arsenal.no_ammo"); dry(p,s); }
            }
            if((actions&INSPECT)!=0 && s.reload<0) { s.inspect=clock; feedback(p,INSPECTING,40); }
        }
        if(p.isSprinting()) { cancel(p,s); aim=false; }
        if(s.reload>=0 && pressed && w.shellReload() && m.rounds()>0) { s.reload=-1; feedback(p,STOP,0); }
        if(s.reload>=0 && clock-s.reload>=w.reloadTicks) {
            // No reservation: cancelling, dropping or disconnecting cannot lose or duplicate ammo.
            int available=p.isCreative()?w.capacity:available(p,m.ammo());
            Magazine next=m.reload(w,available); int amount=next.rounds()-m.rounds();
            if(amount>0) { if(!p.isCreative()) consume(p,m.ammo(),amount); gun.magazine(held,next); m=next; sound(p,"reload_in",.7f,1); }
            if(amount>0 && w.shellReload() && m.rounds()<w.capacity && (p.isCreative()||available(p,m.ammo())>0)) startReload(p,s,w);
            else { s.reload=-1; sound(p,"bolt",.55f,1); feedback(p,STOP,0); }
        }
        boolean allowed=s.reload<0&&!p.isSprinting()&&m.rounds()>0;
        if(s.trigger.tick(clock,(input&FIRE)!=0,allowed,m.mode(),w.interval)) {
            while(!s.projectiles.isEmpty() && s.projectiles.peekFirst()<=clock-100) s.projectiles.removeFirst();
            if(!m.ammo().projectile() || s.projectiles.size()<Arsenal.CONFIG.projectileLimitPerPlayer) {
                boolean fired=Combat.fire(p,w,m.ammo(),aim);
                if(fired) {
                    gun.magazine(held,m.shot()); s.shot=clock; s.inspect=-1;
                    if(m.ammo().projectile()) s.projectiles.addLast(clock);
                    sound(p,w.sound(),w==Weapon.WARDEN?.25f:1, w==Weapon.BASTION?.82f:1);
                    feedback(p,SHOT,w.interval);
                }
            } else message(p,"message.arsenal.projectile_limit");
        } else if(pressed && m.rounds()==0 && s.reload<0) dry(p,s);
        if(s.inspect>=0 && clock-s.inspect>=40) s.inspect=-1;
        int frame=s.reload>=0?Animation.reloadFrame(clock-s.reload,w.reloadTicks):s.inspect>=0?Animation.inspectFrame(clock-s.inspect):Animation.shotFrame(clock-s.shot);
        GunItem.pose(held,frame,aim&&s.reload<0,s.reload>=0);
        s.previous=input;
    }
    /** Vanilla use packets still go through server cooldowns and the shared projectile budget. */
    public void throwGrenade(ServerPlayerEntity p,Hand hand) {
        ItemStack held=p.getStackInHand(hand);
        if(!(held.getItem() instanceof GrenadeItem) || held.isEmpty() || !p.isAlive() || p.isSpectator()
            || !Arsenal.CONFIG.enabled || p.currentScreenHandler!=p.playerScreenHandler) return;
        Session s=sessions.computeIfAbsent(p.getUuid(),id->new Session());
        if(clock<s.nextGrenade) return;
        while(!s.projectiles.isEmpty() && s.projectiles.peekFirst()<=clock-100) s.projectiles.removeFirst();
        if(s.projectiles.size()>=Arsenal.CONFIG.projectileLimitPerPlayer) {
            s.nextGrenade=clock+20; message(p,"message.arsenal.projectile_limit"); return;
        }
        Ammo ammo=Arsenal.ammo(held);
        var world=(ServerWorld)p.getEntityWorld();
        var projectile=new ArsenalProjectile(Arsenal.PROJECTILE,world);
        projectile.setOwner(p); projectile.setItem(new ItemStack(Arsenal.AMMO.get(ammo)));
        projectile.setPosition(p.getX(),p.getEyeY()-.1,p.getZ());
        var direction=p.getRotationVec(1);
        projectile.setVelocity(direction.x,direction.y,direction.z,(float)ammo.velocity,0);
        if(world.spawnEntity(projectile)) {
            s.nextGrenade=clock+20; s.projectiles.addLast(clock); s.received=clock;
            if(!p.isCreative()) { held.decrement(1); p.getInventory().markDirty(); }
            sound(p,"reload_out",.45f,1.25f);
        }
    }
    private static void returnAmmo(ServerPlayerEntity p,Ammo ammo,int count) {
        while(count>0) {
            ItemStack returned=new ItemStack(Arsenal.AMMO.get(ammo));
            int amount=Math.min(count,returned.getMaxCount()); returned.setCount(amount); count-=amount;
            p.getInventory().insertStack(returned);
            if(!returned.isEmpty()) p.dropItem(returned,false);
        }
        p.getInventory().markDirty();
    }
    private void startReload(ServerPlayerEntity p,Session s,Weapon w) {
        s.reload=clock; s.inspect=-1; s.trigger.interrupt(); sound(p,"reload_out",.6f,1);
        feedback(p,RELOADING,w.reloadTicks);
    }
    private void dry(ServerPlayerEntity p,Session s) { if(clock-s.dry>=10) { s.dry=clock; sound(p,"dry",.5f,1); } }
    private static void message(ServerPlayerEntity p,String key) { p.sendMessage(Text.translatable(key),true); }
    public static int available(PlayerEntity p,Ammo a) {
        int result=0;
        for(int i=0;i<p.getInventory().size();i++) { var stack=p.getInventory().getStack(i); if(stack.isOf(Arsenal.AMMO.get(a))) result+=stack.getCount(); }
        return result;
    }
    private static void consume(PlayerEntity p,Ammo a,int count) {
        for(int i=0;i<p.getInventory().size()&&count>0;i++) {
            var stack=p.getInventory().getStack(i); if(!stack.isOf(Arsenal.AMMO.get(a))) continue;
            int amount=Math.min(count,stack.getCount()); stack.decrement(amount); count-=amount;
        }
        p.getInventory().markDirty();
    }
    public static void sound(ServerPlayerEntity p,String id,float volume,float pitch) {
        p.getEntityWorld().playSound(null,p.getX(),p.getY(),p.getZ(),Arsenal.SOUNDS.get(id),SoundCategory.PLAYERS,volume,pitch);
    }
    public static void feedback(ServerPlayerEntity p,byte event,int duration) {
        var packet=new Feedback(p.getId(),event,duration);
        for(ServerPlayerEntity observer:((ServerWorld)p.getEntityWorld()).getPlayers())
            if(observer.squaredDistanceTo(p)<96*96 && ServerPlayNetworking.canSend(observer,Feedback.ID)) ServerPlayNetworking.send(observer,packet);
    }
}
