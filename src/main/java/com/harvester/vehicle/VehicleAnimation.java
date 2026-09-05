package com.harvester.vehicle;

import java.util.List;

/** Per-vehicle, frame-rate-independent visual history. No world/entity references. */
public final class VehicleAnimation {
    public record Frame(double wheelTravel, float engineRotor, float headerLift, float steering) {}
    private boolean initialized;
    private double time, x, z, yaw, travel, displayedTravel, rotor, rotorSpeed, header, steering;
    private double direction = 1;

    public static double smooth(double current, double target, double response, double ticks) {
        double dt = VehiclePhysics.clamp(ticks, 0, 3);
        double result = VehiclePhysics.finite(current)
                + (VehiclePhysics.finite(target) - VehiclePhysics.finite(current))
                * -Math.expm1(-Math.max(0, VehiclePhysics.finite(response)) * dt);
        return Math.abs(result - target) < 1e-7 ? target : result;
    }
    public static double signedDistance(double dx, double dz, double yawDegrees) {
        double angle = Math.toRadians(VehiclePhysics.finite(yawDegrees));
        return -VehiclePhysics.finite(dx) * Math.sin(angle) + VehiclePhysics.finite(dz) * Math.cos(angle);
    }
    public static float wheelPhase(double travel, double radiusBlocks) {
        double radius = Math.max(.01, VehiclePhysics.finite(radiusBlocks));
        return (float) Math.IEEEremainder(VehiclePhysics.finite(travel) / radius, Math.PI * 2);
    }
    /** Hubs must use the corresponding tire radius, not their own smaller mesh radius. */
    public static double wheelRadius(VehicleGeometry.Part part, List<VehicleGeometry.Part> all) {
        String tire = part.name().replace("wheel_hub_", "wheel_");
        VehicleGeometry.Part definition = all.stream().filter(p -> p.name().equals(tire)).findFirst().orElse(part);
        double radius = 0;
        for (var box : definition.boxes()) radius = Math.max(radius, Math.max(Math.abs(box.y()), Math.abs(box.y() + box.h())));
        return Math.max(.01, radius / 16);
    }
    public static int rotorDirection(String name) {
        return name.equals("rotor_-1_-1") || name.equals("rotor_1_1") ? -1 : 1;
    }
    public Frame update(double now, double newX, double newZ, float newYaw,
                        boolean engineActive, boolean headerEnabled, VehicleType.Family family) {
        now = VehiclePhysics.finite(now); newX = VehiclePhysics.finite(newX); newZ = VehiclePhysics.finite(newZ);
        double nextYaw = VehiclePhysics.wrap(newYaw);
        if (!initialized) {
            initialized = true; time = now; x = newX; z = newZ; yaw = nextYaw;
            header = headerEnabled ? 0 : 4;
            return frame();
        }
        double dt = now - time;
        if (dt <= 0 || dt > 5 || Math.hypot(newX - x, newZ - z) > 8) {
            // Same sample is idempotent. Clock reset/teleport does not create phantom wheel spin.
            if (dt != 0 || newX != x || newZ != z) { time = now; x = newX; z = newZ; yaw = nextYaw; }
            return frame();
        }
        double moved = signedDistance(newX - x, newZ - z, nextYaw);
        travel += moved;
        displayedTravel = smooth(displayedTravel, travel, .9, dt);
        double turn = VehiclePhysics.wrap((float) (nextYaw - yaw)) / dt;
        if (Math.abs(moved) > 1e-5) direction = Math.signum(moved);
        double wheelSteer = VehiclePhysics.clamp(turn * 6 * direction, -22, 22);
        steering = smooth(steering, wheelSteer, .4, dt);
        double targetSpeed = engineActive ? .65 : 0;
        if (family == VehicleType.Family.BOAT) targetSpeed *= direction;
        double previousSpeed = rotorSpeed;
        rotorSpeed = smooth(rotorSpeed, targetSpeed, engineActive ? .22 : .14, dt);
        // Integrate the exponential response, so phase does not depend on frame subdivision.
        double response = engineActive ? .22 : .14;
        rotor += targetSpeed * dt + (previousSpeed - targetSpeed) * -Math.expm1(-response * dt) / response;
        rotor = Math.IEEEremainder(rotor, Math.PI * 2);
        header = smooth(header, headerEnabled ? 0 : 4, .22, dt);
        time = now; x = newX; z = newZ; yaw = nextYaw;
        return frame();
    }
    private Frame frame() {
        return new Frame(displayedTravel, (float) rotor, (float) header, (float) Math.toRadians(steering));
    }
}
