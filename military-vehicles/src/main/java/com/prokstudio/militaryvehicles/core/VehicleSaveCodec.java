package com.prokstudio.militaryvehicles.core;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
/** Production supplies ItemStack.OPTIONAL_CODEC and registry-aware NBT ops. All validation failures are DataResults. */
public final class VehicleSaveCodec {
    private record Fields<C>(int version,String type,int fuel,int condition,int fuelTicks,List<C> cargo) {}
    private VehicleSaveCodec() {}
    public static <C> Codec<VehicleSave<C>> create(Codec<C> cargoCodec) {
        Codec<String> type=Codec.STRING.validate(s->VehicleKind.find(s).isPresent()?DataResult.success(s):DataResult.error(()->"Unsupported vehicle type"));
        int maxSlots=java.util.Arrays.stream(VehicleKind.values()).mapToInt(VehicleKind::cargoSlots).max().orElseThrow();
        int maxFuel=java.util.Arrays.stream(VehicleKind.values()).mapToInt(k->k.tank).max().orElseThrow();
        int maxCondition=java.util.Arrays.stream(VehicleKind.values()).mapToInt(k->k.condition).max().orElseThrow();
        var cargo=cargoCodec.listOf().validate(items->items.size()<=maxSlots?DataResult.success(items):DataResult.error(()->"Too many cargo slots"));
        Codec<Fields<C>> raw=RecordCodecBuilder.create(i->i.group(
            Codec.intRange(1,1).fieldOf("Version").forGetter(Fields<C>::version),
            type.fieldOf("Type").forGetter(Fields<C>::type),
            Codec.intRange(0,maxFuel).fieldOf("Fuel").forGetter(Fields<C>::fuel),
            Codec.intRange(0,maxCondition).fieldOf("Condition").forGetter(Fields<C>::condition),
            Codec.intRange(0,9).fieldOf("FuelTicks").forGetter(Fields<C>::fuelTicks),
            cargo.fieldOf("Inventory").forGetter(Fields<C>::cargo)).apply(i,Fields::new));
        return raw.flatXmap(fields->{
            try { return DataResult.success(new VehicleSave<>(fields.version(),fields.type(),fields.fuel(),fields.condition(),fields.fuelTicks(),fields.cargo())); }
            catch(IllegalArgumentException ex) { return DataResult.error(()->"Vehicle state is inconsistent with its type"); }
        },state->DataResult.success(new Fields<>(state.version(),state.type(),state.fuel(),state.condition(),state.fuelTicks(),state.cargo())));
    }
}
