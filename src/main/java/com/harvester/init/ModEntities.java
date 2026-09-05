package com.harvester.init;

import com.harvester.HarvesterMod;
import com.harvester.entity.CombineEntity;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

public class ModEntities {

    private static final Identifier COMBINE_ID = Identifier.of(HarvesterMod.MOD_ID, "combine");
    private static final RegistryKey<EntityType<?>> COMBINE_KEY = RegistryKey.of(RegistryKeys.ENTITY_TYPE, COMBINE_ID);

    public static final EntityType<CombineEntity> COMBINE = Registry.register(
            Registries.ENTITY_TYPE, COMBINE_KEY,
            EntityType.Builder.<CombineEntity>create(CombineEntity::new, SpawnGroup.MISC)
                    .dimensions(4.0f, 2.8f)
                    .maxTrackingRange(80)
                    .trackingTickInterval(3)
                    .makeFireImmune()
                    .build(COMBINE_KEY)
    );

    public static void register() {
        HarvesterMod.LOGGER.info("[Harvester Mod] Регистрация сущностей...");
    }
}
