package com.harvester.vehicle;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests of the actual shared vehicle format, without Minecraft bootstrap.
 * Cargo is an opaque JSON fixture, NOT an ItemStack mock claiming to test registries.
 * Real ItemStack/NbtComponent integration is a separate in-game QA requirement.
 */
class VehicleStateTest {
    private static final Codec<JsonElement> CARGO_CODEC = Codec.PASSTHROUGH.xmap(
            value -> value.convert(JsonOps.INSTANCE).getValue().deepCopy(),
            value -> new Dynamic<>(JsonOps.INSTANCE, value.deepCopy()));

    private static JsonObject cargo(String item, int count, String name) {
        JsonObject stack = new JsonObject();
        stack.addProperty("id", item);
        stack.addProperty("count", count);
        JsonObject components = new JsonObject();
        components.addProperty("minecraft:custom_name", name);
        JsonObject custom = new JsonObject();
        custom.addProperty("marker", "retained-" + name);
        components.add("minecraft:custom_data", custom);
        stack.add("components", components);
        return stack;
    }

    private static JsonObject encode(VehicleStateCodec.State<JsonElement> state) {
        return VehicleStateCodec.encode(JsonOps.INSTANCE, CARGO_CODEC, state).getAsJsonObject();
    }

    private static VehicleStateCodec.State<JsonElement> decode(JsonElement encoded) {
        return VehicleStateCodec.decode(JsonOps.INSTANCE, CARGO_CODEC, encoded);
    }

    private static VehicleStateCodec.State<JsonElement> roundtrip(VehicleStateCodec.State<JsonElement> state) {
        // Cross a serialization boundary; do not merely pass references to the decoder.
        return decode(JsonParser.parseString(encode(state).toString()));
    }

    @Test void fullCargoRoundtripRetainsComponentsAndEverySlot() {
        List<JsonElement> slots = new ArrayList<>();
        for (int i = 0; i < 54; i++) {
            slots.add(cargo(i % 2 == 0 ? "minecraft:wheat" : "minecraft:diamond", 64, "Cargo " + i));
        }
        VehicleStateCodec.State<JsonElement> original = new VehicleStateCodec.State<>(
                VehicleType.COMBINE_WIDE, 1731, 87, 14, false, 4, slots);
        JsonObject wire = encode(original);
        assertEquals(Set.of("Version", "Type", "Fuel", "Condition", "Color", "HeaderEnabled", "WorkCooldown", "Inventory"), wire.keySet());
        assertEquals(1, wire.get("Version").getAsInt());
        assertEquals("combine_wide", wire.get("Type").getAsString());
        VehicleStateCodec.State<JsonElement> result = roundtrip(original);
        assertEquals(original, result);
        assertEquals(54, result.cargo().size());
        for (int i = 0; i < 54; i++) {
            assertEquals(slots.get(i), result.cargo().get(i));
            assertEquals(64, result.cargo().get(i).getAsJsonObject().get("count").getAsInt());
            assertEquals("retained-Cargo " + i, result.cargo().get(i).getAsJsonObject()
                    .getAsJsonObject("components").getAsJsonObject("minecraft:custom_data").get("marker").getAsString());
        }
        slots.get(0).getAsJsonObject().addProperty("count", 63);
        assertEquals(64, result.cargo().get(0).getAsJsonObject().get("count").getAsInt());
        slots.clear();
        assertEquals(54, original.cargo().size());
    }

    @Test void emptySlotsAndBrokenConditionSurvive() {
        VehicleStateCodec.State<JsonElement> original = new VehicleStateCodec.State<>(
                VehicleType.DRONE, 0, 0, 0, true, 0,
                List.of(new JsonObject(), cargo("minecraft:wheat", 3, "Seed"), new JsonObject()));
        VehicleStateCodec.State<JsonElement> result = roundtrip(original);
        assertEquals(original, result);
        assertEquals(3, result.cargo().size());
        assertTrue(result.cargo().get(0).getAsJsonObject().entrySet().isEmpty());
        assertTrue(result.cargo().get(2).getAsJsonObject().entrySet().isEmpty());
        assertEquals(3, result.cargo().get(1).getAsJsonObject().get("count").getAsInt());
        assertEquals(0, result.condition());
        // Format-v1 defaults remain compatible with older partial records.
        JsonObject legacy = new JsonObject();
        legacy.addProperty("Type", "drone");
        VehicleStateCodec.State<JsonElement> defaults = decode(legacy);
        assertEquals(VehicleType.DRONE.durability, defaults.condition());
        assertTrue(defaults.headerEnabled());
        assertTrue(defaults.cargo().isEmpty());
    }

    @Test void rejectsUnknownSchemaAndType() {
        JsonObject bad = new JsonObject();
        bad.addProperty("Version", 999);
        assertThrows(IllegalArgumentException.class, () -> decode(bad));
        bad.addProperty("Version", 1);
        bad.addProperty("Type", "nonexistent");
        assertThrows(IllegalArgumentException.class, () -> decode(bad));
        bad.addProperty("Type", "drone");
        bad.addProperty("Inventory", "not-a-list");
        assertThrows(RuntimeException.class, () -> decode(bad));
        List<JsonElement> tooMany = new ArrayList<>();
        for (int i = 0; i < 55; i++) tooMany.add(new JsonObject());
        JsonObject oversized = encode(new VehicleStateCodec.State<>(VehicleType.DRONE, 0, 0, 0, true, 0, tooMany));
        assertThrows(IllegalArgumentException.class, () -> decode(oversized));
    }

    @Test void everyFamilyHasMultipleStableIds() {
        Set<String> ids = new HashSet<>();
        for (VehicleType type : VehicleType.values()) {
            assertTrue(ids.add(type.id));
            assertEquals(type, VehicleType.fromId(type.id));
            VehicleStateCodec.State<JsonElement> original = new VehicleStateCodec.State<>(
                    type, 17, type.durability, 15, true, 2, List.of());
            assertEquals(original, roundtrip(original));
        }
        for (VehicleType.Family family : VehicleType.Family.values()) {
            assertTrue(Arrays.stream(VehicleType.values()).filter(type -> type.family == family).count() >= 2);
        }
        assertEquals(2, VehicleType.MOTORCYCLE.seats);
        assertTrue(VehicleType.MOTORCYCLE.speed > VehicleType.PICKUP.speed);
        assertTrue(VehicleType.COMBINE.speed < VehicleType.PICKUP.speed);
    }
}
