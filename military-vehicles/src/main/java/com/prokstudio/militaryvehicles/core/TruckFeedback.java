package com.prokstudio.militaryvehicles.core;

import java.util.Comparator;
import java.util.List;

/** Pure selection, load and exhaust rules, shared by runtime and regression tests. */
public final class TruckFeedback {
    public static final int VOICE_LIMIT = 8;
    public static final double AUDIO_RANGE = 32;
    // Centre of the horizontal exhaust outlet, just outside its -Z face (model units / 16).
    public static final double EXHAUST_X = -14.25 / 16;
    public static final double EXHAUST_Y = 38.75 / 16;
    public static final double EXHAUST_Z = 2.5 / 16;
    public record Offset(double x, double y, double z) {}
    public record Candidate(int id, double distanceSquared, boolean own, boolean playing) {}
    private TruckFeedback() {}

    public static byte driveLoad(int keys, boolean powered, boolean grounded) {
        if (!powered || !grounded || (keys & ~ControlLatch.MASK) != 0 || (keys & ControlLatch.BRAKE) != 0) return 0;
        boolean forward = (keys & ControlLatch.FORWARD) != 0;
        boolean reverse = (keys & ControlLatch.BACK) != 0;
        return (byte) (forward != reverse ? 100 : 0);
    }

    /** Interpolated horizontal displacement only; teleports must not rev the engine. */
    public static double observedSpeed(double dx, double dz) {
        double speed = Math.hypot(dx, dz);
        return Double.isFinite(speed) && speed < 2 ? speed : 0;
    }

    public static List<Integer> selectVoices(List<Candidate> candidates) {
        Comparator<Candidate> order = Comparator.comparingInt((Candidate c) -> c.own() ? 0 : 1)
            .thenComparingDouble(c -> c.distanceSquared() - (c.playing() ? 4 : 0))
            .thenComparingInt(Candidate::id);
        return candidates.stream()
            .filter(c -> Double.isFinite(c.distanceSquared()) && c.distanceSquared() >= 0
                && c.distanceSquared() <= AUDIO_RANGE * AUDIO_RANGE)
            .sorted(order).map(Candidate::id).distinct().limit(VOICE_LIMIT).toList();
    }

    /** At most one particle per 4 ticks per running truck; idle halves that rate. */
    public static boolean emitExhaust(boolean powered, double load, long tick, int id) {
        if (!powered || tick < 0) return false;
        int interval = Double.isFinite(load) && load > .5 ? 4 : 8;
        // Separate modulo avoids overflow for large world times and negative entity IDs.
        return (Math.floorMod(tick, interval) + Math.floorMod(id, interval)) % interval == 0;
    }

    public static Offset exhaustOffset(float yaw) {
        if (!Float.isFinite(yaw)) throw new IllegalArgumentException("Non-finite exhaust yaw");
        double angle = Math.toRadians(TruckPhysics.wrap(yaw));
        double sin = Math.sin(angle), cos = Math.cos(angle);
        return new Offset(EXHAUST_X * cos - EXHAUST_Z * sin, EXHAUST_Y,
            EXHAUST_X * sin + EXHAUST_Z * cos);
    }
}
