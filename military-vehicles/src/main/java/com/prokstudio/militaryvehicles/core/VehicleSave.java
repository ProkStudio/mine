package com.prokstudio.militaryvehicles.core;
import java.util.List;
/** Only durable state; no passengers, input, position, velocity or running engine. */
public record VehicleSave<C>(int version,String type,int fuel,int condition,int fuelTicks,List<C> cargo) {
    public static final int VERSION=1;
    public VehicleSave {
        if (version!=VERSION||!TruckSpec.ID.equals(type)) throw new IllegalArgumentException("Unknown vehicle format/type");
        if (fuel<0||fuel>TruckSpec.TANK||condition<0||condition>TruckSpec.CONDITION||fuelTicks<0||fuelTicks>9)
            throw new IllegalArgumentException("Invalid durable state");
        cargo=List.copyOf(cargo);
        if (cargo.size()!=TruckSpec.SLOTS) throw new IllegalArgumentException("Expected 27 cargo slots");
    }
}
