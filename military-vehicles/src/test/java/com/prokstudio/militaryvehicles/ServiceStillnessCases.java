package com.prokstudio.militaryvehicles;

import com.prokstudio.militaryvehicles.core.VehicleParking;

/** Service, deployment and outrigger stillness. Pure production guards, no game mocks. */
public final class ServiceStillnessCases {
    private static long checks;
    private ServiceStillnessCases() {}
    private static void check(boolean actual, String context) {
        checks++;
        if (!actual) throw new AssertionError(context);
    }
    private static boolean operate(double x, double y, double z) {
        return VehicleParking.canOperate(true, false, x, y, z);
    }
    private static boolean pack(double x, double y, double z) {
        return VehicleParking.canPack(false, false, true, false, x, y, z);
    }
    private static boolean station(double x, double y, double z) {
        return VehicleParking.canSwitchStation(true, true, 1, true, false, x, y, z);
    }
    /** The pre-alpha.5 service gate: horizontalLengthSquared() never sees the vertical component. */
    private static boolean legacyHorizontal(double x, double y, double z) {
        double squared = x * x + z * z;
        return Double.isFinite(squared) && squared >= 0 && squared <= .000025;
    }

    public static void serviceAllowsStoppedGroundedVehicle() {
        check(operate(0, 0, 0), "stopped grounded dry vehicle may be serviced");
        check(operate(-0.0, -0.0, -0.0), "signed zero remains stationary");
        check(VehicleParking.stationary(0, 0, 0), "bare stillness rule accepts a resting vehicle");
        check(operate(.002, .002, .002), "small three-axis residual stays inside the service tolerance");
    }
    public static void serviceRejectsAirWaterAndVerticalMotion() {
        for (boolean ground : new boolean[]{false, true})
            for (boolean water : new boolean[]{false, true})
                check(VehicleParking.canOperate(ground, water, 0, 0, 0) == (ground && !water),
                    "service needs solid ground and a dry hull");
        for (double vertical : new double[]{-.4, -.05, .05, .4})
            check(!operate(0, vertical, 0) && !VehicleParking.stationary(0, vertical, 0),
                "vertical motion blocks service and deployment");
        check(!operate(0, -.006, 0), "slow sag still counts as motion");
    }
    public static void serviceRejectsNonFiniteAndOverflowingVelocity() {
        for (double broken : new double[]{Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY}) {
            check(!operate(broken, 0, 0) && !operate(0, broken, 0) && !operate(0, 0, broken),
                "non-finite velocity component blocks service");
            check(!VehicleParking.stationary(0, broken, 0), "non-finite vertical component is never stationary");
        }
        check(!operate(Double.MAX_VALUE, 0, Double.MAX_VALUE), "overflowing squares cannot pass as stillness");
        check(legacyHorizontal(0, Double.NaN, 0) && !operate(0, Double.NaN, 0),
            "the old horizontal gate could not see a non-finite vertical component");
    }
    public static void serviceStillnessMatchesCrewTolerance() {
        double at = .005, above = Math.nextUp(at);
        for (double sign : new double[]{-1, 1}) {
            check(operate(sign * at, 0, 0) && operate(0, sign * at, 0) && operate(0, 0, sign * at),
                "service boundary inclusive in every direction");
            check(!operate(sign * above, 0, 0) && !operate(0, sign * above, 0) && !operate(0, 0, sign * above),
                "immediately above the service limit");
        }
        check(station(.005, 0, 0) == operate(.005, 0, 0) && station(.01, 0, 0) == operate(.01, 0, 0),
            "service reuses the station switch tolerance");
        check(pack(.01, 0, 0) && !operate(.01, 0, 0), "packing keeps its own looser tolerance");
    }
    public static void legacyHorizontalOnlyServiceGapIsClosed() {
        double[][] hidden = {{0, -.4, 0}, {0, .4, 0}, {.001, -1.2, .001},
            {0, Double.NaN, 0}, {0, Double.POSITIVE_INFINITY, 0}};
        for (double[] velocity : hidden) {
            check(legacyHorizontal(velocity[0], velocity[1], velocity[2]),
                "the pre-alpha.5 horizontal gate accepted this velocity");
            check(!operate(velocity[0], velocity[1], velocity[2])
                && !VehicleParking.stationary(velocity[0], velocity[1], velocity[2]),
                "the component-wise rule rejects the velocity that slipped through");
        }
        check(legacyHorizontal(0, 0, 0) == operate(0, 0, 0), "a truly stopped vehicle is unaffected");
    }
    public static long runAll() {
        checks = 0;
        serviceAllowsStoppedGroundedVehicle();
        serviceRejectsAirWaterAndVerticalMotion();
        serviceRejectsNonFiniteAndOverflowingVelocity();
        serviceStillnessMatchesCrewTolerance();
        legacyHorizontalOnlyServiceGapIsClosed();
        return checks;
    }
}
