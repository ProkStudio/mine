package com.harvester;

import com.harvester.entity.CombineEntity;
import com.harvester.config.HarvesterConfig;
import com.harvester.init.ModEntities;
import com.harvester.init.ModItems;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HarvesterMod implements ModInitializer {

    public static final String MOD_ID = "harvester";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static HarvesterConfig CONFIG;

    @Override
    public void onInitialize() {
        LOGGER.info("[Harvester Mod] Инициализация...");
        CONFIG = HarvesterConfig.load();
        ModEntities.register();
        ModItems.register();
        FabricDefaultAttributeRegistry.register(ModEntities.COMBINE, CombineEntity.createAttributes());
        LOGGER.info("[Harvester Mod] Готово!");
    }
}
