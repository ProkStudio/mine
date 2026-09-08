package com.prokstudio.militaryvehicles.core;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
/** Production supplies ItemStack.OPTIONAL_CODEC and registry-aware NBT ops. */
public final class VehicleSaveCodec {
    private VehicleSaveCodec() {}
    public static <C> Codec<VehicleSave<C>> create(Codec<C> cargoCodec) {
        Codec<String> type=Codec.STRING.validate(s->TruckSpec.ID.equals(s)?DataResult.success(s):DataResult.error(()->"Unsupported vehicle type"));
        var cargo=cargoCodec.listOf().validate(items->items.size()==TruckSpec.SLOTS?DataResult.success(items):DataResult.error(()->"Expected 27 cargo slots"));
        return RecordCodecBuilder.create(i->i.group(
            Codec.intRange(1,1).fieldOf("Version").forGetter(VehicleSave<C>::version),
            type.fieldOf("Type").forGetter(VehicleSave<C>::type),
            Codec.intRange(0,TruckSpec.TANK).fieldOf("Fuel").forGetter(VehicleSave<C>::fuel),
            Codec.intRange(0,TruckSpec.CONDITION).fieldOf("Condition").forGetter(VehicleSave<C>::condition),
            Codec.intRange(0,9).fieldOf("FuelTicks").forGetter(VehicleSave<C>::fuelTicks),
            cargo.fieldOf("Inventory").forGetter(VehicleSave<C>::cargo)).apply(i,VehicleSave::new));
    }
}
