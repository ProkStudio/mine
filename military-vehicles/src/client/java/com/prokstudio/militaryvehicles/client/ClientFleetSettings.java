package com.prokstudio.militaryvehicles.client;

import com.prokstudio.militaryvehicles.core.FleetTuning;
import com.prokstudio.militaryvehicles.core.VehicleKind;
import com.prokstudio.militaryvehicles.core.VehicleOperations;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;

/** Display only. Null means no snapshot from this connection; never present defaults as server truth. */
final class ClientFleetSettings {
    private static FleetTuning current;
    private ClientFleetSettings() {}
    static void clear() { current = null; }
    static void receive(FleetTuning settings) { current = settings; }
    private static Text flag(boolean enabled) {
        return Text.translatable("message.militaryvehicles." + (enabled ? "enabled" : "disabled"));
    }
    static void help(PlayerEntity player, VehicleKind kind) {
        FleetTuning c = current;
        if (c == null) { player.sendMessage(Text.translatable("help.militaryvehicles.rules_pending"), false); return; }
        player.sendMessage(Text.translatable("help.militaryvehicles.server_rules", flag(c.weaponsEnabled()),
            flag(c.supportEnabled()), c.engineFuelUnits(), c.nearbyVehicleLimit()), false);
        if (kind.armed()) player.sendMessage(Text.translatable("help.militaryvehicles.server_gun",
            c.shotFuelCost(), VehicleOperations.gunCooldown(c, kind)), false);
        switch (kind) {
            case TANKER -> player.sendMessage(Text.translatable("help.militaryvehicles.server_tanker",
                c.transferLimit(), c.fuelReserve(), c.serviceCooldownTicks()), false);
            case WORKSHOP -> player.sendMessage(Text.translatable("help.militaryvehicles.server_workshop",
                c.workshopRepair(), c.serviceCooldownTicks()), false);
            case RECOVERY -> player.sendMessage(Text.translatable("help.militaryvehicles.server_recovery",
                c.recoveryFuelCost(), c.fuelReserve(), c.serviceCooldownTicks()), false);
            default -> { }
        }
    }
}
