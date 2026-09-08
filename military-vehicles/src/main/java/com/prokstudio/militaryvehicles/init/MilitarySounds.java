package com.prokstudio.militaryvehicles.init;
import com.prokstudio.militaryvehicles.core.VehicleKind;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import java.util.*;
public final class MilitarySounds {
    private static final Map<String,SoundEvent> SOUNDS=new HashMap<>();
    static {for(var kind:VehicleKind.values()) SOUNDS.computeIfAbsent(kind.soundId,MilitarySounds::register);}
    public static final SoundEvent TRUCK_ENGINE=SOUNDS.get(VehicleKind.TRUCK.soundId);
    public static final SoundEvent BUGGY_ENGINE=SOUNDS.get(VehicleKind.BUGGY.soundId);
    public static final SoundEvent CARRIER_ENGINE=SOUNDS.get(VehicleKind.CARRIER.soundId);
    private MilitarySounds() {}
    private static SoundEvent register(String name) {return Registry.register(Registries.SOUND_EVENT,MilitaryContent.id(name),SoundEvent.of(MilitaryContent.id(name)));}
    public static SoundEvent forKind(VehicleKind kind) {return Objects.requireNonNull(SOUNDS.get(kind.soundId));}
    public static void register() {}
}
