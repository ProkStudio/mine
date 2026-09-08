package com.prokstudio.militaryvehicles.init;
import com.prokstudio.militaryvehicles.MilitaryVehicles;
import com.prokstudio.militaryvehicles.core.VehicleKind;
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
    public static final EntityType<TruckEntity> TRUCK_ENTITY=registerVehicle(VehicleKind.TRUCK);
    public static final EntityType<TruckEntity> BUGGY_ENTITY=registerVehicle(VehicleKind.BUGGY);
    public static final EntityType<TruckEntity> CARRIER_ENTITY=registerVehicle(VehicleKind.CARRIER);
    public static final Item TRUCK=registerVehicleItem(VehicleKind.TRUCK);
    public static final Item BUGGY=registerVehicleItem(VehicleKind.BUGGY);
    public static final Item CARRIER=registerVehicleItem(VehicleKind.CARRIER);
    public static final FuelCanItem FUEL_CAN=(FuelCanItem)register("fuel_can",new FuelCanItem(settings("fuel_can").maxCount(1)));
    public static final Item REPAIR_KIT=register("repair_kit",new Item(settings("repair_kit").maxCount(16)));
    private MilitaryContent() {}
    public static Identifier id(String path) { return Identifier.of(MilitaryVehicles.ID,path); }
    private static EntityType<TruckEntity> registerVehicle(VehicleKind kind) {
        RegistryKey<EntityType<?>> key=RegistryKey.of(RegistryKeys.ENTITY_TYPE,id(kind.id));
        return Registry.register(Registries.ENTITY_TYPE,key,
            EntityType.Builder.<TruckEntity>create((type,world)->new TruckEntity(type,world,kind),SpawnGroup.MISC)
                .dimensions(kind.width,kind.height).maxTrackingRange(10).trackingTickInterval(1).makeFireImmune().build(key));
    }
    private static Item registerVehicleItem(VehicleKind kind) {
        return register(kind.id,new TruckItem(settings(kind.id).maxCount(1).fireproof(),kind));
    }
    public static EntityType<TruckEntity> vehicleEntity(VehicleKind kind) {
        return switch(kind) { case TRUCK->TRUCK_ENTITY;case BUGGY->BUGGY_ENTITY;case CARRIER->CARRIER_ENTITY; };
    }
    public static Item vehicleItem(VehicleKind kind) {
        return switch(kind) { case TRUCK->TRUCK;case BUGGY->BUGGY;case CARRIER->CARRIER; };
    }
    private static Item.Settings settings(String name) { return new Item.Settings().registryKey(RegistryKey.of(RegistryKeys.ITEM,id(name))).component(DataComponentTypes.ITEM_MODEL,id(name)); }
    private static Item register(String name,Item item) { return Registry.register(Registries.ITEM,id(name),item); }
    public static void register() {
        Registry.register(Registries.ITEM_GROUP,id("vehicles"),FabricItemGroup.builder().displayName(Text.translatable("itemGroup.militaryvehicles"))
            .icon(()->new ItemStack(TRUCK)).entries((context,entries)->{
                for(var kind:VehicleKind.values()) entries.add(vehicleItem(kind));
                entries.add(FUEL_CAN);entries.add(REPAIR_KIT);
            }).build());
    }
}
