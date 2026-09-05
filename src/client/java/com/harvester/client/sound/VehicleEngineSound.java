package com.harvester.client.sound;

import com.harvester.entity.CombineEntity;
import com.harvester.init.ModSounds;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.MovingSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.sound.SoundCategory;

public final class VehicleEngineSound extends MovingSoundInstance {
    private final CombineEntity vehicle;
    private final float gain;
    private boolean requested=true, finished;
    private int ticks;
    public VehicleEngineSound(CombineEntity vehicle,float gain) {
        super(ModSounds.forType(vehicle.variant()),SoundCategory.NEUTRAL,SoundInstance.createRandom());
        this.vehicle=vehicle; this.gain=gain;
        repeat=true; repeatDelay=0; relative=false;
        volume=.001f; pitch=.75f;
        x=vehicle.getX(); y=vehicle.getY(); z=vehicle.getZ();
    }
    public void request(boolean enabled) { requested=enabled; }
    public boolean finished() { return finished; }
    public int ticks() { return ticks; }
    public void stopNow() { finished=true; setDone(); }
    @Override public boolean shouldAlwaysPlay() { return true; }
    @Override public boolean canPlay() {
        return !finished && !vehicle.isRemoved() && MinecraftClient.getInstance().world==vehicle.getEntityWorld();
    }
    @Override public void tick() {
        ticks++;
        var world=MinecraftClient.getInstance().world;
        if(world==null || world!=vehicle.getEntityWorld() || vehicle.isRemoved() || !world.hasEntity(vehicle)) { stopNow(); return; }
        x=vehicle.getX(); y=vehicle.getY(); z=vehicle.getZ();
        boolean powered=requested && vehicle.isHarvesting() && vehicle.getFuel()>0 && vehicle.getCondition()>0;
        double speed=vehicle.getVelocity().length();
        float load=(float)Math.clamp(Double.isFinite(speed)?speed/.45:0,0,1);
        float targetVolume=powered?gain*(.35f+.35f*load):0;
        volume+=(targetVolume-volume)*.25f;
        pitch+=(.75f+.45f*load-pitch)*.15f;
        if(!powered && volume<.002f) stopNow();
    }
}
