package dev.mine.arsenal;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.*;
import net.minecraft.network.packet.CustomPayload;

public final class ArsenalPackets {
    public static final int FIRE=1,AIM=2,RELOAD=4,MODE=8,AMMO=16,INSPECT=32;
    public static final byte SHOT=0,RELOADING=1,INSPECTING=2,HIT=3,STOP=4;
    private ArsenalPackets() {}
    /** No client-selected damage, target, origin, ammo, elapsed time or fire rate. */
    public record Input(byte keys) implements CustomPayload {
        public static final Id<Input> ID=new Id<>(Arsenal.id("input"));
        public static final PacketCodec<RegistryByteBuf,Input> CODEC=PacketCodec.tuple(PacketCodecs.BYTE,Input::keys,Input::new);
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }
    public record Feedback(int entity,byte event,int duration) implements CustomPayload {
        public static final Id<Feedback> ID=new Id<>(Arsenal.id("feedback"));
        public static final PacketCodec<RegistryByteBuf,Feedback> CODEC=PacketCodec.tuple(PacketCodecs.VAR_INT,Feedback::entity,PacketCodecs.BYTE,Feedback::event,PacketCodecs.VAR_INT,Feedback::duration,Feedback::new);
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }
}
