package com.harvester.client;

import com.harvester.client.entity.renderer.CombineRenderer;
import com.harvester.client.sound.VehicleAudio;
import com.harvester.entity.CombineEntity;
import com.harvester.init.ModEntities;
import com.harvester.network.VehicleInput;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public class HarvesterModClient implements ClientModInitializer {
    @Override public void onInitializeClient() {
        EntityRendererRegistry.register(ModEntities.COMBINE,CombineRenderer::new);
        ClientTickEvents.END_CLIENT_TICK.register(client->{
            VehicleAudio.tick(client);
            if(client.player==null || !(client.player.getVehicle() instanceof CombineEntity vehicle) || vehicle.getFirstPassenger()!=client.player || !ClientPlayNetworking.canSend(VehicleInput.ID)) return;
            int keys=0;
            if(client.currentScreen==null) {
                if(client.options.forwardKey.isPressed()) keys|=1;
                if(client.options.backKey.isPressed()) keys|=2;
                if(client.options.leftKey.isPressed()) keys|=4;
                if(client.options.rightKey.isPressed()) keys|=8;
                if(client.options.jumpKey.isPressed()) keys|=16;
                if(client.options.sprintKey.isPressed()) keys|=32;
            }
            ClientPlayNetworking.send(new VehicleInput((byte)keys));
        });
    }
}
