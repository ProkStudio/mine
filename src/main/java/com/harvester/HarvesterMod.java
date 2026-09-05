package com.harvester;

import com.harvester.config.HarvesterConfig;
import com.harvester.entity.CombineEntity;
import com.harvester.init.*;
import com.harvester.network.VehicleInput;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.*;
import org.slf4j.*;

public class HarvesterMod implements ModInitializer {
    public static final String MOD_ID="harvester";
    public static final Logger LOGGER=LoggerFactory.getLogger(MOD_ID);
    public static HarvesterConfig CONFIG;
    @Override public void onInitialize() {
        CONFIG=HarvesterConfig.load();
        ModEntities.register(); ModItems.register(); ModSounds.register();
        PayloadTypeRegistry.playC2S().register(VehicleInput.ID,VehicleInput.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(VehicleInput.ID,(payload,context)->{
            if(context.player().getVehicle() instanceof CombineEntity vehicle) vehicle.acceptInput(context.player(),payload.keys());
        });
        LOGGER.info("Harvester transport pack initialized");
    }
}
