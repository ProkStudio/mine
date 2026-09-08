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
import java.util.*;
public final class MilitaryContent {
    private static final Map<VehicleKind,EntityType<TruckEntity>> ENTITIES=new EnumMap<>(VehicleKind.class);
    private static final Map<VehicleKind,Item> VEHICLES=new EnumMap<>(VehicleKind.class);
    static {for(var kind:VehicleKind.values()) {ENTITIES.put(kind,registerVehicle(kind));VEHICLES.put(kind,registerVehicleItem(kind));}}
    public static final EntityType<TruckEntity> TRUCK_ENTITY=ENTITIES.get(VehicleKind.TRUCK);
    public static final EntityType<TruckEntity> BUGGY_ENTITY=ENTITIES.get(VehicleKind.BUGGY);
    public static final EntityType<TruckEntity> CARRIER_ENTITY=ENTITIES.get(VehicleKind.CARRIER);
    public static final Item TRUCK=VEHICLES.get(VehicleKind.TRUCK);
    public static final Item BUGGY=VEHICLES.get(VehicleKind.BUGGY);
    public static final Item CARRIER=VEHICLES.get(VehicleKind.CARRIER);
    public static final FuelCanItem FUEL_CAN=(FuelCanItem)register("fuel_can",new FuelCanItem(settings("fuel_can").maxCount(1)));
    public static final Item REPAIR_KIT=register("repair_kit",new Item(settings("repair_kit").maxCount(16)));
    public static final Item VEHICLE_FRAME=register("vehicle_frame",new Item(settings("vehicle_frame").maxCount(16)));
    public static final Item VEHICLE_SHELL=register("vehicle_shell",new Item(settings("vehicle_shell").maxCount(16)));
    private MilitaryContent() {}
    public static Identifier id(String path) { return Identifier.of(MilitaryVehicles.ID,path); }
    private static EntityType<TruckEntity> registerVehicle(VehicleKind kind) {
        RegistryKey<EntityType<?>> key=RegistryKey.of(RegistryKeys.ENTITY_TYPE,id(kind.id));
        return Registry.register(Registries.ENTITY_TYPE,key,
            EntityType.Builder.<TruckEntity>create((type,world)->new TruckEntity(type,world,kind),SpawnGroup.MISC)
                .dimensions(kind.width,kind.height).maxTrackingRange(10).trackingTickInterval(1).makeFireImmune().build(key));
    }
    private static Item registerVehicleItem(VehicleKind kind) {return register(kind.id,new TruckItem(settings(kind.id).maxCount(1).fireproof(),kind));}
    public static EntityType<TruckEntity> vehicleEntity(VehicleKind kind) {return Objects.requireNonNull(ENTITIES.get(kind));}
    public static Item vehicleItem(VehicleKind kind) {return Objects.requireNonNull(VEHICLES.get(kind));}
    private static Item.Settings settings(String name) { return new Item.Settings().registryKey(RegistryKey.of(RegistryKeys.ITEM,id(name))).component(DataComponentTypes.ITEM_MODEL,id(name)); }
    private static Item register(String name,Item item) { return Registry.register(Registries.ITEM,id(name),item); }
    public static void register() {
        Registry.register(Registries.ITEM_GROUP,id("vehicles"),FabricItemGroup.builder().displayName(Text.translatable("itemGroup.militaryvehicles"))
            .icon(()->new ItemStack(TRUCK)).entries((context,entries)->{
                for(var kind:VehicleKind.values()) entries.add(vehicleItem(kind));
                entries.add(FUEL_CAN);entries.add(REPAIR_KIT);entries.add(VEHICLE_FRAME);entries.add(VEHICLE_SHELL);
            }).build());
    }
}
