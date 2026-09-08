package com.prokstudio.militaryvehicles.client;

import com.prokstudio.militaryvehicles.core.EngineFeedback;
import com.prokstudio.militaryvehicles.entity.TruckEntity;
import com.prokstudio.militaryvehicles.init.MilitarySounds;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.MovingSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.sound.SoundCategory;

/** Each audible truck owns one independent, smoothly started/stopped positional loop. */
final class TruckEngineSound extends MovingSoundInstance {
    private final TruckEntity truck;
    private final EngineFeedback envelope = new EngineFeedback();
    private boolean requested = true, finished;

    TruckEngineSound(TruckEntity truck) {
        super(MilitarySounds.TRUCK_ENGINE, SoundCategory.NEUTRAL, SoundInstance.createRandom());
        this.truck = truck;
        repeat = true; repeatDelay = 0; relative = false;
        volume = .001f; pitch = .78f;
        x = truck.getX(); y = truck.getY() + .9; z = truck.getZ();
    }

    void request(boolean enabled) { requested = enabled; }
    boolean finished() { return finished; }
    void stopNow() { finished = true; setDone(); }
    @Override public boolean shouldAlwaysPlay() { return true; }
    @Override public boolean canPlay() {
        return !finished && !truck.isRemoved() && !truck.isSilent()
            && MinecraftClient.getInstance().world == truck.getEntityWorld();
    }
    @Override public void tick() {
        var world = MinecraftClient.getInstance().world;
        if (world == null || world != truck.getEntityWorld() || truck.isRemoved()
                || truck.isSilent() || !world.hasEntity(truck)) { stopNow(); return; }
        x = truck.getX(); y = truck.getY() + .9; z = truck.getZ();
        boolean powered = requested && truck.engineRunning() && truck.fuel() > 0 && truck.condition() > 0;
        var frame = envelope.update(powered, truck.engineLoad(), truck.observedSpeed());
        volume = frame.volume(); pitch = frame.pitch();
        if (!powered && volume == 0) stopNow();
    }
}
