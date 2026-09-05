package com.harvester.init;

import net.minecraft.registry.*;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

public final class ModSounds {
    public static final SoundEvent ENGINE=Registry.register(Registries.SOUND_EVENT,Identifier.of("harvester","engine"),SoundEvent.of(Identifier.of("harvester","engine")));
    public static void register() {}
}
