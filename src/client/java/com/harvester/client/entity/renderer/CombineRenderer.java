package com.harvester.client.entity.renderer;

import com.harvester.HarvesterMod;
import com.harvester.client.entity.model.CombineModel;
import com.harvester.entity.CombineEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public class CombineRenderer extends MobEntityRenderer<CombineEntity, CombineRenderState, CombineModel> {
    private static final Identifier TEXTURE = Identifier.of(HarvesterMod.MOD_ID, "textures/entity/combine.png");

    public CombineRenderer(EntityRendererFactory.Context context) {
        super(context, new CombineModel(context.getPart(CombineModel.MODEL_LAYER)), 1.5f);
    }

    @Override public CombineRenderState createRenderState() { return new CombineRenderState(); }

    @Override public void updateRenderState(CombineEntity entity, CombineRenderState state, float tickProgress) {
        super.updateRenderState(entity, state, tickProgress);
        state.harvesting = entity.isHarvesting();
    }

    @Override public Identifier getTexture(CombineRenderState state) { return TEXTURE; }

    @Override protected void scale(CombineRenderState state, MatrixStack matrices) {
        // Match the broad, low silhouette to the 4-block-wide physical vehicle.
        matrices.scale(2.0f, 1.35f, 2.0f);
    }
}
