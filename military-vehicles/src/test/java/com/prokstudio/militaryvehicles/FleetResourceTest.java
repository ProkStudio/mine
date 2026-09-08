package com.prokstudio.militaryvehicles;
import com.prokstudio.militaryvehicles.core.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.*;
import java.nio.file.*;
class FleetResourceTest {
    private static final Path ROOT=Path.of("build/resources/main/assets/militaryvehicles");
    private JsonObject read(String path) throws Exception {return JsonParser.parseString(Files.readString(ROOT.resolve(path))).getAsJsonObject();}
    @Test void allFiveItemsHaveModernDefinitionsAndDistinctBoundedModels() throws Exception {
        // Historical method name retained; all twelve current item definitions are required.
        Set<String> expected=new HashSet<>(Set.of("truck_6x6","scout_buggy","carrier_8x8","warden_tank","fuel_tanker","field_workshop","recovery_vehicle","bastion_howitzer","fuel_can","repair_kit","vehicle_frame","vehicle_shell"));
        try(var paths=Files.list(ROOT.resolve("items"))) {assertEquals(expected,new HashSet<>(paths.map(p->p.getFileName().toString().replace(".json","")).toList()));}
        Set<JsonArray> shapes=new HashSet<>();
        for(String id:expected) {
            var definition=read("items/"+id+".json").getAsJsonObject("model");assertEquals("minecraft:model",definition.get("type").getAsString());assertEquals("militaryvehicles:item/"+id,definition.get("model").getAsString());
            var model=read("models/item/"+id+".json");var elements=model.getAsJsonArray("elements");assertTrue(shapes.add(elements));
            for(var value:elements) {var e=value.getAsJsonObject();for(int axis=0;axis<3;axis++) {double lo=e.getAsJsonArray("from").get(axis).getAsDouble(),hi=e.getAsJsonArray("to").get(axis).getAsDouble();assertTrue(0<=lo&&lo<hi&&hi<=16);}}
            for(var value:model.getAsJsonObject("textures").entrySet()) {String texture=value.getValue().getAsString();assertTrue(texture.startsWith("militaryvehicles:item/"));assertTrue(Files.size(ROOT.resolve("textures/"+texture.split(":",2)[1]+".png"))>0);}
        }
    }
    @Test void fleetManifestMatchesTheRuntimeProfilesAndMeshCounts() throws Exception {
        var manifest=read("fleet.json");assertEquals(1,manifest.get("schemaVersion").getAsInt());assertEquals(8,manifest.getAsJsonArray("vehicles").size());Set<String> found=new HashSet<>();
        for(var value:manifest.getAsJsonArray("vehicles")) {
            var e=value.getAsJsonObject();var k=VehicleKind.require(e.get("id").getAsString());assertTrue(found.add(k.id));
            assertEquals(k.seats.size(),e.get("seats").getAsInt());assertEquals(k.cargoSlots(),e.get("cargoSlots").getAsInt());assertEquals(k.tank,e.get("tank").getAsInt());assertEquals(k.condition,e.get("condition").getAsInt());
            assertEquals(k.wheels,e.get("wheels").getAsInt());assertEquals(k.steeringWheels,e.get("steeringWheels").getAsInt());assertEquals(k.soundId,e.get("sound").getAsString());
            assertEquals(k.handling.forward(),e.get("forward").getAsDouble(),1e-6);assertEquals(k.handling.reverse(),e.get("reverse").getAsDouble(),1e-6);
            assertEquals(VehicleGeometry.create(k).size(),e.get("parts").getAsInt());assertEquals(VehicleGeometry.create(k).stream().mapToInt(p->p.boxes().size()).sum(),e.get("boxes").getAsInt());
        }
    }
    @Test void bothLanguagesNameEveryRegisteredVehicleAndCapacityTooltip() throws Exception {
        var en=read("lang/en_us.json");var ru=read("lang/ru_ru.json");assertEquals(en.keySet(),ru.keySet());
        for(var k:VehicleKind.values()) for(String prefix:List.of("item.","entity.")) {
            String key=prefix+"militaryvehicles."+k.id;assertTrue(en.has(key));assertTrue(ru.has(key));assertFalse(ru.get(key).getAsString().isBlank());
        }
        assertTrue(en.has("tooltip.militaryvehicles.capacity"));assertTrue(ru.get("container.militaryvehicles.cargo").getAsString().contains("%s"));
    }
    @Test void generatedManifestKeepsOriginalTruckAndNewTypesSeparate() throws Exception {
        Map<String,JsonObject> vehicles=new HashMap<>();for(var v:read("fleet.json").getAsJsonArray("vehicles")) {var o=v.getAsJsonObject();vehicles.put(o.get("id").getAsString(),o);}
        assertEquals(27,vehicles.get("truck_6x6").get("cargoSlots").getAsInt());assertEquals(9,vehicles.get("scout_buggy").get("cargoSlots").getAsInt());assertEquals(18,vehicles.get("carrier_8x8").get("cargoSlots").getAsInt());
        assertEquals(6,vehicles.get("carrier_8x8").get("seats").getAsInt());assertEquals(4,vehicles.get("scout_buggy").get("wheels").getAsInt());assertEquals(8,vehicles.get("carrier_8x8").get("wheels").getAsInt());
    }
}
