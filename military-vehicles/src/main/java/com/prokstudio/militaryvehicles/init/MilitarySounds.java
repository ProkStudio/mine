package com.prokstudio.militaryvehicles.init;

import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;

public final class MilitarySounds {
    public static final SoundEvent TRUCK_ENGINE = Registry.register(Registries.SOUND_EVENT,
        MilitaryContent.id("truck_engine"), SoundEvent.of(MilitaryContent.id("truck_engine")));
    private MilitarySounds() {}
    public static void register() {}
}
