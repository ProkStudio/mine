package com.harvester.entity;

/** Deterministic transport rules. Steering deliberately takes no camera angle. */
public final class HarvesterLogic {
    private HarvesterLogic() {}

    public static boolean isWorking(boolean hasDriver, int fuel, double distanceSquared) {
        return hasDriver && fuel > 0 && Double.isFinite(distanceSquared) && distanceSquared > 1.0e-6;
    }

    public static double safeSpeed(double configured) {
        return Double.isFinite(configured) ? Math.clamp(configured, 0.05, 0.6) : 0.15;
    }

    public static float steer(float yaw, int turn, float rate) {
        float next = yaw + Math.clamp(turn, -1, 1) * rate;
        return (next % 360 + 540) % 360 - 180;
    }

    public static int fuelAfter(int fuel, int movementCost, int workCost, boolean moving, boolean working) {
        return (int) Math.max(0L, (long) fuel
                - (moving ? Math.max(0, movementCost) : 0)
                - (working ? Math.max(0, workCost) : 0));
    }

    public static boolean withinLimit(int count, int limit) {
        return count >= 0 && count < limit;
    }

    public static boolean diggable(double hardness, double maximum, boolean liquid,
                                   boolean blockEntity, boolean denied) {
        return Double.isFinite(hardness) && hardness >= 0 && hardness <= maximum
                && !liquid && !blockEntity && !denied;
    }

    /** World offset: yaw 0 faces +Z, yaw 90 faces -X. */
    public static double[] headerOffset(float yaw, int lateral, double forward) {
        double angle = Math.toRadians(yaw);
        return new double[] {
                Math.cos(angle) * lateral - Math.sin(angle) * forward,
                Math.sin(angle) * lateral + Math.cos(angle) * forward
        };
    }
}
