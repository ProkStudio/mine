package com.prokstudio.militaryvehicles.network;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
/** Entity ID prevents delayed commands from steering a different vehicle after a seat change. */
public record TruckInput(int entityId,byte keys) implements CustomPayload {
    public static final Id<TruckInput> ID=new Id<>(Identifier.of("militaryvehicles","truck_input_v1"));
    public static final PacketCodec<RegistryByteBuf,TruckInput> CODEC=PacketCodec.tuple(PacketCodecs.VAR_INT,TruckInput::entityId,PacketCodecs.BYTE,TruckInput::keys,TruckInput::new);
    @Override public Id<? extends CustomPayload> getId() { return ID; }
}
