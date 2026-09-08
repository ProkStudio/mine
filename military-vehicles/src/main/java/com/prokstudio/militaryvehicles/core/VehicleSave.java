package com.prokstudio.militaryvehicles.core;
import java.util.List;
/** v1 layout retained. Type determines strict capacity/ranges; no passengers, position, input or running engine. */
public record VehicleSave<C>(int version,String type,int fuel,int condition,int fuelTicks,List<C> cargo) {
    public static final int VERSION=1;
    public VehicleSave {
        if(version!=VERSION) throw new IllegalArgumentException("Unknown vehicle format");
        VehicleKind kind=VehicleKind.require(type);
        if(fuel<0||fuel>kind.tank||condition<0||condition>kind.condition||fuelTicks<0||fuelTicks>9)
            throw new IllegalArgumentException("Invalid durable state");
        cargo=List.copyOf(cargo);
        if(cargo.size()!=kind.cargoSlots()) throw new IllegalArgumentException("Cargo capacity does not match vehicle type");
    }
    /** Cross-type placement/load must fail before any entity or inventory mutation. */
    public void requireType(VehicleKind expected) {
        if(!expected.id.equals(type)) throw new IllegalArgumentException("Saved vehicle type does not match entity/item");
    }
}
