package com.prokstudio.militaryvehicles.init;
import com.prokstudio.militaryvehicles.MilitaryVehicles;
import com.prokstudio.militaryvehicles.core.TruckSpec;
import com.prokstudio.militaryvehicles.entity.TruckEntity;
import com.prokstudio.militaryvehicles.item.FuelCanItem;
import com.prokstudio.militaryvehicles.item.TruckItem;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.*;
import net.minecraft.item.*;
import net.minecraft.registry.*;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
public final class MilitaryContent {
    private static final RegistryKey<EntityType<?>> TRUCK_KEY=RegistryKey.of(RegistryKeys.ENTITY_TYPE,id(TruckSpec.ID));
    public static final EntityType<TruckEntity> TRUCK_ENTITY=Registry.register(Registries.ENTITY_TYPE,TRUCK_KEY,
        EntityType.Builder.<TruckEntity>create(TruckEntity::new,SpawnGroup.MISC).dimensions(TruckSpec.WIDTH,TruckSpec.HEIGHT)
            .maxTrackingRange(10).trackingTickInterval(1).makeFireImmune().build(TRUCK_KEY));
    public static final Item TRUCK=register(TruckSpec.ID,new TruckItem(settings(TruckSpec.ID).maxCount(1).fireproof()));
    public static final FuelCanItem FUEL_CAN=(FuelCanItem)register("fuel_can",new FuelCanItem(settings("fuel_can").maxCount(1)));
    public static final Item REPAIR_KIT=register("repair_kit",new Item(settings("repair_kit").maxCount(16)));
    private MilitaryContent() {}
    public static Identifier id(String path) { return Identifier.of(MilitaryVehicles.ID,path); }
    private static Item.Settings settings(String name) { return new Item.Settings().registryKey(RegistryKey.of(RegistryKeys.ITEM,id(name))).component(DataComponentTypes.ITEM_MODEL,id(name)); }
    private static Item register(String name,Item item) { return Registry.register(Registries.ITEM,id(name),item); }
    public static void register() {
        Registry.register(Registries.ITEM_GROUP,id("vehicles"),FabricItemGroup.builder().displayName(Text.translatable("itemGroup.militaryvehicles"))
            .icon(()->new ItemStack(TRUCK)).entries((context,entries)->{entries.add(TRUCK);entries.add(FUEL_CAN);entries.add(REPAIR_KIT);}).build());
    }
}
