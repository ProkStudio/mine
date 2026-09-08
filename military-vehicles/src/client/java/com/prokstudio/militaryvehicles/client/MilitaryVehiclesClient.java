package com.prokstudio.militaryvehicles.client;
import com.prokstudio.militaryvehicles.core.ControlLatch;
import com.prokstudio.militaryvehicles.entity.TruckEntity;
import com.prokstudio.militaryvehicles.init.MilitaryContent;
import com.prokstudio.militaryvehicles.network.TruckInput;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
public final class MilitaryVehiclesClient implements ClientModInitializer {
    @Override public void onInitializeClient() {
        EntityRendererRegistry.register(MilitaryContent.TRUCK_ENTITY,TruckRenderer::new);
        var category=KeyBinding.Category.create(MilitaryContent.id("controls"));
        KeyBinding engine=KeyBindingHelper.registerKeyBinding(new KeyBinding("key.militaryvehicles.engine",InputUtil.Type.KEYSYM,GLFW.GLFW_KEY_G,category));
        ClientTickEvents.END_CLIENT_TICK.register(client->{
            if(client.player==null||!(client.player.getVehicle() instanceof TruckEntity truck)||truck.getFirstPassenger()!=client.player||!ClientPlayNetworking.canSend(TruckInput.ID)) return;
            int keys=ControlLatch.BRAKE;
            if(client.currentScreen==null&&client.isWindowFocused()) {
                keys=0;if(client.options.forwardKey.isPressed()) keys|=ControlLatch.FORWARD;if(client.options.backKey.isPressed()) keys|=ControlLatch.BACK;
                if(client.options.leftKey.isPressed()) keys|=ControlLatch.LEFT;if(client.options.rightKey.isPressed()) keys|=ControlLatch.RIGHT;
                if(client.options.jumpKey.isPressed()) keys|=ControlLatch.BRAKE;if(engine.isPressed()) keys|=ControlLatch.ENGINE;
            }
            ClientPlayNetworking.send(new TruckInput(truck.getId(),(byte)keys));
        });
    }
}
