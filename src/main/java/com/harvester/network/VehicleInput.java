package com.harvester.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.*;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/** Only key bits cross the network: W, S, A, D, jump, sprint. No position or camera yaw. */
public record VehicleInput(byte keys) implements CustomPayload {
    public static final Id<VehicleInput> ID=new Id<>(Identifier.of("harvester","vehicle_input"));
    public static final PacketCodec<RegistryByteBuf,VehicleInput> CODEC=PacketCodec.tuple(PacketCodecs.BYTE,VehicleInput::keys,VehicleInput::new);
    @Override public Id<? extends CustomPayload> getId() { return ID; }
}
