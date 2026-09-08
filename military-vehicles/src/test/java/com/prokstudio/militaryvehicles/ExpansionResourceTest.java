package com.prokstudio.militaryvehicles;
import com.google.gson.*;
import com.prokstudio.militaryvehicles.core.VehicleKind;
import org.junit.jupiter.api.Test;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class ExpansionResourceTest {
    private static final Path ROOT=Path.of("build/resources/main");
    private JsonObject read(String path) throws Exception {return JsonParser.parseString(Files.readString(ROOT.resolve(path))).getAsJsonObject();}
    private Set<String> recipeIds() {Set<String> ids=new HashSet<>(Set.of("vehicle_frame","vehicle_shell","fuel_can","repair_kit","refill_fuel_can"));for(var k:VehicleKind.values())ids.add(k.id);return ids;}
    @Test void everyVehicleAndSupplyHasOneBoundedSurvivalRecipe() throws Exception {
        try(var files=Files.list(ROOT.resolve("data/militaryvehicles/recipe"))) {assertEquals(recipeIds(),new HashSet<>(files.map(p->p.getFileName().toString().replace(".json","")).toList()));}
        for(String id:recipeIds()) {
            var recipe=read("data/militaryvehicles/recipe/"+id+".json");assertEquals("militaryvehicles:"+(id.equals("refill_fuel_can")?"fuel_can":id),recipe.getAsJsonObject("result").get("id").getAsString());
            int count=recipe.getAsJsonObject("result").get("count").getAsInt();assertTrue(count>=1&&count<=4);
            if(recipe.has("pattern")) {assertEquals("minecraft:crafting_shaped",recipe.get("type").getAsString());var pattern=recipe.getAsJsonArray("pattern");assertEquals(3,pattern.size());Set<String> used=new HashSet<>();
                for(var row:pattern) {assertEquals(3,row.getAsString().length());for(char c:row.getAsString().toCharArray())if(c!=' ')used.add(String.valueOf(c));}
                assertEquals(used,recipe.getAsJsonObject("key").keySet());
            } else assertEquals("minecraft:crafting_shapeless",recipe.get("type").getAsString());
        }
    }
    @Test void craftingNeverConsumesPackedVehiclesOrDuplicatesCans() throws Exception {
        Set<String> vehicles=new HashSet<>();for(var k:VehicleKind.values())vehicles.add("militaryvehicles:"+k.id);
        for(String id:recipeIds()) {var recipe=read("data/militaryvehicles/recipe/"+id+".json");
            if(recipe.has("key"))for(var entry:recipe.getAsJsonObject("key").entrySet())assertFalse(vehicles.contains(entry.getValue().getAsString()));
            if(recipe.has("ingredients"))for(var ingredient:recipe.getAsJsonArray("ingredients"))assertFalse(vehicles.contains(ingredient.getAsString()));
        }
        var refill=read("data/militaryvehicles/recipe/refill_fuel_can.json");assertEquals(1,refill.getAsJsonObject("result").get("count").getAsInt());
        assertEquals(JsonParser.parseString("[\"militaryvehicles:fuel_can\",\"minecraft:coal\",\"minecraft:coal\"]"),refill.get("ingredients"));
    }
    @Test void recipeBookUnlockCoversExactCraftingSet() throws Exception {
        var advancement=read("data/militaryvehicles/advancement/recipes/field_manual.json");var unlock=advancement.getAsJsonObject("rewards").getAsJsonArray("recipes");Set<String> expected=new HashSet<>();for(String id:recipeIds())expected.add("militaryvehicles:"+id);
        Set<String> actual=new HashSet<>();for(var id:unlock)assertTrue(actual.add(id.getAsString()));assertEquals(expected,actual);
        assertEquals("minecraft:inventory_changed",advancement.getAsJsonObject("criteria").getAsJsonObject("has_iron").get("trigger").getAsString());
    }
    @Test void localizedRoleHelpAndRemappableKeysCoverTheFleet() throws Exception {
        for(String lang:List.of("ru_ru","en_us")) {var text=read("assets/militaryvehicles/lang/"+lang+".json");
            for(var value:text.entrySet())assertFalse(value.getValue().getAsString().contains("\ufffd"));
            for(var k:VehicleKind.values())assertFalse(text.get("help.militaryvehicles."+k.id).getAsString().isBlank());
            for(String key:List.of("engine","action","deploy","crew","help"))assertTrue(text.has("key.militaryvehicles."+key));
            for(String item:List.of("vehicle_frame","vehicle_shell"))assertTrue(text.has("item.militaryvehicles."+item));
        }
    }
    @Test void generatedRoleFlagsMatchProductionProfiles() throws Exception {
        for(var item:read("assets/militaryvehicles/fleet.json").getAsJsonArray("vehicles")) {var v=item.getAsJsonObject();var k=VehicleKind.require(v.get("id").getAsString());
            assertEquals(k.tracked(),v.get("tracked").getAsBoolean());assertEquals(k.armed(),v.get("armed").getAsBoolean());assertEquals(k.support(),v.get("support").getAsBoolean());
        }
    }
}
