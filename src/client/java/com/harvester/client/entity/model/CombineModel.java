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

/** Low-poly combine, retaining the existing 128x128 texture atlas. */
public class CombineModel extends EntityModel<CombineRenderState> {
    public static final EntityModelLayer MODEL_LAYER = new EntityModelLayer(Identifier.of(MOD_ID, "combine"), "main");
    private final ModelPart header;
    private final ModelPart reel;
    private final ModelPart wheelFL;
    private final ModelPart wheelFR;
    private final ModelPart wheelBL;
    private final ModelPart wheelBR;

    public CombineModel(ModelPart root) {
        super(root);
        ModelPart body = root.getChild("body");
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
        // Wheel bottoms now meet model Y=24 (ground), instead of floating at Y=13.
        ModelPartData body = root.addChild("body", ModelPartBuilder.create().uv(0, 0).cuboid(-14, -8, -8, 28, 16, 16), ModelTransform.origin(0, 11, 0));
        ModelPartData cabin = body.addChild("cabin", ModelPartBuilder.create().uv(0, 32).cuboid(-7, -18, 3, 14, 10, 2), ModelTransform.origin(0, 0, 0));
        cabin.addChild("glass_left", ModelPartBuilder.create().uv(0, 64).cuboid(-7, -18, -5, 1, 10, 8), ModelTransform.origin(0, 0, 0));
        cabin.addChild("glass_right", ModelPartBuilder.create().uv(0, 64).cuboid(6, -18, -5, 1, 10, 8), ModelTransform.origin(0, 0, 0));
        cabin.addChild("windshield_frame", ModelPartBuilder.create().uv(40, 64).cuboid(-7, -18, -5, 14, 1, 1).uv(40, 68).cuboid(-7, -9, -5, 14, 1, 1), ModelTransform.origin(0, 0, 0));
        cabin.addChild("roof", ModelPartBuilder.create().uv(72, 16).cuboid(-8, -20, -6, 16, 2, 13), ModelTransform.origin(0, 0, 0));
        body.addChild("rear", ModelPartBuilder.create().uv(72, 32).cuboid(-11, -5, 8, 22, 10, 7), ModelTransform.origin(0, 0, 0));
        body.addChild("grain_tank", ModelPartBuilder.create().uv(72, 32).cuboid(-10, -13, 6, 20, 5, 8), ModelTransform.origin(0, 0, 0));
        body.addChild("exhaust", ModelPartBuilder.create().uv(72, 0).cuboid(-1, -24, -2, 2, 8, 2), ModelTransform.origin(9, 0, 5));
        // The unloading pipe is stowed along the machine's side (visual only).
        body.addChild("unloading_pipe", ModelPartBuilder.create().uv(72, 0).cuboid(12, -12, 2, 2, 2, 16).cuboid(12, -12, 16, 2, 5, 2), ModelTransform.origin(0, 0, 0));
        body.addChild("ladder", ModelPartBuilder.create().uv(40, 64)
                .cuboid(-17, -6, 1, 1, 12, 1).cuboid(-17, -6, 6, 1, 12, 1)
                .cuboid(-18, -4, 1, 2, 1, 6).cuboid(-18, 0, 1, 2, 1, 6).cuboid(-18, 4, 1, 2, 1, 6), ModelTransform.origin(0, 0, 0));
        // Pivot at the feeder, not at the center of the whole machine.
        ModelPartData header = body.addChild("header", ModelPartBuilder.create().uv(0, 50).cuboid(-15, 0, -14, 30, 4, 14), ModelTransform.origin(0, 4, -8));
        header.addChild("divider_left", ModelPartBuilder.create().uv(72, 32).cuboid(-16, -2, -16, 2, 6, 16), ModelTransform.origin(0, 0, 0));
        header.addChild("divider_right", ModelPartBuilder.create().uv(72, 32).cuboid(14, -2, -16, 2, 6, 16), ModelTransform.origin(0, 0, 0));
        ModelPartBuilder cutter = ModelPartBuilder.create().uv(40, 64);
        for (int x = -13; x <= 13; x += 2) cutter.cuboid(x, 2, -16, 1, 1, 3);
        header.addChild("cutter", cutter, ModelTransform.origin(0, 0, 0));
        // All reel cuboids are local to its axle; pitch rotates around the horizontal X axis.
        header.addChild("reel", ModelPartBuilder.create().uv(0, 88)
                .cuboid(-14, -1, -1, 28, 2, 2)
                .cuboid(-14, -4, -1, 28, 1, 2).cuboid(-14, 3, -1, 28, 1, 2)
                .cuboid(-14, -1, -4, 28, 2, 1).cuboid(-14, -1, 3, 28, 2, 1)
                .uv(0, 94).cuboid(-14, -4, -1, 1, 8, 2).cuboid(13, -4, -1, 1, 8, 2), ModelTransform.origin(0, 0, 0));
        root.addChild("wheel_fl", ModelPartBuilder.create().uv(0, 70).cuboid(-3, -5, -5, 6, 10, 10), ModelTransform.origin(-14, 19, -4));
        root.addChild("wheel_fr", ModelPartBuilder.create().uv(0, 70).cuboid(-3, -5, -5, 6, 10, 10), ModelTransform.origin(14, 19, -4));
        root.addChild("wheel_bl", ModelPartBuilder.create().uv(32, 70).cuboid(-2, -4, -4, 4, 8, 8), ModelTransform.origin(-14, 20, 6));
        root.addChild("wheel_br", ModelPartBuilder.create().uv(32, 70).cuboid(-2, -4, -4, 4, 8, 8), ModelTransform.origin(14, 20, 6));
        return TexturedModelData.of(modelData, 128, 128);
    }

    @Override public void setAngles(CombineRenderState state) {
        super.setAngles(state);
        float wheelRotation = state.limbSwingAnimationProgress * 0.4f;
        wheelFL.pitch = wheelRotation;
        wheelFR.pitch = wheelRotation;
        wheelBL.pitch = wheelRotation * 1.25f;
        wheelBR.pitch = wheelRotation * 1.25f;
        // Negative pitch lifts the front when switched into transport mode.
        header.pitch = state.headerEnabled ? 0.0f : -0.22f;
        reel.pitch = state.reelRotation;
    }
}
