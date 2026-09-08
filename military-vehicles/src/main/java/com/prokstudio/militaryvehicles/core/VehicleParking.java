package com.prokstudio.militaryvehicles.core;

/** Pure interaction guards. All three velocity components are required, including vertical motion. */
public final class VehicleParking {
    private static final double PACK_SPEED_SQUARED = .000225;
    private static final double STATION_SPEED_SQUARED = .000025;

    private VehicleParking() {}

    public static boolean canPack(boolean passengers, boolean deploymentLocked,
                                  boolean grounded, boolean water, double x, double y, double z) {
        return !passengers && !deploymentLocked && grounded && !water
            && stopped(x, y, z, PACK_SPEED_SQUARED);
    }

    /** firstPassenger refers to the actual passenger list, not the effective solo-gunner seat. */
    public static boolean canSwitchStation(boolean armed, boolean firstPassenger, int passengers,
                                           boolean grounded, boolean water, double x, double y, double z) {
        return armed && firstPassenger && passengers == 1 && grounded && !water
            && stopped(x, y, z, STATION_SPEED_SQUARED);
    }

    private static boolean stopped(double x, double y, double z, double limit) {
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)) return false;
        double squared = x * x + y * y + z * z;
        return Double.isFinite(squared) && squared <= limit;
    }
}
