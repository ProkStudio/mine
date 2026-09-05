package com.harvester.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.harvester.HarvesterMod;
import com.harvester.entity.HarvesterLogic;
import com.harvester.vehicle.VehicleType;
import net.fabricmc.loader.api.FabricLoader;
import java.nio.file.*;
import java.io.*;
import java.util.*;

/** Server authority: movement and limits never depend on the client's config. */
public final class HarvesterConfig {
    private static final Gson GSON=new GsonBuilder().setPrettyPrinting().create();
    // Migration inputs for the original harvester.json.
    public int maxFuel=1600, fuelPerTick=1, harvestRadius=1, harvestIntervalTicks=5, inventorySize=27;
    public double drivingSpeed=.15;
    public Map<String,Stats> vehicles=new LinkedHashMap<>();
    public FuelCans fuelCans=new FuelCans();
    public VehicleLimit vehicleLimit=new VehicleLimit();
    public DigRules digRules=new DigRules();
    public static final class Stats {
        public double speed;
        public int tank, durability, slots, workRadius, movementFuel=1, workFuel=1;
        public Stats() {}
        public Stats(VehicleType t) { speed=t.speed; tank=t.tank; durability=t.durability; slots=t.slots; workRadius=t.radius; }
        void sanitize() {
            speed=HarvesterLogic.safeSpeed(speed); tank=Math.clamp(tank,80,64000);
            durability=Math.clamp(durability,1,10000); slots=Math.clamp(slots/9,1,6)*9;
            workRadius=Math.clamp(workRadius,0,4); movementFuel=Math.clamp(movementFuel,0,20); workFuel=Math.clamp(workFuel,0,100);
        }
    }
    public static final class FuelCans { public int small=400, medium=1200, large=3200; }
    public static final class VehicleLimit { public int maximum=12; public double radius=32; }
    public static final class DigRules {
        public double maxHardness=3;
        public int blocksPerCycle=4, intervalTicks=5;
        public List<String> denied=new ArrayList<>(List.of("minecraft:bedrock","minecraft:barrier","minecraft:end_portal_frame","minecraft:reinforced_deepslate","minecraft:spawner","minecraft:trial_spawner","minecraft:vault"));
    }
    public Stats stats(VehicleType t) { return vehicles.get(t.id); }
    public static HarvesterConfig load() {
        Path path=FabricLoader.getInstance().getConfigDir().resolve("harvester.json");
        HarvesterConfig c=new HarvesterConfig();
        boolean existed=Files.exists(path);
        try {
            if (existed) try (Reader r=Files.newBufferedReader(path)) { HarvesterConfig loaded=GSON.fromJson(r,HarvesterConfig.class); if (loaded!=null) c=loaded; }
        } catch (IOException | RuntimeException e) { HarvesterMod.LOGGER.warn("Invalid harvester config; using defaults",e); }
        c.sanitize();
        // Preserve the original. An expanded example is written separately for old configs.
        Path output=existed ? path.resolveSibling("harvester.example.json") : path;
        try {
            Files.createDirectories(output.getParent());
            try (Writer w=Files.newBufferedWriter(output)) { GSON.toJson(c,w); }
        } catch (IOException e) { HarvesterMod.LOGGER.warn("Cannot save harvester config example",e); }
        return c;
    }
    public void sanitize() {
        if (vehicles==null) vehicles=new LinkedHashMap<>();
        boolean legacy=!vehicles.containsKey(VehicleType.COMBINE.id);
        for (VehicleType t:VehicleType.values()) { Stats s=vehicles.get(t.id); if(s==null) { s=new Stats(t); vehicles.put(t.id,s); } s.sanitize(); }
        if (legacy) {
            Stats s=stats(VehicleType.COMBINE); s.tank=Math.clamp(maxFuel,80,64000);
            s.speed=Math.min(.20,HarvesterLogic.safeSpeed(drivingSpeed));
            s.workRadius=Math.clamp(harvestRadius,0,4); s.movementFuel=Math.clamp(fuelPerTick,0,20);
        }
        harvestIntervalTicks=Math.clamp(harvestIntervalTicks,1,100);
        if (fuelCans==null) fuelCans=new FuelCans();
        fuelCans.small=Math.clamp(fuelCans.small,1,64000); fuelCans.medium=Math.clamp(fuelCans.medium,1,64000); fuelCans.large=Math.clamp(fuelCans.large,1,64000);
        if(vehicleLimit==null) vehicleLimit=new VehicleLimit();
        vehicleLimit.maximum=Math.clamp(vehicleLimit.maximum,1,128);
        vehicleLimit.radius=Double.isFinite(vehicleLimit.radius)?Math.clamp(vehicleLimit.radius,4,128):32;
        if(digRules==null) digRules=new DigRules();
        digRules.maxHardness=Double.isFinite(digRules.maxHardness)?Math.clamp(digRules.maxHardness,0,50):3;
        digRules.blocksPerCycle=Math.clamp(digRules.blocksPerCycle,1,16); digRules.intervalTicks=Math.clamp(digRules.intervalTicks,1,100);
        if(digRules.denied==null) digRules.denied=new ArrayList<>();
    }
}
