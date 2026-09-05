package com.harvester.vehicle;

import com.mojang.serialization.DynamicOps;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.*;
import java.util.*;

/** One codec boundary for item pickup and world saves. No UUID/position/passengers. */
public record VehicleState(VehicleType type, int fuel, int condition, int color, boolean headerEnabled,
                           int workCooldown, List<ItemStack> cargo) {
    public static final int VERSION=1;
    public NbtCompound encode(DynamicOps<NbtElement> ops) {
        NbtCompound n=new NbtCompound();
        n.putInt("Version",VERSION); n.putString("Type",type.id); n.putInt("Fuel",fuel);
        n.putInt("Condition",condition); n.putInt("Color",color); n.putBoolean("HeaderEnabled",headerEnabled);
        n.putInt("WorkCooldown",workCooldown);
        // Encoding failures abort pickup rather than deleting the original cargo.
        n.put("Inventory",ItemStack.OPTIONAL_CODEC.listOf().encodeStart(ops,cargo).getOrThrow());
        return n;
    }
    public static VehicleState decode(NbtCompound n, DynamicOps<NbtElement> ops) {
        if(n.getInt("Version",VERSION)!=VERSION) throw new IllegalArgumentException("Unsupported vehicle state version");
        VehicleType type=VehicleType.fromId(n.getString("Type",VehicleType.COMBINE.id));
        List<ItemStack> cargo=n.contains("Inventory") ? ItemStack.OPTIONAL_CODEC.listOf().parse(ops,n.get("Inventory")).getOrThrow() : List.of();
        if(cargo.size()>54) throw new IllegalArgumentException("Cargo exceeds 54 slots");
        return new VehicleState(type,Math.clamp(n.getInt("Fuel",0),0,64000),
            Math.clamp(n.getInt("Condition",type.durability),0,10000),Math.clamp(n.getInt("Color",0),0,15),
            n.getBoolean("HeaderEnabled",true),Math.clamp(n.getInt("WorkCooldown",0),0,100),cargo);
    }
}
