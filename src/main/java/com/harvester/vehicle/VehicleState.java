package com.harvester.vehicle;

import com.mojang.serialization.DynamicOps;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import java.util.List;

/** Minecraft adapter for the shared item/world save format. No UUID/position/passengers. */
public record VehicleState(VehicleType type, int fuel, int condition, int color, boolean headerEnabled,
                           int workCooldown, List<ItemStack> cargo) {
    public static final int VERSION = VehicleStateCodec.VERSION;

    public NbtCompound encode(DynamicOps<NbtElement> ops) {
        VehicleStateCodec.State<ItemStack> state = new VehicleStateCodec.State<>(
                type, fuel, condition, color, headerEnabled, workCooldown, cargo);
        NbtElement encoded = VehicleStateCodec.encode(ops, ItemStack.OPTIONAL_CODEC, state);
        if (!(encoded instanceof NbtCompound compound)) {
            throw new IllegalArgumentException("Vehicle state requires compound NBT operations");
        }
        return compound;
    }

    public static VehicleState decode(NbtCompound n, DynamicOps<NbtElement> ops) {
        VehicleStateCodec.State<ItemStack> state = VehicleStateCodec.decode(ops, ItemStack.OPTIONAL_CODEC, n);
        return new VehicleState(state.type(), state.fuel(), state.condition(), state.color(),
                state.headerEnabled(), state.workCooldown(), state.cargo());
    }
}
