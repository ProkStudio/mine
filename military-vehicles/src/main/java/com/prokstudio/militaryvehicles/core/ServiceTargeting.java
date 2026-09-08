package com.prokstudio.militaryvehicles.core;

import java.util.Collection;
import java.util.OptionalInt;

/** Select the first physical ray hit, not the nearest eligible truck in a wide view cone. */
public final class ServiceTargeting {
    public record Hit(int entityId, double distanceSquared, boolean usable) {}
    private ServiceTargeting() {}
    public static boolean ready(boolean operational, boolean occupied, boolean running, boolean locked,
                                boolean grounded, boolean water, double speedSquared) {
        return operational && !occupied && !running && !locked && grounded && !water
            && VehicleOperations.parked(speedSquared);
    }
    public static OptionalInt select(Collection<Hit> hits) {
        Hit first = null;
        for (Hit hit : hits) {
            if (hit == null || hit.entityId() < 0 || !Double.isFinite(hit.distanceSquared()) || hit.distanceSquared() < 0) continue;
            if (first == null || hit.distanceSquared() < first.distanceSquared()
                || (hit.distanceSquared() == first.distanceSquared() && hit.entityId() < first.entityId())) first = hit;
        }
        // An occupied vehicle, player or other solid entity shields everything behind it.
        return first != null && first.usable() ? OptionalInt.of(first.entityId()) : OptionalInt.empty();
    }
}
