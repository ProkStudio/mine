package com.prokstudio.militaryvehicles.core;
/** Gameplay units only, not specifications of a real military vehicle. */
public final class TruckSpec {
    public static final String ID="truck_6x6";
    public static final int SEATS=2, SLOTS=27, TANK=2400, CONDITION=200, CAN_FUEL=600;
    public static final double MAX_FORWARD=.28, MAX_REVERSE=.10;
    // Conservative square collision encloses the entire model at every yaw.
    public static final float WIDTH=5.2f, HEIGHT=3.05f;
    public static final int NEARBY_LIMIT=12;
    public static final double LIMIT_RADIUS=48;
    private TruckSpec() {}
}
