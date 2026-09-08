package com.prokstudio.militaryvehicles;

import com.prokstudio.militaryvehicles.core.VehicleKind;
import com.google.gson.*;
import org.junit.jupiter.api.Test;
import java.nio.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.stream.Collectors;
import static org.junit.jupiter.api.Assertions.*;

class AudioResourceTest {
    private static final Path ROOT=Path.of("build/resources/main/assets/militaryvehicles");
    private JsonObject read(String name) throws Exception {return JsonParser.parseString(Files.readString(ROOT.resolve(name))).getAsJsonObject();}
    private Set<String> sounds() {return Arrays.stream(VehicleKind.values()).map(k->k.soundId).collect(Collectors.toSet());}
    @Test void registeredLoopHasLocalizedSubtitle() throws Exception {
        var all=read("sounds.json");assertEquals(sounds(),all.keySet());
        for(String name:sounds()) {
            var loop=all.getAsJsonObject(name);String subtitle=loop.get("subtitle").getAsString();
            assertTrue(read("lang/en_us.json").has(subtitle));assertTrue(read("lang/ru_ru.json").has(subtitle));
            var samples=loop.getAsJsonArray("sounds");assertEquals(1,samples.size());var sound=samples.get(0).getAsJsonObject();
            assertEquals("militaryvehicles:"+name,sound.get("name").getAsString());assertEquals(32,sound.get("attenuation_distance").getAsInt());
        }
    }
    @Test void generatedLoopIsMonoVorbisAndMatchesManifest() throws Exception {
        var manifest=read("sounds/manifest.json");assertEquals(sounds(),manifest.keySet());Set<String> hashes=new HashSet<>();
        for(String name:sounds()) {
            byte[] data=Files.readAllBytes(ROOT.resolve("sounds/"+name+".ogg"));
            assertEquals("OggS",new String(data,0,4,StandardCharsets.US_ASCII));int id=-1;
            for(int i=0;i<Math.min(128,data.length-16);i++) if(data[i]==1&&new String(data,i+1,6,StandardCharsets.US_ASCII).equals("vorbis")){id=i;break;}
            assertTrue(id>=0);assertEquals(1,data[id+11]);assertEquals(32000,ByteBuffer.wrap(data,id+12,4).order(ByteOrder.LITTLE_ENDIAN).getInt());
            var m=manifest.getAsJsonObject(name);String hash=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data));
            assertEquals(hash,m.get("sha256").getAsString());assertTrue(hashes.add(hash),"profiles must not be duplicate clips");
            assertEquals(data.length,m.get("bytes").getAsInt());assertEquals(128000,m.get("samples").getAsInt());assertEquals(1,m.get("channels").getAsInt());assertEquals(32000,m.get("sampleRate").getAsInt());
            assertTrue(m.get("rms").getAsDouble()>=.07&&m.get("rms").getAsDouble()<=.20);
            assertTrue(m.get("peak").getAsDouble()>0&&m.get("peak").getAsDouble()<.95);assertTrue(m.get("seamDelta").getAsDouble()>=0&&m.get("seamDelta").getAsDouble()<=.06);assertTrue(m.get("dcOffset").getAsDouble()>=0&&m.get("dcOffset").getAsDouble()<=.002);
        }
        assertTrue(Files.readString(ROOT.resolve("sounds/LICENSE.txt")).contains("CC0-1.0"));
    }
    @Test void encoderAndRawPcmAreNotRuntimeResources() throws Exception {
        Set<String> expected=sounds().stream().map(s->s+".ogg").collect(Collectors.toSet());expected.add("manifest.json");expected.add("LICENSE.txt");
        try(var files=Files.walk(ROOT.resolve("sounds"))) {assertEquals(expected,new HashSet<>(files.filter(Files::isRegularFile).map(p->p.getFileName().toString()).toList()));}
    }
}
