package com.harvester.init;

import com.harvester.HarvesterMod;
import com.harvester.item.*;
import com.harvester.vehicle.VehicleType;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.*;
import net.minecraft.registry.*;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import java.util.*;

public final class ModItems {
    private static final Map<VehicleType,Item> VEHICLES=new EnumMap<>(VehicleType.class);
    static {
        for(VehicleType type:VehicleType.values()) VEHICLES.put(type,register(type.id,
            new VehicleItem(settings(type.id).maxCount(1).fireproof(),type)));
    }
    public static final Item COMBINE_SPAWN_EGG=VEHICLES.get(VehicleType.COMBINE);
    public static final Item FUEL_SMALL=register("fuel_can_small",new FuelCanItem(settings("fuel_can_small").maxCount(1),0));
    public static final Item FUEL_MEDIUM=register("fuel_can_medium",new FuelCanItem(settings("fuel_can_medium").maxCount(1),1));
    public static final Item FUEL_LARGE=register("fuel_can_large",new FuelCanItem(settings("fuel_can_large").maxCount(1),2));
    public static final Item REPAIR_KIT=register("repair_kit",new Item(settings("repair_kit").maxCount(16)));
    public static final Item PAINT=register("paint",new Item(settings("paint").maxCount(16)));
    private static Item.Settings settings(String id) {
        return new Item.Settings().registryKey(RegistryKey.of(RegistryKeys.ITEM,Identifier.of("harvester",id)))
            .component(DataComponentTypes.ITEM_MODEL,Identifier.of("harvester",id));
    }
    private static Item register(String id,Item item) { return Registry.register(Registries.ITEM,Identifier.of(HarvesterMod.MOD_ID,id),item); }
    public static Item vehicle(VehicleType type) { return VEHICLES.get(type); }
    public static void register() {
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register(entries->{
            for(VehicleType type:VehicleType.values()) entries.add(VEHICLES.get(type));
            entries.add(FUEL_SMALL); entries.add(FUEL_MEDIUM); entries.add(FUEL_LARGE); entries.add(REPAIR_KIT); entries.add(PAINT);
        });
        Registry.register(Registries.ITEM_GROUP,Identifier.of("harvester","vehicles"),FabricItemGroup.builder()
            .icon(()->new ItemStack(COMBINE_SPAWN_EGG))
            .displayName(Text.literal("Harvester — техника и заправка"))
            .entries((context,entries)->{
                entries.add(FUEL_SMALL); entries.add(FUEL_MEDIUM); entries.add(FUEL_LARGE); entries.add(REPAIR_KIT); entries.add(PAINT);
                for(VehicleType type:VehicleType.values()) entries.add(VEHICLES.get(type));
            }).build());
    }
}
