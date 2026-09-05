package com.harvester.client;

import com.harvester.HarvesterMod;
import com.harvester.client.entity.model.CombineModel;
import com.harvester.client.entity.renderer.CombineRenderer;
import com.harvester.init.ModEntities;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

public class HarvesterModClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        HarvesterMod.LOGGER.info("[Harvester Mod] Клиент инициализирован");
        EntityModelLayerRegistry.registerModelLayer(CombineModel.MODEL_LAYER, CombineModel::createModelData);
        EntityRendererRegistry.register(ModEntities.COMBINE, CombineRenderer::new);
    }
}
