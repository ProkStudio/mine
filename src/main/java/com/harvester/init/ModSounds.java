package com.harvester.init;

import com.harvester.vehicle.VehicleType;
import net.minecraft.registry.*;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

public final class ModSounds {
    public static final SoundEvent ENGINE=register("engine");
    public static final SoundEvent MOTORCYCLE=register("motorcycle_engine");
    public static final SoundEvent BOAT=register("boat_engine");
    public static final SoundEvent PLANE=register("plane_engine");
    public static final SoundEvent HELICOPTER=register("helicopter_engine");
    public static final SoundEvent DRONE=register("drone_engine");
    private static SoundEvent register(String name) {
        Identifier id=Identifier.of("harvester",name);
        return Registry.register(Registries.SOUND_EVENT,id,SoundEvent.of(id));
    }
    public static SoundEvent forType(VehicleType type) {
        return switch(type.family) {
            case MOTORCYCLE -> MOTORCYCLE;
            case BOAT -> BOAT;
            case PLANE -> PLANE;
            case HELICOPTER -> HELICOPTER;
            case DRONE -> DRONE;
            default -> ENGINE;
        };
    }
    public static void register() {}
}
