package com.prokstudio.militaryvehicles.core;

/** Per-loop, 20 Hz arcade audio envelope. Never used for vehicle movement or saving. */
public final class EngineFeedback {
    public record Frame(float volume, float pitch) {}
    private float volume;
    private float pitch = .78f;

    public Frame update(boolean running, double load, double speed) {
        double demand = unit(load);
        double road = unit(Math.abs(speed) / TruckSpec.MAX_FORWARD);
        float targetVolume = running ? (float) (.22 + .15 * demand + .10 * road) : 0;
        float targetPitch = running ? (float) (.78 + .42 * demand + .32 * road) : .78f;
        volume += (targetVolume - volume) * (running ? .16f : .26f);
        pitch += (targetPitch - pitch) * .14f;
        if (!running && volume < .001f) volume = 0;
        return new Frame(volume, pitch);
    }

    private static double unit(double value) {
        return Double.isFinite(value) ? Math.clamp(value, 0, 1) : 0;
    }
}
