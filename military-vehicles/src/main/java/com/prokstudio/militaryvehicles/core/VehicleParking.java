package com.prokstudio.militaryvehicles.core;

/** Pure interaction guards. All three velocity components are required, including vertical motion. */
public final class VehicleParking {
    private static final double PACK_SPEED_SQUARED = .000225;
    private static final double STATION_SPEED_SQUARED = .000025;
    private static final double SERVICE_SPEED_SQUARED = STATION_SPEED_SQUARED;

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

    /** Service, deployment and outriggers need the same component-wise stillness as switching stations. */
    public static boolean stationary(double x, double y, double z) {
        return stopped(x, y, z, SERVICE_SPEED_SQUARED);
    }

    /** Horizontal speed alone cannot see a fall, a launch or a non-finite vertical component. */
    public static boolean canOperate(boolean grounded, boolean water, double x, double y, double z) {
        return grounded && !water && stationary(x, y, z);
    }

    private static boolean stopped(double x, double y, double z, double limit) {
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)) return false;
        double squared = x * x + y * y + z * z;
        return Double.isFinite(squared) && squared <= limit;
    }
}
