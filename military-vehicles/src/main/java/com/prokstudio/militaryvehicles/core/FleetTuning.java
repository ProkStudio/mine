package com.prokstudio.militaryvehicles.core;

import java.io.IOException;
import java.io.StringReader;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;

/** Immutable server economy. Does not change registry IDs, geometry, cargo or the v1 save shape. */
public record FleetTuning(boolean weaponsEnabled, boolean supportEnabled, int engineFuelUnits,
                          int shotFuelCost, int tankReloadTicks, int howitzerReloadTicks,
                          int serviceCooldownTicks, int transferLimit, int fuelReserve,
                          int workshopRepair, int recoveryFuelCost, int nearbyVehicleLimit) {
    public static final int MAX_TEXT_LENGTH = 4096;
    public static final FleetTuning DEFAULT = new FleetTuning(true, true, 1, 5, 60, 100, 40, 600, 200, 100, 40, 12);
    private static final Set<String> KEYS = Set.of("schemaVersion", "weaponsEnabled", "supportEnabled",
        "engineFuelUnits", "shotFuelCost", "tankReloadTicks", "howitzerReloadTicks", "serviceCooldownTicks",
        "transferLimit", "fuelReserve", "workshopRepair", "recoveryFuelCost", "nearbyVehicleLimit");

    public FleetTuning {
        bounds("engineFuelUnits", engineFuelUnits, 1, 10);
        bounds("shotFuelCost", shotFuelCost, 1, 50);
        bounds("tankReloadTicks", tankReloadTicks, 20, 600);
        bounds("howitzerReloadTicks", howitzerReloadTicks, 40, 1200);
        bounds("serviceCooldownTicks", serviceCooldownTicks, 10, 200);
        bounds("transferLimit", transferLimit, 1, 600);
        bounds("fuelReserve", fuelReserve, 0, 2000);
        bounds("workshopRepair", workshopRepair, 1, 100);
        bounds("recoveryFuelCost", recoveryFuelCost, 1, 200);
        bounds("nearbyVehicleLimit", nearbyVehicleLimit, 1, 12);
    }
    private static void bounds(String key, int value, int min, int max) {
        if (value < min || value > max) throw new IllegalArgumentException(key + " must be in [" + min + ", " + max + "]");
    }
    public int engineDebit(int available) {
        if (available < 0) throw new IllegalArgumentException("Negative fuel store");
        return Math.min(available, engineFuelUnits);
    }
    /** Partial files inherit documented defaults, never values left over from another server. */
    public static FleetTuning parse(String text) {
        Objects.requireNonNull(text, "settings");
        if (text.length() > MAX_TEXT_LENGTH) throw new IllegalArgumentException("Settings exceed 4096 characters");
        Properties p = new Properties() {
            @Override public synchronized Object put(Object key, Object value) {
                if (containsKey(key)) throw new IllegalArgumentException("Duplicate setting: " + key);
                return super.put(key, value);
            }
        };
        try { p.load(new StringReader(text)); }
        catch (IOException impossible) { throw new IllegalArgumentException("Cannot read settings", impossible); }
        for (String key : p.stringPropertyNames())
            if (!KEYS.contains(key)) throw new IllegalArgumentException("Unknown setting: " + key);
        if (number(p, "schemaVersion", 1) != 1) throw new IllegalArgumentException("Unsupported settings schema");
        FleetTuning d = DEFAULT;
        return new FleetTuning(flag(p, "weaponsEnabled", d.weaponsEnabled), flag(p, "supportEnabled", d.supportEnabled),
            number(p, "engineFuelUnits", d.engineFuelUnits), number(p, "shotFuelCost", d.shotFuelCost),
            number(p, "tankReloadTicks", d.tankReloadTicks), number(p, "howitzerReloadTicks", d.howitzerReloadTicks),
            number(p, "serviceCooldownTicks", d.serviceCooldownTicks), number(p, "transferLimit", d.transferLimit),
            number(p, "fuelReserve", d.fuelReserve), number(p, "workshopRepair", d.workshopRepair),
            number(p, "recoveryFuelCost", d.recoveryFuelCost), number(p, "nearbyVehicleLimit", d.nearbyVehicleLimit));
    }
    private static int number(Properties p, String key, int fallback) {
        String value = p.getProperty(key);
        if (value == null) return fallback;
        value = value.trim();
        if (!value.matches("[0-9]+")) throw new IllegalArgumentException(key + " must be an integer");
        try { return Integer.parseInt(value); }
        catch (NumberFormatException ex) { throw new IllegalArgumentException(key + " is too large", ex); }
    }
    private static boolean flag(Properties p, String key, boolean fallback) {
        String value = p.getProperty(key);
        if (value == null) return fallback;
        return switch (value.trim()) {
            case "true" -> true;
            case "false" -> false;
            default -> throw new IllegalArgumentException(key + " must be true or false");
        };
    }
    /** Stable, small wire representation; no file paths, comments or host information. */
    public String serialize() {
        return "schemaVersion=1\nweaponsEnabled=" + weaponsEnabled + "\nsupportEnabled=" + supportEnabled
            + "\nengineFuelUnits=" + engineFuelUnits + "\nshotFuelCost=" + shotFuelCost
            + "\ntankReloadTicks=" + tankReloadTicks + "\nhowitzerReloadTicks=" + howitzerReloadTicks
            + "\nserviceCooldownTicks=" + serviceCooldownTicks + "\ntransferLimit=" + transferLimit
            + "\nfuelReserve=" + fuelReserve + "\nworkshopRepair=" + workshopRepair
            + "\nrecoveryFuelCost=" + recoveryFuelCost + "\nnearbyVehicleLimit=" + nearbyVehicleLimit + "\n";
    }
}
