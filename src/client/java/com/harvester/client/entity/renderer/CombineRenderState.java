package com.harvester.client.entity.renderer;

import com.harvester.vehicle.VehicleType;
import com.harvester.vehicle.VehicleRig;
import net.minecraft.client.render.entity.state.EntityRenderState;
import java.util.Map;

public class CombineRenderState extends EntityRenderState {
    public VehicleType variant=VehicleType.COMBINE;
    public int color;
    public float yaw,wheels,rotor,engineRotor,headerLift,steering,workingStrength;
    public float inputSteer,inputDrive,controlPitch,animationTime;
    public double wheelTravel,yawTravel;
    public boolean harvesting,engineActive,headerEnabled=true;
    public VehicleRig.Pose bodyPose=VehicleRig.Pose.ZERO;
    public Map<String,Float> suspension=Map.of();
    public float limbSwingAnimationProgress;
}
