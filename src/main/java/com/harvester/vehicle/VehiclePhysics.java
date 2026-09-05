package com.harvester.vehicle;

/** Server-side movement mathematics. No Minecraft classes or registry bootstrap. Units: blocks/tick. */
public final class VehiclePhysics {
    public static final double TAKEOFF_SPEED = .22;
    public static final double BOAT_DRAFT = .30;
    private VehiclePhysics() {}

    public record Motion(double x, double y, double z, float yaw, float pitch) {}
    public record Flight(Motion motion, boolean engineActive) {}
    public record Boat(Motion motion, double turnRate) {}

    public static double finite(double value) { return Double.isFinite(value) ? value : 0; }
    public static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, finite(value)));
    }
    public static float wrap(float angle) {
        return (float) (((finite(angle) % 360) + 540) % 360 - 180);
    }
    public static float approachAngle(float angle, float target, float step) {
        return wrap(wrap(angle) + (float) clamp(wrap(target - wrap(angle)), -step, step));
    }
    public static int usableKeys(int keys, int age, boolean sameDriver) {
        return sameDriver && age >= 0 && age <= 10 ? keys & 63 : 0;
    }
    /** Hysteresis: the engine must be covered, not just a wing tip touching water. */
    public static boolean flooded(boolean previous, double depth, double engineHeight, double coverage) {
        if (!Double.isFinite(depth) || !Double.isFinite(coverage) || coverage < 2.0 / 9) return false;
        return depth > Math.max(.12, finite(engineHeight)) * (previous ? .60 : 1);
    }
    public static boolean spendsMovementFuel(boolean powered, boolean moved, boolean airborneEngine) {
        return powered && (moved || airborneEngine);
    }
    private static boolean bit(int keys, int mask) { return (keys & mask) != 0; }
    private static Motion bounded(double x, double y, double z, float yaw, float pitch, double speed) {
        x = finite(x); z = finite(z);
        double horizontal = Math.hypot(x, z);
        if (horizontal > speed) { x *= speed / horizontal; z *= speed / horizontal; }
        return new Motion(x, clamp(y, -.18, .14), z, wrap(yaw), (float) clamp(pitch, -45, 45));
    }
    /** S brakes instead of reversing aircraft. Look controls heading only while input is fresh. */
    public static Flight flight(Motion old, boolean vertical, boolean powered, boolean grounded,
                                boolean submerged, boolean fresh, int keys, float lookYaw,
                                float lookPitch, double configuredSpeed) {
        double speed = clamp(configuredSpeed, .05, .60);
        fresh &= Float.isFinite(lookYaw) && Float.isFinite(lookPitch);
        keys = fresh ? keys & 63 : 0;
        boolean forward = bit(keys, 1) && !bit(keys, 2);
        boolean brake = bit(keys, 2);
        boolean up = bit(keys, 16) && !bit(keys, 32);
        boolean down = bit(keys, 32) && !bit(keys, 16);
        float yaw = wrap(old.yaw());
        float pitch = (float) clamp(old.pitch(), -45, 45);
        double x = clamp(old.x(), -.6, .6), y = clamp(old.y(), -.18, .14), z = clamp(old.z(), -.6, .6);
        if (submerged) {
            // Passive sinking/drag. No underwater thrust, lift or hover, even with W + jump.
            return new Flight(bounded(x * .65, Math.max(-.08, y * .6 - .018), z * .65,
                    yaw, pitch * .85f, speed), false);
        }
        boolean engine = powered && (!grounded || forward || up || down);
        if (powered && fresh) {
            yaw = approachAngle(yaw, lookYaw, vertical ? 3f : 2f);
            float targetPitch = (float) clamp(lookPitch, -45, 45);
            pitch += (float) clamp(targetPitch - pitch, -2, 2);
        } else pitch *= .95f;
        double radians = Math.toRadians(yaw), pitchRadians = Math.toRadians(pitch);
        double target = powered && forward ? speed * Math.cos(pitchRadians) : 0;
        double blend = brake ? .22 : vertical ? .12 : powered && forward ? .065 : .025;
        x = x * (1 - blend) - Math.sin(radians) * target * blend;
        z = z * (1 - blend) + Math.cos(radians) * target * blend;
        if (vertical && engine) {
            double targetY = up ? .12 : down ? -.12 : powered && forward ? -Math.sin(pitchRadians) * speed : 0;
            y += (clamp(targetY, -.12, .12) - y) * .30;
            if (!up && !down && !forward && Math.abs(y) < .001) y = 0;
        } else if (!vertical && engine && Math.hypot(x, z) > TAKEOFF_SPEED) {
            // Without W, the plane glides downward; it cannot hover for free.
            double targetY = forward ? clamp(-Math.sin(pitchRadians) * Math.hypot(x, z), -.12, .10) : -.035;
            if (forward && up) targetY = Math.max(targetY, .09);
            if (down) targetY = -.12;
            y += (targetY - y) * .20;
        } else y = Math.max(-.18, y - .04);
        return new Flight(bounded(x, y, z, yaw, pitch, speed), engine);
    }
    /** Stable damped spring around the hull waterline, not a water/air velocity toggle. */
    public static double buoyancy(double oldY, double error) {
        return clamp(clamp(oldY, -.18, .14) * .68 + clamp(error, -2, 2) * .14, -.12, .08);
    }
    public static Boat boat(Motion old, double oldTurnRate, int keys, boolean powered,
                            boolean waterContact, boolean recentWater, boolean grounded,
                            double waterlineError, double configuredSpeed) {
        double speed = clamp(configuredSpeed, .05, .60);
        int forward = powered && waterContact ? (bit(keys, 1) ? 1 : 0) - (bit(keys, 2) ? 1 : 0) : 0;
        int turn = powered && waterContact ? (bit(keys, 8) ? 1 : 0) - (bit(keys, 4) ? 1 : 0) : 0;
        double yaw = Math.toRadians(wrap(old.yaw()));
        double x = clamp(old.x(), -.6, .6), z = clamp(old.z(), -.6, .6);
        double longitudinal = -Math.sin(yaw) * x + Math.cos(yaw) * z;
        double lateral = Math.cos(yaw) * x + Math.sin(yaw) * z;
        double desiredTurn = turn * (.5 + 1.8 * Math.min(1, Math.abs(longitudinal) / speed));
        if (longitudinal < -.015) desiredTurn = -desiredTurn;
        double turnRate = clamp(finite(oldTurnRate) * .75 + desiredTurn * .25, -2.3, 2.3);
        if (Math.abs(turnRate) < .001) turnRate = 0;
        float nextYaw = wrap(old.yaw() + (float) turnRate);
        if (waterContact || recentWater) {
            double target = forward * speed * (forward < 0 ? .35 : 1);
            longitudinal = forward == 0 ? longitudinal * .975 : longitudinal * .94 + target * .06;
            lateral *= .65;
        } else {
            double drag = grounded ? .55 : .98;
            longitudinal *= drag; lateral *= drag;
        }
        double angle = Math.toRadians(nextYaw);
        x = -Math.sin(angle) * longitudinal + Math.cos(angle) * lateral;
        z = Math.cos(angle) * longitudinal + Math.sin(angle) * lateral;
        double y = waterContact || recentWater ? buoyancy(old.y(), waterlineError)
                : Math.max(-.18, finite(old.y()) - .04);
        return new Boat(bounded(x, y, z, nextYaw, 0, speed), turnRate);
    }
}
