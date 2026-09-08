package com.prokstudio.militaryvehicles.core;

import java.util.*;

/** Registered arcade vehicle profiles. Values are fictional gameplay units, not real specifications. */
public enum VehicleKind {
    TRUCK("truck_6x6", "truck_engine", 2400, 200, 3, 5.2f, 3.05f, 5.5f, 6, 2,
        new Handling(.28, .10, .010, .024, .035, .006, .48f, 2.1f),
        List.of(new Seat(9,24,16), new Seat(-9,24,16)), new Point(-14.25,38.75,2.5)),
    BUGGY("scout_buggy", "buggy_engine", 1200, 120, 1, 3.9f, 2.5f, 5.5f, 4, 2,
        new Handling(.40, .14, .018, .032, .050, .009, .58f, 3.1f),
        List.of(new Seat(6,19,2), new Seat(-6,19,2)), new Point(-9.75,14.75,-25.5)),
    CARRIER("carrier_8x8", "carrier_engine", 3200, 360, 2, 5.8f, 3.15f, 6.5f, 8, 4,
        new Handling(.22, .085, .007, .018, .030, .005, .38f, 1.65f),
        List.of(new Seat(8.5f,24,20), new Seat(-8.5f,24,20), new Seat(9,24,1),
            new Seat(-9,24,1), new Seat(9,24,-17), new Seat(-9,24,-17)), new Point(-17.75,38.75,-28.5));

    public record Handling(double forward, double reverse, double acceleration, double counterBrake,
                           double brake, double coast, float steer, float yawRate) {}
    /** Model units: +Y up, +Z forwards; topY is the cushion surface, not entity feet. */
    public record Seat(float x, float topY, float z) {}
    public record Point(double x, double y, double z) {}
    public final String id, soundId;
    public final int tank, condition, cargoRows, wheels, steeringWheels;
    public final float width, height, wheelRadius;
    public final Handling handling;
    public final List<Seat> seats;
    public final Point exhaust;

    VehicleKind(String id, String soundId, int tank, int condition, int cargoRows, float width, float height,
                float wheelRadius, int wheels, int steeringWheels, Handling handling, List<Seat> seats, Point exhaust) {
        this.id=id; this.soundId=soundId; this.tank=tank; this.condition=condition; this.cargoRows=cargoRows;
        this.width=width; this.height=height; this.wheelRadius=wheelRadius; this.wheels=wheels;
        this.steeringWheels=steeringWheels; this.handling=handling; this.seats=List.copyOf(seats); this.exhaust=exhaust;
    }
    public int cargoSlots() { return cargoRows*9; }
    public float itemScale() { return this==BUGGY?.235f:.175f; }
    public static Optional<VehicleKind> find(String id) {
        return Arrays.stream(values()).filter(kind->kind.id.equals(id)).findFirst();
    }
    public static VehicleKind require(String id) {
        return find(id).orElseThrow(()->new IllegalArgumentException("Unsupported vehicle type"));
    }
}
