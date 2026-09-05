package com.harvester.init;

import com.harvester.entity.CombineEntity;
import net.minecraft.entity.*;
import net.minecraft.registry.*;
import net.minecraft.util.Identifier;

public final class ModEntities {
    private static final RegistryKey<EntityType<?>> KEY=RegistryKey.of(RegistryKeys.ENTITY_TYPE,Identifier.of("harvester","combine"));
    /** Keep the legacy entity ID. All sixteen variants share this type and one class. */
    public static final EntityType<CombineEntity> COMBINE=Registry.register(Registries.ENTITY_TYPE,KEY,
        EntityType.Builder.<CombineEntity>create(CombineEntity::new,SpawnGroup.MISC)
            .dimensions(3.8f,2.8f).maxTrackingRange(10).trackingTickInterval(1).makeFireImmune().build(KEY));
    public static void register() {}
}
