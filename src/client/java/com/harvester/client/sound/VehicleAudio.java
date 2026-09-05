package com.harvester.client.sound;

import com.harvester.entity.CombineEntity;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import java.nio.file.*;
import java.util.*;

/** One loop per nearby vehicle, capped voices and no retained entities after world exit. */
public final class VehicleAudio {
    private static final Map<CombineEntity,VehicleEngineSound> SOUNDS=new HashMap<>();
    private static ClientWorld world;
    private static float gain=.65f;
    private static boolean configured;
    private VehicleAudio() {}
    private static void configure() {
        configured=true;
        Path path=FabricLoader.getInstance().getConfigDir().resolve("harvester-client.properties");
        Properties p=new Properties();
        try {
            if(Files.exists(path)) try(var input=Files.newBufferedReader(path)) { p.load(input); }
            else {
                p.setProperty("engineVolume","0.65"); Files.createDirectories(path.getParent());
                try(var output=Files.newBufferedWriter(path)) { p.store(output,"Harvester client audio. engineVolume 0..1; restart client after editing. Also respects Friendly Creatures volume."); }
            }
            float value=Float.parseFloat(p.getProperty("engineVolume","0.65"));
            gain=Float.isFinite(value)?Math.clamp(value,0,1):.65f;
        } catch(Exception e) { com.harvester.HarvesterMod.LOGGER.warn("Invalid client audio configuration; using defaults",e); }
    }
    public static void tick(MinecraftClient client) {
        if(!configured) configure();
        if(world!=client.world || client.player==null) {
            for(var sound:SOUNDS.values()) { sound.stopNow(); client.getSoundManager().stop(sound); }
            SOUNDS.clear(); world=client.world;
        }
        if(world==null || client.player==null || gain<=0) return;
        var manager=client.getSoundManager();
        List<CombineEntity> nearby=new ArrayList<>();
        for(var entity:world.getEntities()) if(entity instanceof CombineEntity vehicle
                && !vehicle.isRemoved() && vehicle.isHarvesting() && client.player.squaredDistanceTo(vehicle)<=32*32) nearby.add(vehicle);
        nearby.sort(Comparator.comparingDouble(v->client.player.squaredDistanceTo(v)));
        Set<CombineEntity> selected=new HashSet<>(nearby.subList(0,Math.min(16,nearby.size())));
        var iterator=SOUNDS.entrySet().iterator();
        while(iterator.hasNext()) {
            var entry=iterator.next(); var vehicle=entry.getKey(); var sound=entry.getValue();
            sound.request(selected.contains(vehicle));
            boolean lost=vehicle.isRemoved() || !world.hasEntity(vehicle);
            boolean reload=sound.ticks()>20 && !manager.isPlaying(sound);
            if(lost || sound.finished() || reload) { sound.stopNow(); manager.stop(sound); iterator.remove(); }
        }
        for(var vehicle:selected) if(!SOUNDS.containsKey(vehicle)) {
            var sound=new VehicleEngineSound(vehicle,gain); SOUNDS.put(vehicle,sound); manager.play(sound);
        }
    }
}
