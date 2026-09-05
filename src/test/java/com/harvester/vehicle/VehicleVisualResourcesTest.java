package com.harvester.vehicle;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
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
        var manifest=resource("/assets/harvester/sounds/manifest.json");
        var english=resource("/assets/harvester/lang/en_us.json");
        var russian=resource("/assets/harvester/lang/ru_ru.json");
        Set<String> expected=Set.of("engine","motorcycle","boat","plane","helicopter","drone");
        assertEquals(expected,manifest.keySet());
        assertEquals(6,sounds.size());
        Set<String> referenced=new HashSet<>();
        for(var event:sounds.entrySet()) {
            var definition=event.getValue().getAsJsonObject();
            String subtitle=definition.get("subtitle").getAsString();
            assertTrue(english.has(subtitle),"Missing English subtitle: "+subtitle);
            assertTrue(russian.has(subtitle),"Missing Russian subtitle: "+subtitle);
            var entries=definition.getAsJsonArray("sounds");
            assertEquals(1,entries.size());
            var sound=entries.get(0).getAsJsonObject();
            String id=sound.get("name").getAsString();
            assertTrue(id.startsWith("harvester:")); referenced.add(id.substring(10));
            int distance=sound.get("attenuation_distance").getAsInt();
            assertTrue(distance>0 && distance<=32,"Unbounded audio distance");
        }
        assertEquals(expected,referenced);
        for(String name:expected) {
            try(var input=getClass().getResourceAsStream("/assets/harvester/sounds/"+name+".ogg")) {
                assertNotNull(input,"Missing OGG: "+name);
                byte[] data=input.readAllBytes();
                assertTrue(data.length>100);
                assertEquals("OggS",new String(data,0,4,StandardCharsets.US_ASCII));
                int identification=-1;
                for(int i=0;i<Math.min(128,data.length-16);i++) {
                    if(data[i]==1 && new String(data,i+1,6,StandardCharsets.US_ASCII).equals("vorbis")) { identification=i; break; }
                }
                assertTrue(identification>=0,"Missing Vorbis identification: "+name);
                assertEquals(1,data[identification+11],"Positional audio must be mono");
                assertEquals(16000,ByteBuffer.wrap(data,identification+12,4).order(ByteOrder.LITTLE_ENDIAN).getInt());
                String hash=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data));
                var metadata=manifest.getAsJsonObject(name);
                assertEquals(metadata.get("sha256").getAsString(),hash);
                assertEquals(metadata.get("bytes").getAsInt(),data.length);
                assertEquals(1,metadata.get("channels").getAsInt());
                assertEquals(16000,metadata.get("sampleRate").getAsInt());
                assertEquals(32000,metadata.get("samples").getAsInt());
            }
        }
    }
}
