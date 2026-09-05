package com.harvester.client.entity.model;

import com.harvester.client.entity.renderer.CombineRenderState;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.util.Identifier;

import static com.harvester.HarvesterMod.MOD_ID;

/** Client model using the 1.21.11 render-state API. */
public class CombineModel extends EntityModel<CombineRenderState> {
    public static final EntityModelLayer MODEL_LAYER = new EntityModelLayer(Identifier.of(MOD_ID, "combine"), "main");

    private final ModelPart body;
    private final ModelPart header;
    private final ModelPart reel;
    private final ModelPart wheelFL;
    private final ModelPart wheelFR;
    private final ModelPart wheelBL;
    private final ModelPart wheelBR;

    public CombineModel(ModelPart root) {
        super(root);
        body = root.getChild("body");
        header = body.getChild("header");
        reel = header.getChild("reel");
        wheelFL = root.getChild("wheel_fl");
        wheelFR = root.getChild("wheel_fr");
        wheelBL = root.getChild("wheel_bl");
        wheelBR = root.getChild("wheel_br");
    }

    public static TexturedModelData createModelData() {
        ModelData modelData = new ModelData();
        ModelPartData root = modelData.getRoot();
        ModelPartData body = root.addChild("body", ModelPartBuilder.create().uv(0, 0).cuboid(-14, -8, -8, 28, 16, 16), ModelTransform.origin(0, 0, 0));
        // The cab is built from a frame and side glass instead of a solid block: the front remains panoramic in first person.
        ModelPartData cabin = body.addChild("cabin", ModelPartBuilder.create().uv(0, 32).cuboid(-7, -18, 3, 14, 10, 2), ModelTransform.origin(0, 0, 0));
        cabin.addChild("glass_left", ModelPartBuilder.create().uv(0, 64).cuboid(-7, -18, -5, 2, 10, 10), ModelTransform.origin(0, 0, 0));
        cabin.addChild("glass_right", ModelPartBuilder.create().uv(0, 64).cuboid(5, -18, -5, 2, 10, 10), ModelTransform.origin(0, 0, 0));
        cabin.addChild("windshield_frame", ModelPartBuilder.create().uv(40, 64).cuboid(-7, -18, -5, 14, 2, 2).uv(40, 68).cuboid(-7, -10, -5, 14, 2, 2), ModelTransform.origin(0, 0, 0));
        cabin.addChild("roof", ModelPartBuilder.create().uv(72, 16).cuboid(-8, -20, -6, 16, 2, 13), ModelTransform.origin(0, 0, 0));
        body.addChild("rear", ModelPartBuilder.create().uv(72, 32).cuboid(-11, -5, 8, 22, 10, 7), ModelTransform.origin(0, 0, 0));
        ModelPartData header = body.addChild("header", ModelPartBuilder.create().uv(0, 50).cuboid(-15, 4, -22, 30, 4, 14), ModelTransform.origin(0, 0, 0));
        header.addChild("reel", ModelPartBuilder.create().uv(0, 88).cuboid(-14, -2, -23, 28, 3, 3).uv(0, 94).cuboid(-14, -4, -22, 3, 8, 2).uv(0, 94).cuboid(11, -4, -22, 3, 8, 2), ModelTransform.origin(0, 0, 0));
        body.addChild("exhaust", ModelPartBuilder.create().uv(72, 0).cuboid(-1, -26, -2, 2, 10, 2), ModelTransform.origin(8, 0, 2));

        float wheelY = 8;
        root.addChild("wheel_fl", ModelPartBuilder.create().uv(0, 70).cuboid(-3, -5, -5, 6, 10, 10), ModelTransform.origin(-14, wheelY, -4));
        root.addChild("wheel_fr", ModelPartBuilder.create().uv(0, 70).cuboid(-3, -5, -5, 6, 10, 10), ModelTransform.origin(14, wheelY, -4));
        root.addChild("wheel_bl", ModelPartBuilder.create().uv(32, 70).cuboid(-2, -4, -4, 4, 8, 8), ModelTransform.origin(-14, wheelY + 1, 6));
        root.addChild("wheel_br", ModelPartBuilder.create().uv(32, 70).cuboid(-2, -4, -4, 4, 8, 8), ModelTransform.origin(14, wheelY + 1, 6));
        return TexturedModelData.of(modelData, 128, 128);
    }

    @Override public void setAngles(CombineRenderState state) {
        super.setAngles(state);
        float wheelRotation = state.limbSwingAnimationProgress * 0.4f;
        wheelFL.roll = wheelRotation;
        wheelFR.roll = wheelRotation;
        wheelBL.roll = wheelRotation;
        wheelBR.roll = wheelRotation;
        header.pitch = state.harvesting ? (float) Math.sin(state.age * 0.3f) * 0.1f : 0.0f;
        reel.roll = state.harvesting ? state.age * 0.9f : 0.0f;
    }
}
