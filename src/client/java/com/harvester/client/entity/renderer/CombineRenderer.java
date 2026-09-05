package com.harvester.client.entity.renderer;

import com.harvester.entity.CombineEntity;
import com.harvester.vehicle.VehicleType;
import net.minecraft.block.*;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.*;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;

/** Lightweight voxel vehicles rendered with vanilla block materials; no animation library. */
public class CombineRenderer extends EntityRenderer<CombineEntity,CombineRenderState> {
    private static final Block[] PALETTE={Blocks.LIME_CONCRETE,Blocks.WHITE_CONCRETE,Blocks.ORANGE_CONCRETE,Blocks.MAGENTA_CONCRETE,
        Blocks.LIGHT_BLUE_CONCRETE,Blocks.YELLOW_CONCRETE,Blocks.PINK_CONCRETE,Blocks.GRAY_CONCRETE,
        Blocks.LIGHT_GRAY_CONCRETE,Blocks.CYAN_CONCRETE,Blocks.PURPLE_CONCRETE,Blocks.BLUE_CONCRETE,
        Blocks.BROWN_CONCRETE,Blocks.GREEN_CONCRETE,Blocks.RED_CONCRETE,Blocks.BLACK_CONCRETE};
    public CombineRenderer(EntityRendererFactory.Context context) { super(context); shadowRadius=1; }
    @Override public CombineRenderState createRenderState() { return new CombineRenderState(); }
    @Override public void updateRenderState(CombineEntity entity,CombineRenderState state,float tickProgress) {
        super.updateRenderState(entity,state,tickProgress);
        state.variant=entity.variant(); state.color=entity.getColor(); state.yaw=entity.getLerpedYaw(tickProgress);
        state.wheels=entity.wheelAngle(); state.rotor=entity.rotorAngle(); state.harvesting=entity.isHarvesting(); state.headerEnabled=entity.isHeaderEnabled();
    }
    @Override public void render(CombineRenderState s,MatrixStack m,OrderedRenderCommandQueue q,CameraRenderState camera) {
        super.render(s,m,q,camera);
        m.push(); m.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-s.yaw));
        float w=s.variant.width, h=s.variant.height;
        Block body=PALETTE[Math.floorMod(s.color,PALETTE.length)];
        box(m,q,s,body,0,.55,0,w*.65,.45,w*.8);
        switch(s.variant.family) {
            case COMBINE -> {
                box(m,q,s,body,0,1.25,-.45,w*.62,.9,1.4);
                // Open front and roof above the original cockpit camera height.
                box(m,q,s,Blocks.GRAY_CONCRETE,-.65,1.8,.45,.12,1.1,.12);
                box(m,q,s,Blocks.GRAY_CONCRETE,.65,1.8,.45,.12,1.1,.12);
                box(m,q,s,body,0,2.6,.45,1.6,.14,1.4);
                box(m,q,s,Blocks.BLACK_CONCRETE,0,.45,w*.55,w,.2,.7);
                m.push(); m.translate(0,s.headerEnabled?.75:1.05,w*.55); m.multiply(RotationAxis.POSITIVE_X.rotation(s.rotor));
                box(m,q,s,Blocks.IRON_BLOCK,0,0,0,w*.95,.08,.08);
                box(m,q,s,body,0,.23,0,w*.95,.08,.12); box(m,q,s,body,0,-.23,0,w*.95,.08,.12); m.pop();
                wheels(m,q,s,w,.45);
            }
            case DOZER -> {
                box(m,q,s,body,0,1.25,0,w*.6,.8,1);
                box(m,q,s,Blocks.IRON_BLOCK,0,.65,w*.6,w,.9,.2);
                wheels(m,q,s,w,.5);
                for(int side:new int[]{-1,1}) box(m,q,s,Blocks.BLACK_CONCRETE,side*w*.4,.3,0,.35,.3,w*.9);
            }
            case PICKUP -> {
                box(m,q,s,body,0,1.2,.4,w*.7,.7,.8);
                box(m,q,s,Blocks.LIGHT_BLUE_STAINED_GLASS,0,1.35,.83,w*.5,.35,.04);
                box(m,q,s,body,0,.8,-.6,w*.7,.25,.7); wheels(m,q,s,w,.28);
            }
            case MOTORCYCLE -> {
                box(m,q,s,Blocks.BLACK_CONCRETE,0,.85,-.2,.45,.15,1.2);
                box(m,q,s,Blocks.IRON_BLOCK,0,1.1,.5,.8,.08,.08);
                for(int side:new int[]{-1,1}) {
                    m.push(); m.translate(0,.32,side*.72); m.multiply(RotationAxis.POSITIVE_X.rotation(s.wheels));
                    box(m,q,s,Blocks.BLACK_CONCRETE,0,0,0,.23,.6,.6); m.pop();
                }
            }
            case BOAT -> {
                box(m,q,s,body,-w*.42,.7,0,.18,.55,w); box(m,q,s,body,w*.42,.7,0,.18,.55,w);
                box(m,q,s,body,0,.7,w*.5,w*.8,.4,.3);
                m.push(); m.translate(0,.1,-w*.5); m.multiply(RotationAxis.POSITIVE_Z.rotation(s.rotor));
                box(m,q,s,Blocks.IRON_BLOCK,0,0,0,.65,.1,.1); m.pop();
            }
            case PLANE -> {
                box(m,q,s,body,0,.85,0,.8,.55,w); box(m,q,s,body,0,.75,0,w,.12,.65);
                box(m,q,s,body,0,.9,-w*.4,w*.55,.1,.45);
                m.push(); m.translate(0,.9,w*.52); m.multiply(RotationAxis.POSITIVE_Z.rotation(s.rotor));
                box(m,q,s,Blocks.BLACK_CONCRETE,0,0,0,1.3,.08,.08); m.pop(); wheels(m,q,s,w*.6,.2);
            }
            case HELICOPTER -> {
                box(m,q,s,body,0,1,0,1.2,.8,1.5); box(m,q,s,body,0,1,-1.2,.2,.2,1.5);
                m.push(); m.translate(0,h,0); m.multiply(RotationAxis.POSITIVE_Y.rotation(s.rotor));
                box(m,q,s,Blocks.BLACK_CONCRETE,0,0,0,w*1.4,.07,.15); box(m,q,s,Blocks.BLACK_CONCRETE,0,0,0,.15,.07,w*1.4); m.pop();
                for(int side:new int[]{-1,1}) box(m,q,s,Blocks.IRON_BLOCK,side*.7,.2,0,.1,.1,1.9);
            }
            case DRONE -> {
                box(m,q,s,body,0,.45,0,w,.12,.14); box(m,q,s,body,0,.45,0,.14,.12,w);
                for(int a:new int[]{-1,1}) for(int b:new int[]{-1,1}) {
                    m.push(); m.translate(a*w*.4,.65,b*w*.4); m.multiply(RotationAxis.POSITIVE_Y.rotation(s.rotor*a*b));
                    box(m,q,s,Blocks.BLACK_CONCRETE,0,0,0,.65,.05,.08); m.pop();
                }
            }
        }
        m.pop();
    }
    private static void wheels(MatrixStack m,OrderedRenderCommandQueue q,CombineRenderState s,float width,double radius) {
        for(int side:new int[]{-1,1}) for(int axle:new int[]{-1,1}) {
            m.push(); m.translate(side*width*.4,radius,axle*width*.3); m.multiply(RotationAxis.POSITIVE_X.rotation(s.wheels));
            box(m,q,s,Blocks.BLACK_CONCRETE,0,0,0,.3,radius*2,radius*2);
            box(m,q,s,Blocks.IRON_BLOCK,side*.16,0,0,.04,radius*.8,radius*.8); m.pop();
        }
    }
    private static void box(MatrixStack m,OrderedRenderCommandQueue q,CombineRenderState s,Block block,
                            double x,double y,double z,double sx,double sy,double sz) {
        m.push(); m.translate(x-sx/2,y-sy/2,z-sz/2); m.scale((float)sx,(float)sy,(float)sz);
        q.getBatchingQueue(0).submitBlock(m,block.getDefaultState(),s.light,OverlayTexture.DEFAULT_UV,0); m.pop();
    }
}
