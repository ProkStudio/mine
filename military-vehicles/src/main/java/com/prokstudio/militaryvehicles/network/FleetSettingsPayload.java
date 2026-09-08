package com.prokstudio.militaryvehicles.network;

import com.prokstudio.militaryvehicles.core.FleetTuning;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import java.util.Objects;

/** S2C only: a validated economy snapshot, never a client request to change server settings. */
public record FleetSettingsPayload(FleetTuning settings) implements CustomPayload {
    public FleetSettingsPayload { Objects.requireNonNull(settings); }
    public static final Id<FleetSettingsPayload> ID = new Id<>(Identifier.of("militaryvehicles", "fleet_settings_v1"));
    public static final PacketCodec<RegistryByteBuf, FleetSettingsPayload> CODEC = new PacketCodec<>() {
        @Override public FleetSettingsPayload decode(RegistryByteBuf buf) {
            return new FleetSettingsPayload(FleetTuning.parse(buf.readString(FleetTuning.MAX_TEXT_LENGTH)));
        }
        @Override public void encode(RegistryByteBuf buf, FleetSettingsPayload value) {
            buf.writeString(value.settings().serialize(), FleetTuning.MAX_TEXT_LENGTH);
        }
    };
    @Override public Id<? extends CustomPayload> getId() { return ID; }
}
