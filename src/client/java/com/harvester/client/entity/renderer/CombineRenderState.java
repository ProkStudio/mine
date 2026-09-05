package com.harvester.client.entity.renderer;

import com.harvester.vehicle.VehicleType;
import net.minecraft.client.render.entity.state.EntityRenderState;

public class CombineRenderState extends EntityRenderState {
    public VehicleType variant=VehicleType.COMBINE;
    public int color;
    public float yaw, wheels, rotor;
    public boolean harvesting, headerEnabled=true;
    // Retained for the original model source, which is no longer registered.
    public float limbSwingAnimationProgress;
}
