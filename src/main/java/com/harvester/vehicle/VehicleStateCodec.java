package com.harvester.vehicle;

import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * The shared vehicle save format, independent of Minecraft classes and registries.
 * Production supplies ItemStack.OPTIONAL_CODEC and registry-aware NbtOps;
 * unit tests supply a registry-free cargo codec and JsonOps.
 */
public final class VehicleStateCodec {
    public static final int VERSION = 1;
    private VehicleStateCodec() {}

    public record State<C>(VehicleType type, int fuel, int condition, int color,
                           boolean headerEnabled, int workCooldown, List<C> cargo) {
        public State {
            Objects.requireNonNull(type, "type");
            cargo = List.copyOf(cargo);
        }
    }

    public static <T, C> T encode(DynamicOps<T> ops, Codec<C> cargoCodec, State<C> state) {
        Map<T, T> fields = new LinkedHashMap<>();
        fields.put(ops.createString("Version"), ops.createInt(VERSION));
        fields.put(ops.createString("Type"), ops.createString(state.type().id));
        fields.put(ops.createString("Fuel"), ops.createInt(state.fuel()));
        fields.put(ops.createString("Condition"), ops.createInt(state.condition()));
        fields.put(ops.createString("Color"), ops.createInt(state.color()));
        fields.put(ops.createString("HeaderEnabled"), ops.createBoolean(state.headerEnabled()));
        fields.put(ops.createString("WorkCooldown"), ops.createInt(state.workCooldown()));
        // Keep cargo failures visible: the caller must not remove the original vehicle.
        fields.put(ops.createString("Inventory"), cargoCodec.listOf().encodeStart(ops, state.cargo()).getOrThrow());
        return ops.createMap(fields);
    }

    public static <T, C> State<C> decode(DynamicOps<T> ops, Codec<C> cargoCodec, T encoded) {
        // Reject a non-object root instead of silently producing an empty default vehicle.
        ops.getMap(encoded).getOrThrow();
        Dynamic<T> data = new Dynamic<>(ops, encoded);
        if (data.get("Version").asInt(VERSION) != VERSION) {
            throw new IllegalArgumentException("Unsupported vehicle state version");
        }
        VehicleType type = VehicleType.fromId(data.get("Type").asString(VehicleType.COMBINE.id));
        List<C> cargo = data.get("Inventory").result()
                .map(value -> cargoCodec.listOf().parse(ops, value.getValue()).getOrThrow())
                .orElse(List.of());
        if (cargo.size() > 54) throw new IllegalArgumentException("Cargo exceeds 54 slots");
        return new State<>(type,
                Math.clamp(data.get("Fuel").asInt(0), 0, 64000),
                Math.clamp(data.get("Condition").asInt(type.durability), 0, 10000),
                Math.clamp(data.get("Color").asInt(0), 0, 15),
                data.get("HeaderEnabled").asBoolean(true),
                Math.clamp(data.get("WorkCooldown").asInt(0), 0, 100), cargo);
    }
}
