package com.prokstudio.militaryvehicles;
import com.google.gson.*;
import org.junit.jupiter.api.Test;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class ResourceTest {
    private JsonObject read(String path) throws Exception { return JsonParser.parseString(Files.readString(Path.of("build/resources/main",path))).getAsJsonObject(); }
    @Test void languagesHaveMatchingKeysAndPlaceholders() throws Exception {
        String b="assets/militaryvehicles/lang/";var ru=read(b+"ru_ru.json");var en=read(b+"en_us.json");assertEquals(en.keySet(),ru.keySet());
        for(String k:en.keySet()) assertEquals(en.get(k).getAsString().split("%s",-1).length,ru.get(k).getAsString().split("%s",-1).length,k);
    }
    @Test void registeredItemsHaveModernDefinitionsAndTextures() throws Exception {
        for(String id:List.of("truck_6x6","fuel_can","repair_kit")) {
            var def=read("assets/militaryvehicles/items/"+id+".json");assertEquals("minecraft:model",def.getAsJsonObject("model").get("type").getAsString());
            var model=read("assets/militaryvehicles/models/item/"+id+".json");assertFalse(model.getAsJsonArray("elements").isEmpty());
            for(var texture:model.getAsJsonObject("textures").entrySet()) { String path=texture.getValue().getAsString().replace("militaryvehicles:","assets/militaryvehicles/textures/")+".png";assertTrue(path.contains("textures/item/"));assertNotNull(javax.imageio.ImageIO.read(Path.of("build/resources/main",path).toFile())); }
        }
    }
    @Test void manifestPinsTargetAndDoesNotRequireOtherMods() throws Exception {
        var m=read("fabric.mod.json");assertEquals("militaryvehicles",m.get("id").getAsString());var d=m.getAsJsonObject("depends");
        assertEquals("=1.21.11",d.get("minecraft").getAsString());assertEquals(">=0.19.5",d.get("fabricloader").getAsString());assertFalse(d.has("harvester"));assertFalse(d.has("military_arsenal"));assertEquals("CC0-1.0",m.get("license").getAsString());
    }
}
