package dev.mine.arsenal.core;

/** Rotation of a mesh whose nose is local -Z. Independent of entity yaw conventions. */
public record FlightOrientation(float yaw, float pitch) {
    public static FlightOrientation of(double x, double y, double z, float oldYaw, float oldPitch) {
        double horizontal = Math.hypot(x, z);
        if (!Double.isFinite(horizontal) || !Double.isFinite(y) || Math.hypot(horizontal, y) < 1e-7)
            return new FlightOrientation(oldYaw, oldPitch);
        float yaw = horizontal < 1e-7 ? oldYaw : (float)Math.toDegrees(Math.atan2(-x, -z));
        return new FlightOrientation(yaw, (float)Math.toDegrees(Math.atan2(y, horizontal)));
    }
}
