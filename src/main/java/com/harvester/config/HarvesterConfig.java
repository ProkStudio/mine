package com.harvester.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.harvester.HarvesterMod;
import com.harvester.entity.HarvesterLogic;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/** Server-side balance settings. The default file is created on first launch. */
public final class HarvesterConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("harvester.json");

    public int maxFuel = 1600;
    public int fuelPerTick = 1;
    public double drivingSpeed = 0.25;
    public int harvestRadius = 1;
    public int harvestIntervalTicks = 5;
    public int inventorySize = 27;

    public static HarvesterConfig load() {
        HarvesterConfig config = new HarvesterConfig();
        try {
            if (Files.exists(PATH)) {
                try (Reader reader = Files.newBufferedReader(PATH)) {
                    HarvesterConfig loaded = GSON.fromJson(reader, HarvesterConfig.class);
                    if (loaded != null) config = loaded;
                }
            } else {
                Files.createDirectories(PATH.getParent());
                try (Writer writer = Files.newBufferedWriter(PATH)) {
                    GSON.toJson(config, writer);
                }
            }
        } catch (IOException | RuntimeException exception) {
            HarvesterMod.LOGGER.warn("Не удалось загрузить конфиг комбайна; используются значения по умолчанию", exception);
        }
        config.sanitize();
        return config;
    }

    private void sanitize() {
        maxFuel = Math.clamp(maxFuel, 80, 64_000);
        fuelPerTick = Math.clamp(fuelPerTick, 0, 20);
        drivingSpeed = HarvesterLogic.safeSpeed(drivingSpeed);
        harvestRadius = Math.clamp(harvestRadius, 0, 4);
        harvestIntervalTicks = Math.clamp(harvestIntervalTicks, 1, 100);
        inventorySize = 27;
    }
}
