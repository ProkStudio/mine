package com.prokstudio.militaryvehicles.entity;

import com.prokstudio.militaryvehicles.core.*;
import com.prokstudio.militaryvehicles.init.MilitaryContent;
import net.minecraft.entity.*;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.item.Item;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.*;
import net.minecraft.world.RaycastContext;
import java.util.*;

/** Server-authoritative crew/service/PvE layer. No client target, position, damage or inventory is trusted. */
final class VehicleSystems {
    private final TruckEntity vehicle;
    private VehicleOperations.Deployment deployment=new VehicleOperations.Deployment(0,false);
    private TrackDrive.State tracks=new TrackDrive.State(0,0,0);
    private long lastAction=Long.MIN_VALUE;
    private int cooldown=100,recoil;
    private float turretYaw,gunPitch;
    private UUID soloCrew;
    VehicleSystems(TruckEntity vehicle) { this.vehicle=vehicle; }
    boolean locked() { return deployment.locked(); }
    void reset() {
        deployment=new VehicleOperations.Deployment(0,false);tracks=new TrackDrive.State(0,0,vehicle.getYaw());
        cooldown=100;recoil=0;turretYaw=0;gunPitch=0;soloCrew=null;lastAction=Long.MIN_VALUE;
        vehicle.syncSystems(0,0,0,0,cooldown,false);
    }
    private ServerWorld world() { return (ServerWorld)vehicle.getEntityWorld(); }
    void tick() {
        if(cooldown>0) cooldown--;if(recoil>0) recoil--;
        var first=vehicle.getFirstPassenger();
        if(soloCrew!=null&&(first==null||!soloCrew.equals(first.getUuid())||vehicle.getPassengerList().size()!=1)) soloCrew=null;
        deployment=deployment.tick(VehicleOperations.parked(vehicle.getVelocity().horizontalLengthSquared()),vehicle.isOnGround(),vehicle.operationReady()&&vehicle.condition()>0&&!vehicle.isTouchingWater());
        if(vehicle.kind().armed()) {
            for(var passenger:vehicle.getPassengerList()) if(passenger instanceof ServerPlayerEntity p&&vehicle.effectiveSeat(p)==1&&!p.isSpectator()&&p.currentScreenHandler==p.playerScreenHandler) {
                turretYaw=VehicleOperations.aimYaw(turretYaw,TruckPhysics.wrap(p.getYaw()-vehicle.getYaw()));
                gunPitch=VehicleOperations.aimPitch(vehicle.kind(),gunPitch,p.getPitch());
                if(vehicle.age%10==0) p.sendMessage(Text.translatable("hud.militaryvehicles.gunner",count(MilitaryContent.VEHICLE_SHELL),cooldown,vehicle.kind()==VehicleKind.HOWITZER?Text.translatable("message.militaryvehicles."+(deployment.ready()?"deployed":"stowed")):Text.literal("PvE")),true);
            }
        }
        vehicle.syncSystems(turretYaw,gunPitch,deployment.ticks(),recoil,cooldown,soloCrew!=null);
    }
    TruckPhysics.Motion move(double actualSpeed,float yaw,int keys,boolean powered,boolean grounded) {
        if(locked()||soloCrew!=null) {tracks=new TrackDrive.State(0,0,yaw);return new TruckPhysics.Motion(0,yaw,0);}
        if(!vehicle.kind().tracked()) return TruckPhysics.step(vehicle.kind(),actualSpeed,yaw,keys,powered,grounded,1);
        tracks=TrackDrive.step(vehicle.kind(),new TrackDrive.State(tracks.left(),tracks.right(),yaw),actualSpeed,keys,powered,grounded,1);
        return new TruckPhysics.Motion(tracks.speed(),tracks.yaw(),(float)((tracks.left()-tracks.right())*2));
    }
    void accept(ServerPlayerEntity player,int action) {
        if(player.getVehicle()!=vehicle||player.isSpectator()||!player.isAlive()||!player.getAbilities().allowModifyWorld
            ||player.currentScreenHandler!=player.playerScreenHandler||!vehicle.operationReady()) return;
        long now=world().getTime();if(now<0||now<=lastAction||action<1||action>3) return;
        lastAction=now;
        if(action==3) {
            if(!vehicle.kind().armed()||player!=vehicle.getFirstPassenger()||vehicle.getPassengerList().size()!=1
                ||!VehicleOperations.parked(vehicle.getVelocity().horizontalLengthSquared())) { message(player,"crew_blocked");return; }
            soloCrew=soloCrew==null?player.getUuid():null;vehicle.clearDriverControls();
            vehicle.syncSystems(turretYaw,gunPitch,deployment.ticks(),recoil,cooldown,soloCrew!=null);
            message(player,soloCrew==null?"driver_station":"gunner_station");return;
        }
        if(!VehicleOperations.authorized(vehicle.kind(),vehicle.effectiveSeat(player),action)) { message(player,"wrong_station");return; }
        if(action==VehicleOperations.DEPLOY) {
            if(!VehicleOperations.parked(vehicle.getVelocity().horizontalLengthSquared())||!vehicle.isOnGround()||vehicle.isTouchingWater()||vehicle.condition()==0) { message(player,"service_park");return; }
            deployment=deployment.toggle(true,true);vehicle.clearDriverControls();
            message(player,deployment.extending()?"deploying":"retracting");return;
        }
        if(vehicle.kind().armed()) fire(player);else service(player);
    }
    private int count(Item item) {
        int result=0;var cargo=vehicle.operationCargo();for(int i=0;i<cargo.size();i++) if(cargo.getStack(i).isOf(item)) result+=cargo.getStack(i).getCount();return result;
    }
    private boolean consume(Item item) {
        var cargo=vehicle.operationCargo();for(int i=0;i<cargo.size();i++) if(cargo.getStack(i).isOf(item)&&!cargo.getStack(i).isEmpty()) { cargo.removeStack(i,1);cargo.markDirty();return true; }return false;
    }
    private Vec3d position(TruckEntity v) { return new Vec3d(v.getX(),v.getY(),v.getZ()); }
    private boolean clearLine(Vec3d from,Vec3d to) {
        return world().raycast(new RaycastContext(from,to,RaycastContext.ShapeType.COLLIDER,RaycastContext.FluidHandling.NONE,vehicle)).getType()==HitResult.Type.MISS;
    }
    private boolean permitted(ServerPlayerEntity p,Vec3d at) {
        BlockPos pos=BlockPos.ofFloored(at);return world().isChunkLoaded(pos)&&world().getWorldBorder().contains(pos)&&world().canEntityModifyAt(p,pos);
    }
    private TruckEntity serviceTarget(ServerPlayerEntity player) {
        Vec3d eye=player.getEyePos(),look=direction(player.getYaw(),player.getPitch());
        return world().getEntitiesByClass(TruckEntity.class,vehicle.getBoundingBox().expand(VehicleOperations.SERVICE_RANGE),target->{
            if(target==vehicle||!target.operationReady()||target.hasPassengers()||target.engineRunning()||target.systemsLocked()
                ||!VehicleOperations.parked(target.getVelocity().horizontalLengthSquared())||target.squaredDistanceTo(vehicle)>VehicleOperations.SERVICE_RANGE*VehicleOperations.SERVICE_RANGE) return false;
            Vec3d center=position(target).add(0,1.5,0),delta=center.subtract(eye);
            return delta.lengthSquared()>.01&&look.dotProduct(delta.normalize())>.88&&permitted(player,center)&&clearLine(eye,center);
        }).stream().min(Comparator.comparingDouble(t->t.squaredDistanceTo(vehicle))).orElse(null);
    }
    private void service(ServerPlayerEntity player) {
        if(cooldown>0) {message(player,"cooldown");return;}
        if(!vehicle.engineRunning()||!vehicle.isOnGround()||vehicle.isTouchingWater()||vehicle.condition()==0||!VehicleOperations.parked(vehicle.getVelocity().horizontalLengthSquared())) {message(player,"service_park");return;}
        TruckEntity target=serviceTarget(player);if(target==null) {message(player,"service_target");return;}
        switch(vehicle.kind()) {
            case TANKER -> {
                int amount=VehicleOperations.transfer(vehicle.fuel(),target.fuel(),target.kind().tank);
                if(amount==0) {message(player,"service_resource");return;}
                target.changeFuel(amount);vehicle.changeFuel(-amount);
                player.sendMessage(Text.translatable("message.militaryvehicles.fuel_transferred",amount),true);
            }
            case WORKSHOP -> {
                int amount=VehicleOperations.repair(target.condition(),target.kind().condition,count(MilitaryContent.REPAIR_KIT));
                if(amount==0||!consume(MilitaryContent.REPAIR_KIT)) {message(player,"service_resource");return;}
                target.repairCondition(amount);player.sendMessage(Text.translatable("message.militaryvehicles.repaired",amount),true);
            }
            case RECOVERY -> {
                if(vehicle.fuel()<VehicleOperations.RECOVERY_COST+VehicleOperations.FUEL_RESERVE) {message(player,"service_resource");return;}
                Vec3d towards=position(vehicle).subtract(position(target));towards=new Vec3d(towards.x,0,towards.z);
                double room=towards.length()-(vehicle.kind().width+target.kind().width)*.5-.6;
                if(room<=.05) {message(player,"recovery_blocked");return;}
                Vec3d delta=towards.normalize().multiply(Math.min(VehicleOperations.RECOVERY_STEP,room));
                Box swept=target.getBoundingBox().stretch(delta);
                for(double x:new double[]{swept.minX,swept.maxX}) for(double z:new double[]{swept.minZ,swept.maxZ})
                    if(!permitted(player,new Vec3d(x,swept.minY,z))) {message(player,"recovery_blocked");return;}
                if(!world().isSpaceEmpty(target,swept)||!world().getOtherEntities(target,swept,e->e.canHit()).isEmpty()) {message(player,"recovery_blocked");return;}
                Vec3d before=position(target);target.setVelocity(Vec3d.ZERO);target.move(MovementType.SELF,delta);
                if(position(target).squaredDistanceTo(before)<.0001) {message(player,"recovery_blocked");return;}
                vehicle.changeFuel(-VehicleOperations.RECOVERY_COST);message(player,"recovered");
            }
            default -> {return;}
        }
        cooldown=VehicleOperations.SERVICE_COOLDOWN;
        world().spawnParticles(ParticleTypes.HAPPY_VILLAGER,target.getX(),target.getY()+2,target.getZ(),6,.3,.2,.3,.01);
    }
    private static Vec3d direction(float yaw,float pitch) {
        double a=Math.toRadians(yaw),p=Math.toRadians(pitch),c=Math.cos(p);
        return new Vec3d(-Math.sin(a)*c,-Math.sin(p),Math.cos(a)*c);
    }
    private void fire(ServerPlayerEntity gunner) {
        VehicleKind kind=vehicle.kind();
        if(!VehicleOperations.canFire(kind,cooldown,count(MilitaryContent.VEHICLE_SHELL),vehicle.condition()>0&&vehicle.fuel()>=5,vehicle.isOnGround(),vehicle.isTouchingWater(),deployment)) {message(gunner,"gun_not_ready");return;}
        if(!Float.isFinite(gunner.getYaw())||!Float.isFinite(gunner.getPitch())) return;
        // Trace from the breech, not a camera or a muzzle teleported through a wall.
        Vec3d turret=new Vec3d(0,ExpansionGeometry.TURRET_Y/16.0,ExpansionGeometry.TURRET_Z/16.0).rotateY((float)-Math.toRadians(vehicle.getYaw()));
        Vec3d bore=new Vec3d(0,(ExpansionGeometry.GUN_Y-ExpansionGeometry.TURRET_Y)/16.0,(ExpansionGeometry.GUN_Z-ExpansionGeometry.TURRET_Z)/16.0).rotateY((float)-Math.toRadians(vehicle.getYaw()+turretYaw));
        Vec3d start=position(vehicle).add(turret).add(bore),look=direction(vehicle.getYaw()+turretYaw,gunPitch);
        if(!permitted(gunner,start)) {message(gunner,"recovery_blocked");return;}
        double range=VehicleOperations.gunRange(kind),allowed=0;
        for(double d=1;d<=range;d++) { if(!permitted(gunner,start.add(look.multiply(d)))) break;allowed=d; }
        if(allowed<1) return;
        Vec3d end=start.add(look.multiply(allowed));var block=world().raycast(new RaycastContext(start,end,RaycastContext.ShapeType.COLLIDER,RaycastContext.FluidHandling.NONE,vehicle));
        if(block.getType()!=HitResult.Type.MISS) end=block.getPos();
        double nearest=start.squaredDistanceTo(end);
        for(Entity e:world().getOtherEntities(vehicle,new Box(start,end).expand(1),e->e.canHit()&&!vehicle.getPassengerList().contains(e))) {
            var hit=e.getBoundingBox().raycast(start,end);if(hit.isPresent()&&start.squaredDistanceTo(hit.get())<nearest) {end=hit.get();nearest=start.squaredDistanceTo(end);}
        }
        if(!consume(MilitaryContent.VEHICLE_SHELL)) return;
        vehicle.changeFuel(-5);cooldown=VehicleOperations.gunCooldown(kind);recoil=8;
        Vec3d impact=end;double radius=kind==VehicleKind.HOWITZER?2.5:1.5;
        // Deliberately PvE-only. No players, pets, friendly vehicles, block edits, fire or explosions.
        int damaged=0;
        for(HostileEntity mob:world().getEntitiesByClass(HostileEntity.class,new Box(impact,impact).expand(radius),e->e.isAlive()&&!e.hasVehicle())) {
            Vec3d center=mob.getBoundingBox().getCenter();if(center.squaredDistanceTo(impact)>radius*radius||!permitted(gunner,center)||!clearLine(impact.add(look.multiply(-.1)),center)) continue;
            mob.damage(world(),world().getDamageSources().playerAttack(gunner),VehicleOperations.gunDamage(kind));
            if(++damaged>=16) break;
        }
        double length=start.distanceTo(impact);int dots=Math.min(24,Math.max(1,(int)Math.ceil(length/2)));
        for(int i=1;i<=dots;i++) {Vec3d point=start.lerp(impact,i/(double)dots);world().spawnParticles(ParticleTypes.SMOKE,point.x,point.y,point.z,1,0,0,0,0);}
        world().spawnParticles(ParticleTypes.EXPLOSION,impact.x,impact.y,impact.z,1,0,0,0,0);
        world().playSound(null,vehicle.getX(),vehicle.getY(),vehicle.getZ(),SoundEvents.ENTITY_GENERIC_EXPLODE,SoundCategory.PLAYERS,1.0f,kind==VehicleKind.HOWITZER?.65f:.85f);
        vehicle.syncSystems(turretYaw,gunPitch,deployment.ticks(),recoil,cooldown,soloCrew!=null);
    }
    private void message(ServerPlayerEntity p,String key) {p.sendMessage(Text.translatable("message.militaryvehicles."+key),true);}
}
