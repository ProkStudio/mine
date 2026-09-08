package com.prokstudio.militaryvehicles.network;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
/** Intent only. Server chooses crew station, aim, target, resources and cooldown. */
public record VehicleAction(int entityId,byte action) implements CustomPayload {
    public static final Id<VehicleAction> ID=new Id<>(Identifier.of("militaryvehicles","vehicle_action_v1"));
    public static final PacketCodec<RegistryByteBuf,VehicleAction> CODEC=PacketCodec.tuple(PacketCodecs.VAR_INT,VehicleAction::entityId,PacketCodecs.BYTE,VehicleAction::action,VehicleAction::new);
    @Override public Id<? extends CustomPayload> getId() {return ID;}
}
