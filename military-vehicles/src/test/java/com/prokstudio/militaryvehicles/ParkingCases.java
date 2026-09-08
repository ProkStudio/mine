package com.prokstudio.militaryvehicles;

import com.prokstudio.militaryvehicles.core.VehicleParking;

/** Shared by JUnit and the standalone smoke runner; executes production guards without game mocks. */
public final class ParkingCases {
    private static long checks;
    private ParkingCases() {}
    private static void check(boolean actual, String context) {
        checks++;
        if (!actual) throw new AssertionError(context);
    }
    private static boolean pack(double x, double y, double z) {
        return VehicleParking.canPack(false, false, true, false, x, y, z);
    }
    private static boolean station(double x, double y, double z) {
        return VehicleParking.canSwitchStation(true, true, 1, true, false, x, y, z);
    }

    public static void packingAllowsStoppedDryEmptyVehicle() {
        check(pack(0, 0, 0), "stopped dry empty vehicle");
        check(pack(-0.0, 0, -0.0), "signed zero remains stopped");
        check(pack(.001, -.001, .001), "small three-axis residual remains allowed");
    }
    public static void packingKeepsPassengerAndDeploymentLocks() {
        for (boolean passengers : new boolean[]{false, true})
            for (boolean locked : new boolean[]{false, true}) {
                check(VehicleParking.canPack(passengers, locked, true, false, 0, 0, 0)
                    == (!passengers && !locked), "passengers and deployment remain independent locks");
            }
    }
    public static void packingRejectsAirAndWater() {
        for (boolean ground : new boolean[]{false, true})
            for (boolean water : new boolean[]{false, true}) {
                check(VehicleParking.canPack(false, false, ground, water, 0, 0, 0)
                    == (ground && !water), "packing needs dry ground even at zero velocity");
            }
    }
    public static void packingIncludesVerticalAndDiagonalMotion() {
        for (double v : new double[]{-.5, -.016, .016, .5}) {
            check(!pack(0, v, 0), "vertical-only travel must block packing");
            check(!pack(v, 0, 0) && !pack(0, 0, v), "horizontal travel still blocks packing");
        }
        check(!pack(.01, .01, .01), "three small components exceed total packing limit");
        check(!pack(.01, -.012, 0), "combined horizontal and vertical motion");
    }
    public static void packingRejectsNonFiniteAndOverflowingVelocity() {
        for (double bad : new double[]{Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY,
                                       Double.MAX_VALUE, -Double.MAX_VALUE}) {
            check(!pack(bad, 0, 0), "invalid packing x");
            check(!pack(0, bad, 0), "invalid packing y");
            check(!pack(0, 0, bad), "invalid packing z");
        }
        check(!pack(Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE), "overflow fails closed");
    }
    public static void packingPreservesLegacySpeedBoundary() {
        double at = .015, above = Math.nextUp(at);
        for (double sign : new double[]{-1, 1}) {
            check(pack(sign * at, 0, 0), "legacy x boundary inclusive");
            check(pack(0, sign * at, 0), "same radial boundary on y");
            check(pack(0, 0, sign * at), "legacy z boundary inclusive");
            check(!pack(sign * above, 0, 0), "x immediately above boundary rejected");
            check(!pack(0, sign * above, 0), "y immediately above boundary rejected");
            check(!pack(0, 0, sign * above), "z immediately above boundary rejected");
        }
    }
    public static void stationSwitchRequiresArmedSoleFirstPassenger() {
        for (boolean armed : new boolean[]{false, true})
            for (boolean first : new boolean[]{false, true})
                for (int passengers : new int[]{-1, 0, 1, 2, 6, Integer.MAX_VALUE}) {
                    check(VehicleParking.canSwitchStation(armed, first, passengers, true, false, 0, 0, 0)
                        == (armed && first && passengers == 1), "sole actual first passenger of armed vehicle");
                }
        check(station(0, 0, 0) && station(0, 0, 0), "the same guard allows both solo station directions");
    }
    public static void stationSwitchRejectsAirWaterAndVerticalMotion() {
        for (boolean ground : new boolean[]{false, true})
            for (boolean water : new boolean[]{false, true}) {
                check(VehicleParking.canSwitchStation(true, true, 1, ground, water, 0, 0, 0)
                    == (ground && !water), "station transition needs dry ground");
            }
        for (double v : new double[]{-.5, -.006, .006, .5}) {
            check(!station(0, v, 0), "vertical-only travel must block station transition");
            check(!station(v, 0, 0) && !station(0, 0, v), "horizontal movement also blocks transition");
        }
        check(!station(.003, .003, .003), "total velocity, not separate axis limits");
    }
    public static void stationSwitchRejectsNonFiniteAndOverflowingVelocity() {
        for (double bad : new double[]{Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY,
                                       Double.MAX_VALUE, -Double.MAX_VALUE}) {
            check(!station(bad, 0, 0), "invalid station x");
            check(!station(0, bad, 0), "invalid station y");
            check(!station(0, 0, bad), "invalid station z");
        }
    }
    public static void stationSwitchPreservesStopThreshold() {
        double at = .005, above = Math.nextUp(at);
        for (double sign : new double[]{-1, 1}) {
            check(station(sign * at, 0, 0) && station(0, sign * at, 0)
                && station(0, 0, sign * at), "station stop boundary inclusive in every direction");
            check(!station(sign * above, 0, 0) && !station(0, sign * above, 0)
                && !station(0, 0, sign * above), "immediately above station stop limit");
        }
        check(pack(.01, 0, 0) && !station(.01, 0, 0), "packing and crew retain different old tolerances");
    }
    public static long runAll() {
        checks = 0;
        packingAllowsStoppedDryEmptyVehicle();
        packingKeepsPassengerAndDeploymentLocks();
        packingRejectsAirAndWater();
        packingIncludesVerticalAndDiagonalMotion();
        packingRejectsNonFiniteAndOverflowingVelocity();
        packingPreservesLegacySpeedBoundary();
        stationSwitchRequiresArmedSoleFirstPassenger();
        stationSwitchRejectsAirWaterAndVerticalMotion();
        stationSwitchRejectsNonFiniteAndOverflowingVelocity();
        stationSwitchPreservesStopThreshold();
        return checks;
    }
}
