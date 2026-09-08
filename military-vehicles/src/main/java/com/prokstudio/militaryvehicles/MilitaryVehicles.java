package com.prokstudio.militaryvehicles;
import com.prokstudio.militaryvehicles.entity.TruckEntity;
import com.prokstudio.militaryvehicles.init.MilitaryContent;
import com.prokstudio.militaryvehicles.network.TruckInput;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
public final class MilitaryVehicles implements ModInitializer {
    public static final String ID="militaryvehicles";
    public static final Logger LOGGER=LoggerFactory.getLogger(ID);
    @Override public void onInitialize() {
        MilitaryContent.register();
        PayloadTypeRegistry.playC2S().register(TruckInput.ID,TruckInput.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(TruckInput.ID,(payload,context)->{
            var player=context.player();
            if(player.getVehicle() instanceof TruckEntity truck&&truck.getId()==payload.entityId()) truck.acceptInput(player,Byte.toUnsignedInt(payload.keys()));
        });
    }
}
