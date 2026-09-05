package com.harvester.vehicle;

/** Visual-only seat pose. Degrees for head/body; radians for limb ModelPart rotations. */
public final class PassengerPose {
    private PassengerPose() {}
    public record Limbs(float armPitch, float armInward, float legPitch, float legSpread) {}
    public record Facing(float bodyYaw, float headYaw, float headPitch) {}

    public static Facing facing(float seatYaw, float viewYaw, float viewPitch) {
        float body = VehiclePhysics.wrap(seatYaw);
        float head = (float) VehiclePhysics.clamp(VehiclePhysics.wrap(viewYaw - body), -65, 65);
        return new Facing(body, head, (float) VehiclePhysics.clamp(viewPitch, -35, 45));
    }
    public static Limbs limbs(VehicleType type, int seat) {
        if (type.family == VehicleType.Family.MOTORCYCLE && seat > 0)
            return new Limbs(-.35f, .08f, -1.30f, .30f);
        return switch (type.family) {
            case MOTORCYCLE -> new Limbs(-1.05f, .18f, -1.25f, .32f);
            case COMBINE, DOZER -> new Limbs(-.95f, .22f, -1.40f, .20f);
            case PICKUP, BOAT -> new Limbs(-1.10f, .24f, -1.40f, .20f);
            case PLANE -> new Limbs(-1.05f, .20f, -1.40f, .18f);
            case HELICOPTER -> new Limbs(-.75f, .12f, -1.40f, .20f);
            case DRONE -> new Limbs(-.85f, .16f, -1.30f, .28f);
        };
    }
    public static boolean keepVanillaArms(boolean usingItem, float swingProgress) {
        return usingItem || Float.isFinite(swingProgress) && swingProgress > 0;
    }
}
