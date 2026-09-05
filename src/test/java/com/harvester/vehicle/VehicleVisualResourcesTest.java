package com.harvester.vehicle;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Packaging contracts, NOT a client launch or proof that Mixin injection succeeds at runtime. */
class VehicleVisualResourcesTest {
    private static JsonObject resource(String path) throws Exception {
        try (var input=VehicleVisualResourcesTest.class.getResourceAsStream(path)) {
            assertNotNull(input,"Missing processed resource: "+path);
            return JsonParser.parseReader(new InputStreamReader(input,StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }
    @Test void passengerMixinsAreClientOnlyAndRequired() throws Exception {
        var mod=resource("/fabric.mod.json");
        boolean found=false;
        for(var entry:mod.getAsJsonArray("mixins")) {
            if(!entry.isJsonObject()) continue;
            var declaration=entry.getAsJsonObject();
            if(!declaration.get("config").getAsString().equals("harvester.client.mixins.json")) continue;
            found=true;
            assertEquals("client",declaration.get("environment").getAsString());
        }
        assertTrue(found,"Client mixin declaration must be present");
        var config=resource("/harvester.client.mixins.json");
        assertTrue(config.get("required").getAsBoolean());
        assertEquals("com.harvester.client.mixin",config.get("package").getAsString());
        assertEquals("JAVA_21",config.get("compatibilityLevel").getAsString());
        assertFalse(config.has("mixins"),"Client classes must not be in common mixins");
        Set<String> classes=new HashSet<>();
        for(var entry:config.getAsJsonArray("client")) classes.add(entry.getAsString());
        assertEquals(Set.of("PassengerRenderStateMixin","PassengerRendererMixin","PassengerBipedModelMixin"),classes);
        assertEquals(1,config.getAsJsonObject("injectors").get("defaultRequire").getAsInt());
    }
    @Test void pistonPlaceholderIsNotRestored() throws Exception {
        var sounds=resource("/assets/harvester/sounds.json");
        assertFalse(sounds.toString().contains("piston"));
        // Until real OGG audio is added, silence is intentional and explicitly documented.
        assertTrue(sounds.getAsJsonObject("engine").getAsJsonArray("sounds").isEmpty());
    }
}
