import com.prokstudio.militaryvehicles.core.VehicleKind;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Vanilla 1.21.11 recipe format. No packed vehicle is ever consumed as a crafting ingredient. */
public final class GenerateRecipes {
    private GenerateRecipes() {}
    private static void write(Path path,String text) throws Exception {Files.createDirectories(path.getParent());Files.writeString(path,text+"\n",StandardCharsets.UTF_8);}
    public static void generate(Path resources) throws Exception {
        Path root=resources.resolve("data/militaryvehicles");
        shaped(root,"vehicle_frame",1,List.of("ICI","RBR","ICI"),Map.of("I","minecraft:iron_block","C","minecraft:copper_block","R","minecraft:redstone","B","minecraft:blast_furnace"));
        shaped(root,"fuel_can",1,List.of(" I ","ICI","ICI"),Map.of("I","minecraft:iron_ingot","C","minecraft:coal"));
        shaped(root,"repair_kit",2,List.of(" II"," RC","II "),Map.of("I","minecraft:iron_ingot","R","minecraft:redstone","C","minecraft:copper_ingot"));
        shaped(root,"vehicle_shell",4,List.of(" I ","IGI"," I "),Map.of("I","minecraft:iron_ingot","G","minecraft:gunpowder"));
        Map<VehicleKind,String> modules=Map.of(VehicleKind.TRUCK,"minecraft:chest",VehicleKind.BUGGY,"minecraft:rabbit_hide",VehicleKind.CARRIER,"minecraft:iron_block",VehicleKind.TANK,"minecraft:obsidian",VehicleKind.TANKER,"minecraft:cauldron",VehicleKind.WORKSHOP,"minecraft:anvil",VehicleKind.RECOVERY,"minecraft:piston",VehicleKind.HOWITZER,"minecraft:dispenser");
        for(var kind:VehicleKind.values()) shaped(root,kind.id,1,List.of("MMM","IFI","IRI"),Map.of("M",modules.get(kind),"I","minecraft:iron_block","F","militaryvehicles:vehicle_frame","R","minecraft:redstone_block"));
        // Refilling deliberately consumes the input can (and any remainder), yielding exactly one full can.
        write(root.resolve("recipe/refill_fuel_can.json"),"{\"type\":\"minecraft:crafting_shapeless\",\"category\":\"misc\",\"ingredients\":[\"militaryvehicles:fuel_can\",\"minecraft:coal\",\"minecraft:coal\"],\"result\":{\"id\":\"militaryvehicles:fuel_can\",\"count\":1}}");
        List<String> recipes=new ArrayList<>(List.of("vehicle_frame","fuel_can","repair_kit","vehicle_shell","refill_fuel_can"));
        for(var kind:VehicleKind.values()) recipes.add(kind.id);
        String rewards=String.join(",",recipes.stream().map(s->"\"militaryvehicles:"+s+"\"").toList());
        write(root.resolve("advancement/recipes/field_manual.json"),"{\"criteria\":{\"has_iron\":{\"trigger\":\"minecraft:inventory_changed\",\"conditions\":{\"items\":[{\"items\":\"minecraft:iron_ingot\"}]}}},\"rewards\":{\"recipes\":["+rewards+"]}}");
    }
    private static void shaped(Path root,String name,int count,List<String> pattern,Map<String,String> keys) throws Exception {
        String p=String.join(",",pattern.stream().map(s->"\""+s+"\"").toList());
        String key=String.join(",",new TreeMap<>(keys).entrySet().stream().map(e->"\""+e.getKey()+"\":\""+e.getValue()+"\"").toList());
        write(root.resolve("recipe/"+name+".json"),"{\"type\":\"minecraft:crafting_shaped\",\"category\":\"misc\",\"pattern\":["+p+"],\"key\":{"+key+"},\"result\":{\"id\":\"militaryvehicles:"+name+"\",\"count\":"+count+"}}");
    }
}
