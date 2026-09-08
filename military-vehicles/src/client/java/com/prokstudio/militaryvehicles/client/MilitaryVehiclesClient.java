package com.prokstudio.militaryvehicles.client;
import com.prokstudio.militaryvehicles.core.ControlLatch;
import com.prokstudio.militaryvehicles.entity.TruckEntity;
import com.prokstudio.militaryvehicles.init.MilitaryContent;
import com.prokstudio.militaryvehicles.network.TruckInput;
import com.prokstudio.militaryvehicles.network.VehicleAction;
import com.prokstudio.militaryvehicles.network.FleetSettingsPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
public final class MilitaryVehiclesClient implements ClientModInitializer {
    private static KeyBinding key(String name,int code,KeyBinding.Category category) {
        return KeyBindingHelper.registerKeyBinding(new KeyBinding("key.militaryvehicles."+name,InputUtil.Type.KEYSYM,code,category));
    }
    private static boolean drain(KeyBinding key) {boolean pressed=false;while(key.wasPressed()) pressed=true;return pressed;}
    @Override public void onInitializeClient() {
        ClientPlayConnectionEvents.INIT.register((handler,client)->ClientFleetSettings.clear());
        ClientPlayConnectionEvents.DISCONNECT.register((handler,client)->ClientFleetSettings.clear());
        ClientPlayNetworking.registerGlobalReceiver(FleetSettingsPayload.ID,(payload,context)->ClientFleetSettings.receive(payload.settings()));
        for(var kind:com.prokstudio.militaryvehicles.core.VehicleKind.values())
            EntityRendererRegistry.register(MilitaryContent.vehicleEntity(kind),context->new TruckRenderer(context,kind));
        ClientTickEvents.END_CLIENT_TICK.register(TruckAudio::tick);
        var category=KeyBinding.Category.create(MilitaryContent.id("controls"));
        KeyBinding engine=key("engine",GLFW.GLFW_KEY_G,category),action=key("action",GLFW.GLFW_KEY_Z,category),
            deploy=key("deploy",GLFW.GLFW_KEY_V,category),crew=key("crew",GLFW.GLFW_KEY_C,category),help=key("help",GLFW.GLFW_KEY_K,category);
        ClientTickEvents.END_CLIENT_TICK.register(client->{
            // Drain even while in menus/outside a vehicle: commands must not leak into a later mount.
            boolean fire=drain(action),stabilize=drain(deploy),changeCrew=drain(crew),showHelp=drain(help);
            if(client.player==null||!(client.player.getVehicle() instanceof TruckEntity truck)) return;
            boolean focused=client.currentScreen==null&&client.isWindowFocused();
            if(focused&&showHelp) {
                client.player.sendMessage(Text.translatable("message.militaryvehicles.role_controls",action.getBoundKeyLocalizedText(),deploy.getBoundKeyLocalizedText(),crew.getBoundKeyLocalizedText(),help.getBoundKeyLocalizedText()),false);
                client.player.sendMessage(Text.translatable("help.militaryvehicles."+truck.kind().id),false);
                ClientFleetSettings.help(client.player,truck.kind());
            }
            if(focused&&ClientPlayNetworking.canSend(VehicleAction.ID)) {
                int command=changeCrew?3:stabilize?2:fire?1:0;
                if(command!=0) ClientPlayNetworking.send(new VehicleAction(truck.getId(),(byte)command));
            }
            if(truck.getFirstPassenger()!=client.player||!ClientPlayNetworking.canSend(TruckInput.ID)) return;
            int keys=ControlLatch.BRAKE;
            if(focused&&!truck.soloGunner()) {
                keys=0;if(client.options.forwardKey.isPressed()) keys|=ControlLatch.FORWARD;if(client.options.backKey.isPressed()) keys|=ControlLatch.BACK;
                if(client.options.leftKey.isPressed()) keys|=ControlLatch.LEFT;if(client.options.rightKey.isPressed()) keys|=ControlLatch.RIGHT;
                if(client.options.jumpKey.isPressed()) keys|=ControlLatch.BRAKE;if(engine.isPressed()) keys|=ControlLatch.ENGINE;
            }
            ClientPlayNetworking.send(new TruckInput(truck.getId(),(byte)keys));
        });
    }
}
