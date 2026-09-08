package com.prokstudio.militaryvehicles;
import com.google.gson.*;
import com.mojang.serialization.*;
import com.prokstudio.militaryvehicles.core.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class VehicleSaveTest {
    private final Codec<VehicleSave<String>> codec=VehicleSaveCodec.create(Codec.STRING);
    private VehicleSave<String> sample() { List<String> items=new ArrayList<>(Collections.nCopies(27,""));items.set(0,"custom-name;components;damage=3");items.set(26,"last slot");return new VehicleSave<>(1,"truck_6x6",1357,79,9,items); }
    private JsonObject json() { return codec.encodeStart(JsonOps.INSTANCE,sample()).getOrThrow().getAsJsonObject(); }
    @Test void roundTripPreservesEverySlotAndFractionalFuel() { assertEquals(sample(),codec.parse(JsonOps.INSTANCE,json()).getOrThrow()); }
    @Test void unsupportedVersionAndTypeFailClosed() { var v=json();v.addProperty("Version",2);assertTrue(codec.parse(JsonOps.INSTANCE,v).error().isPresent());var t=json();t.addProperty("Type","tank");assertTrue(codec.parse(JsonOps.INSTANCE,t).error().isPresent()); }
    @Test void missingOrMalformedCargoCannotBecomeEmptyTruck() {
        var m=json();m.remove("Inventory");assertTrue(codec.parse(JsonOps.INSTANCE,m).error().isPresent());var s=json();s.add("Inventory",new JsonArray());assertTrue(codec.parse(JsonOps.INSTANCE,s).error().isPresent());
        var b=json();b.getAsJsonArray("Inventory").set(0,new JsonObject());assertTrue(codec.parse(JsonOps.INSTANCE,b).error().isPresent());
    }
    @Test void invalidNumbersRejectedRatherThanSilentlyClamped() {
        for(String key:List.of("Fuel","Condition","FuelTicks")) { var n=json();n.addProperty(key,-1);assertTrue(codec.parse(JsonOps.INSTANCE,n).error().isPresent());var h=json();h.addProperty(key,1000000);assertTrue(codec.parse(JsonOps.INSTANCE,h).error().isPresent()); }
    }
    @Test void inputAndRunningEngineAreNotPersisted() { assertEquals(Set.of("Version","Type","Fuel","Condition","FuelTicks","Inventory"),json().keySet()); }
    @Test void snapshotsOwnTheirCargoList() {
        var items=new ArrayList<>(Collections.nCopies(27,""));var s=new VehicleSave<>(1,"truck_6x6",0,200,0,items);items.set(0,"changed");assertEquals("",s.cargo().get(0));assertThrows(UnsupportedOperationException.class,()->s.cargo().set(0,"changed"));
    }
}
