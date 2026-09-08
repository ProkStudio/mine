package com.prokstudio.militaryvehicles.client;

import com.prokstudio.militaryvehicles.core.TruckFeedback;
import com.prokstudio.militaryvehicles.entity.TruckEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.sound.SoundCategory;
import java.util.*;

/** Hard cap includes fading loops. World exit, mute and unload release all retained entities. */
public final class TruckAudio {
    private record Loop(TruckEngineSound sound, long started) {}
    private static final Map<TruckEntity, Loop> LOOPS = new LinkedHashMap<>();
    private static ClientWorld world;
    private static long clock;
    private TruckAudio() {}

    private static void stop(MinecraftClient client, Loop loop) {
        loop.sound().stopNow(); client.getSoundManager().stop(loop.sound());
    }
    private static void clear(MinecraftClient client) {
        LOOPS.values().forEach(loop -> stop(client, loop));
        LOOPS.clear(); clock = 0;
    }
    public static void tick(MinecraftClient client) {
        if (world != client.world || client.player == null) {
            clear(client); world = client.world;
        }
        if (world == null || client.player == null) return;
        if (client.options.getSoundVolume(SoundCategory.MASTER) <= 0
                || client.options.getSoundVolume(SoundCategory.NEUTRAL) <= 0) { clear(client); return; }
        if (client.isPaused()) return;
        clock++;
        var manager = client.getSoundManager();
        Map<Integer, TruckEntity> trucks = new HashMap<>();
        List<TruckFeedback.Candidate> candidates = new ArrayList<>();
        for (var entity : world.getEntities()) {
            if (!(entity instanceof TruckEntity truck) || truck.isRemoved() || truck.isSilent()
                    || !truck.engineRunning() || truck.fuel() <= 0 || truck.condition() <= 0) continue;
            trucks.put(truck.getId(), truck);
            candidates.add(new TruckFeedback.Candidate(truck.getId(), client.player.squaredDistanceTo(truck),
                client.player.getVehicle() == truck, LOOPS.containsKey(truck)));
        }
        List<TruckEntity> priority = TruckFeedback.selectVoices(candidates).stream().map(trucks::get).toList();
        Set<TruckEntity> selected = new HashSet<>(priority);
        var iterator = LOOPS.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next(); var truck = entry.getKey(); var loop = entry.getValue();
            loop.sound().request(selected.contains(truck));
            boolean gone = truck.isRemoved() || truck.isSilent() || !world.hasEntity(truck);
            // Sound reload stops channels without deleting entities. Retry after a startup grace period.
            boolean channelLost = clock - loop.started() > 20 && !manager.isPlaying(loop.sound());
            if (gone || loop.sound().finished() || channelLost) { stop(client, loop); iterator.remove(); }
        }
        for (var truck : priority) {
            if (LOOPS.containsKey(truck)) continue;
            if (LOOPS.size() >= TruckFeedback.VOICE_LIMIT) {
                // Do not make the player's engine wait behind eight fading or lower-priority loops.
                var victim = LOOPS.keySet().stream().filter(v -> !selected.contains(v)).findFirst();
                if (victim.isEmpty()) break;
                stop(client, LOOPS.remove(victim.get()));
            }
            var sound = new TruckEngineSound(truck);
            LOOPS.put(truck, new Loop(sound, clock)); manager.play(sound);
        }
    }
}
