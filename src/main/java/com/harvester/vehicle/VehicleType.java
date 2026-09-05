package com.harvester.vehicle;

/** Stable string IDs are persisted; enum ordinals are only used for live tracking. */
public enum VehicleType {
    COMBINE("combine_spawn_egg", Family.COMBINE, "Комбайн", .15, 1600, 100, 27, 1, 1, 3.8f, 2.8f),
    COMBINE_WIDE("combine_wide", Family.COMBINE, "Комбайн широкий", .12, 2400, 140, 54, 2, 1, 4.8f, 2.8f),
    DOZER("bulldozer", Family.DOZER, "Бульдозер", .13, 2000, 160, 27, 1, 1, 3f, 2.5f),
    DOZER_HEAVY("bulldozer_heavy", Family.DOZER, "Бульдозер тяжёлый", .10, 3000, 220, 54, 2, 1, 4f, 2.8f),
    PICKUP("pickup", Family.PICKUP, "Пикап", .32, 1800, 100, 27, 0, 1, 2f, 1.8f),
    PICKUP_CARGO("pickup_cargo", Family.PICKUP, "Пикап грузовой", .28, 2400, 130, 54, 0, 1, 2.2f, 1.9f),
    MOTORCYCLE("motorcycle", Family.MOTORCYCLE, "Мотоцикл", .38, 1000, 70, 9, 0, 2, .9f, 1.3f),
    MOTORCYCLE_TOURING("motorcycle_touring", Family.MOTORCYCLE, "Мотоцикл туристический", .36, 1600, 90, 18, 0, 2, 1f, 1.4f),
    BOAT("motorboat", Family.BOAT, "Катер", .44, 1800, 110, 18, 0, 1, 2f, 1.2f),
    BOAT_CARGO("motorboat_cargo", Family.BOAT, "Катер грузовой", .42, 2600, 150, 36, 0, 1, 2.5f, 1.4f),
    PLANE("plane", Family.PLANE, "Самолёт", .42, 2400, 120, 18, 0, 1, 3.2f, 1.8f),
    PLANE_CARGO("plane_cargo", Family.PLANE, "Самолёт грузовой", .38, 3200, 160, 36, 0, 1, 4f, 2f),
    HELICOPTER("helicopter", Family.HELICOPTER, "Вертолёт", .30, 2400, 120, 18, 0, 1, 2.6f, 2.4f),
    HELICOPTER_CARGO("helicopter_cargo", Family.HELICOPTER, "Вертолёт грузовой", .26, 3200, 160, 36, 0, 1, 3f, 2.6f),
    DRONE("drone", Family.DRONE, "Дрон", .24, 800, 50, 9, 0, 1, 1.3f, .8f),
    DRONE_CARGO("drone_cargo", Family.DRONE, "Дрон грузовой", .20, 1200, 70, 18, 0, 1, 1.6f, 1f);

    public enum Family { COMBINE, DOZER, PICKUP, MOTORCYCLE, BOAT, PLANE, HELICOPTER, DRONE }
    public final String id, displayName;
    public final Family family;
    public final double speed;
    public final int tank, durability, slots, radius, seats;
    public final float width, height;
    VehicleType(String id, Family family, String name, double speed, int tank, int durability,
                int slots, int radius, int seats, float width, float height) {
        this.id=id; this.family=family; this.displayName=name; this.speed=speed; this.tank=tank;
        this.durability=durability; this.slots=slots; this.radius=radius; this.seats=seats;
        this.width=width; this.height=height;
    }
    public boolean aircraft() { return family==Family.PLANE || verticalAircraft(); }
    public boolean verticalAircraft() { return family==Family.HELICOPTER || family==Family.DRONE; }
    public boolean worker() { return family==Family.COMBINE || family==Family.DOZER; }
    public String modelId() { return family.name().toLowerCase(java.util.Locale.ROOT); }
    public static VehicleType fromId(String id) {
        for (VehicleType type : values()) if (type.id.equals(id)) return type;
        throw new IllegalArgumentException("Unknown vehicle type: " + id);
    }
}
