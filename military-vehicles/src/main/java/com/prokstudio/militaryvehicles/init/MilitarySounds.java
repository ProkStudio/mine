package com.prokstudio.militaryvehicles.init;

import com.prokstudio.militaryvehicles.core.VehicleKind;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;

public final class MilitarySounds {
    public static final SoundEvent TRUCK_ENGINE=register(VehicleKind.TRUCK.soundId);
    public static final SoundEvent BUGGY_ENGINE=register(VehicleKind.BUGGY.soundId);
    public static final SoundEvent CARRIER_ENGINE=register(VehicleKind.CARRIER.soundId);
    private MilitarySounds() {}
    private static SoundEvent register(String name) {
        return Registry.register(Registries.SOUND_EVENT,MilitaryContent.id(name),SoundEvent.of(MilitaryContent.id(name)));
    }
    public static SoundEvent forKind(VehicleKind kind) {
        return switch(kind) {case TRUCK->TRUCK_ENGINE;case BUGGY->BUGGY_ENGINE;case CARRIER->CARRIER_ENGINE;};
    }
    public static void register() {}
}
