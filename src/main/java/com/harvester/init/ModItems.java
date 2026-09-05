package com.harvester.init;

import com.harvester.HarvesterMod;
import net.minecraft.item.Item;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

public class ModItems {

    private static final Identifier COMBINE_SPAWN_EGG_ID = Identifier.of(HarvesterMod.MOD_ID, "combine_spawn_egg");
    private static final RegistryKey<Item> COMBINE_SPAWN_EGG_KEY = RegistryKey.of(RegistryKeys.ITEM, COMBINE_SPAWN_EGG_ID);

    public static final Item COMBINE_SPAWN_EGG = Registry.register(
            Registries.ITEM,
            COMBINE_SPAWN_EGG_KEY,
            new SpawnEggItem(new Item.Settings().registryKey(COMBINE_SPAWN_EGG_KEY).maxCount(1).spawnEgg(ModEntities.COMBINE))
    );

    public static void register() {
        HarvesterMod.LOGGER.info("[Harvester Mod] Регистрация предметов...");
    }
}
