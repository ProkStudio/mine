package com.harvester.client.sound;

import com.harvester.entity.CombineEntity;
import com.harvester.init.ModSounds;
import com.harvester.vehicle.VehicleSoundEnvelope;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.MovingSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.sound.SoundCategory;

public final class VehicleEngineSound extends MovingSoundInstance {
    private final CombineEntity vehicle;
    private final float gain;
    private final VehicleSoundEnvelope envelope=new VehicleSoundEnvelope();
    private boolean requested=true,finished;
    public VehicleEngineSound(CombineEntity vehicle,float gain) {
        super(ModSounds.forType(vehicle.variant()),SoundCategory.NEUTRAL,SoundInstance.createRandom());
        this.vehicle=vehicle; this.gain=gain; repeat=true; repeatDelay=0; relative=false;
        volume=.001f; pitch=.75f; x=vehicle.getX(); y=vehicle.getY(); z=vehicle.getZ();
    }
    public void request(boolean enabled) { requested=enabled; }
    public boolean finished() { return finished; }
    public void stopNow() { finished=true; setDone(); }
    @Override public boolean shouldAlwaysPlay() { return true; }
    @Override public boolean canPlay() { return !finished && !vehicle.isRemoved() && MinecraftClient.getInstance().world==vehicle.getEntityWorld(); }
    @Override public void tick() {
        var world=MinecraftClient.getInstance().world;
        if(world==null || world!=vehicle.getEntityWorld() || vehicle.isRemoved() || !world.hasEntity(vehicle)) { stopNow(); return; }
        var at=vehicle.localEffect(0,.6,0); x=at.x; y=at.y; z=at.z;
        boolean powered=requested && vehicle.isEngineActive() && vehicle.getFuel()>0 && vehicle.getCondition()>0;
        double speed=vehicle.getVelocity().length();
        var frame=envelope.update(vehicle.variant(),1,powered,vehicle.driveInput(),speed,vehicle.isWorking(),gain);
        volume=frame.volume();pitch=frame.pitch();
        if(!powered && volume<.002f) stopNow();
    }
}
