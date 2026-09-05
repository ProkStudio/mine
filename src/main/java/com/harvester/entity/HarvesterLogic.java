package com.harvester.entity;

/** Deterministic rules shared by the entity and dependency-free regression tests. */
public final class HarvesterLogic {
    private HarvesterLogic() {}

    public static boolean isWorking(boolean hasDriver, int fuel, double distanceSquared) {
        return hasDriver && fuel > 0 && Double.isFinite(distanceSquared) && distanceSquared > 1.0e-6;
    }

    public static double safeSpeed(double configured) {
        return Double.isFinite(configured) ? Math.clamp(configured, 0.05, 1.0) : 0.25;
    }

    /** World offset of a point on the header: yaw 0 faces +Z, yaw 90 faces -X. */
    public static double[] headerOffset(float yaw, int lateral, int forward) {
        double angle = Math.toRadians(yaw);
        return new double[] {
                Math.cos(angle) * lateral - Math.sin(angle) * forward,
                Math.sin(angle) * lateral + Math.cos(angle) * forward
        };
    }
}
