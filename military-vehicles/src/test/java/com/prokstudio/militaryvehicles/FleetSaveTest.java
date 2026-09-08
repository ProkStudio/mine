package com.prokstudio.militaryvehicles;
import com.prokstudio.militaryvehicles.core.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.*;
import com.mojang.serialization.*;
class FleetSaveTest {
    private static final Codec<VehicleSave<String>> CODEC=VehicleSaveCodec.create(Codec.STRING);
    private static VehicleSave<String> state(VehicleKind k) {
        List<String> cargo=new ArrayList<>();
        for(int i=0;i<k.cargoSlots();i++) cargo.add(i==0?"custom book / name / enchantments / payload":"slot-"+i);
        return new VehicleSave<>(1,k.id,k.tank-1,k.condition-1,9,cargo);
    }
    private static JsonObject json(VehicleSave<String> s) { return CODEC.encodeStart(JsonOps.INSTANCE,s).getOrThrow().getAsJsonObject(); }
    @Test void allThreeTypesRoundTripWithoutChangingCargoOrFuelRemainder() {
        for(var k:VehicleKind.values()) {var s=state(k);assertEquals(s,CODEC.parse(JsonOps.INSTANCE,json(s)).getOrThrow());}
    }
    @Test void legacyTruckVersionOneShapeRemainsReadableAndIdentical() {
        JsonArray cargo=new JsonArray();for(int i=0;i<27;i++) cargo.add("legacy-"+i);
        JsonObject fixture=JsonParser.parseString("{\"Version\":1,\"Type\":\"truck_6x6\",\"Fuel\":1357,\"Condition\":79,\"FuelTicks\":9}").getAsJsonObject();fixture.add("Inventory",cargo);
        var s=CODEC.parse(JsonOps.INSTANCE,fixture).getOrThrow();assertEquals(1357,s.fuel());assertEquals(79,s.condition());assertEquals(9,s.fuelTicks());assertEquals(fixture,json(s));
        assertEquals(Set.of("Version","Type","Fuel","Condition","FuelTicks","Inventory"),json(s).keySet());
    }
    @Test void crossTypeGuardRejectsBeforeStateCanBeApplied() {
        for(var from:VehicleKind.values()) for(var to:VehicleKind.values()) {
            var s=state(from);var before=json(s);
            if(from==to) assertDoesNotThrow(()->s.requireType(to));
            else assertThrows(IllegalArgumentException.class,()->s.requireType(to));
            assertEquals(before,json(s));
        }
    }
    @Test void perTypeBoundsAndWrongCargoSizesReturnCodecErrors() {
        for(var k:VehicleKind.values()) {
            var base=json(state(k));
            for(String field:List.of("Fuel","Condition","FuelTicks")) for(int invalid:new int[]{-1,field.equals("Fuel")?k.tank+1:field.equals("Condition")?k.condition+1:10}) {
                var bad=base.deepCopy();bad.addProperty(field,invalid);
                assertTrue(assertDoesNotThrow(()->CODEC.parse(JsonOps.INSTANCE,bad)).error().isPresent());
            }
            for(int size:new int[]{0,k.cargoSlots()-1,k.cargoSlots()+1,100}) {
                var bad=base.deepCopy();JsonArray cargo=new JsonArray();for(int i=0;i<size;i++) cargo.add("");bad.add("Inventory",cargo);
                assertTrue(assertDoesNotThrow(()->CODEC.parse(JsonOps.INSTANCE,bad)).error().isPresent());
            }
        }
    }
    @Test void unsupportedAndMalformedFieldsNeverBecomeAnEmptyVehicle() {
        for(var k:VehicleKind.values()) {
            for(String field:List.of("Version","Type","Fuel","Condition","FuelTicks","Inventory")) {
                var missing=json(state(k));missing.remove(field);assertTrue(CODEC.parse(JsonOps.INSTANCE,missing).error().isPresent());
                var malformed=json(state(k));malformed.add(field,new JsonObject());assertTrue(CODEC.parse(JsonOps.INSTANCE,malformed).error().isPresent());
            }
            var version=json(state(k));version.addProperty("Version",2);assertTrue(CODEC.parse(JsonOps.INSTANCE,version).error().isPresent());
            var type=json(state(k));type.addProperty("Type","tank");assertTrue(CODEC.parse(JsonOps.INSTANCE,type).error().isPresent());
        }
    }
    @Test void directSnapshotsOwnTheirListsAndValidateTheirProfile() {
        for(var k:VehicleKind.values()) {
            var cargo=new ArrayList<>(Collections.nCopies(k.cargoSlots(),"original"));
            var s=new VehicleSave<>(1,k.id,0,k.condition,0,cargo);cargo.set(0,"changed");assertEquals("original",s.cargo().getFirst());
            assertThrows(UnsupportedOperationException.class,()->s.cargo().set(0,"changed"));
            assertThrows(IllegalArgumentException.class,()->new VehicleSave<>(1,k.id,k.tank+1,k.condition,0,cargo));
        }
    }
}
