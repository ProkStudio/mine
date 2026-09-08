package com.prokstudio.militaryvehicles.config;

import com.prokstudio.militaryvehicles.MilitaryVehicles;
import com.prokstudio.militaryvehicles.core.FleetTuning;
import com.prokstudio.militaryvehicles.network.FleetSettingsPayload;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import java.io.IOException;
import java.util.Map;
import java.util.WeakHashMap;

/** Server-thread lifecycle. Integrated servers do not share a mutable client-side singleton. */
public final class ServerFleetConfig {
    private static final Map<MinecraftServer, FleetConfigFile> SERVERS = new WeakHashMap<>();
    private ServerFleetConfig() {}
    public static FleetTuning get(MinecraftServer server) {
        FleetConfigFile file = SERVERS.get(server);
        return file == null ? FleetTuning.DEFAULT : file.current();
    }
    private static FleetConfigFile store(MinecraftServer server) {
        return SERVERS.computeIfAbsent(server, ignored -> new FleetConfigFile(
            FabricLoader.getInstance().getConfigDir().resolve("militaryvehicles-server.properties")));
    }
    public static void register() {
        PayloadTypeRegistry.playS2C().register(FleetSettingsPayload.ID, FleetSettingsPayload.CODEC);
        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            try { store(server).reload(); }
            catch (IOException | IllegalArgumentException ex) {
                MilitaryVehicles.LOGGER.warn("Military Vehicles settings rejected; safe defaults remain active: {}", ex.getMessage());
            }
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(SERVERS::remove);
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> send(handler.player, get(server)));
        CommandRegistrationCallback.EVENT.register((dispatcher, access, environment) -> dispatcher.register(
            CommandManager.literal("militaryvehicles").then(CommandManager.literal("config")
                .executes(context -> {
                    context.getSource().sendFeedback(() -> Text.literal(get(context.getSource().getServer()).serialize()), false);
                    return 1;
                })
                .then(CommandManager.literal("reload")
                    .requires(CommandManager.requirePermissionLevel(CommandManager.ADMINS_CHECK))
                    .executes(context -> reload(context.getSource()))))));
    }
    private static int reload(ServerCommandSource source) {
        MinecraftServer server = source.getServer();
        final FleetTuning next;
        try { next = store(server).reload(); }
        catch (IOException | IllegalArgumentException ex) {
            MilitaryVehicles.LOGGER.warn("Military Vehicles settings reload rejected; previous rules retained: {}", ex.getMessage());
            source.sendError(Text.translatable("message.militaryvehicles.config_failed"));
            return 0;
        }
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) send(player, next);
        source.sendFeedback(() -> Text.translatable("message.militaryvehicles.config_reloaded"), true);
        return 1;
    }
    private static void send(ServerPlayerEntity player, FleetTuning settings) {
        if (ServerPlayNetworking.canSend(player, FleetSettingsPayload.ID))
            ServerPlayNetworking.send(player, new FleetSettingsPayload(settings));
    }
}
