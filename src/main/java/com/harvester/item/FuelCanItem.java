package com.harvester.item;

import com.harvester.HarvesterMod;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.*;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;

/** Partial refills retain unused fuel in the can; empty cans never refill themselves. */
public final class FuelCanItem extends Item {
    private final int size;
    public FuelCanItem(Settings settings,int size) { super(settings); this.size=size; }
    public int capacity() { var c=HarvesterMod.CONFIG.fuelCans; return size==0?c.small:size==1?c.medium:c.large; }
    public int remaining(ItemStack stack) {
        return Math.clamp(stack.getOrDefault(DataComponentTypes.CUSTOM_DATA,NbtComponent.DEFAULT).copyNbt().getInt("FuelLeft",capacity()),0,64000);
    }
    public void consume(ItemStack stack,int amount) {
        NbtCompound n=stack.getOrDefault(DataComponentTypes.CUSTOM_DATA,NbtComponent.DEFAULT).copyNbt();
        n.putInt("FuelLeft",Math.max(0,remaining(stack)-amount)); stack.set(DataComponentTypes.CUSTOM_DATA,NbtComponent.of(n));
    }
    @Override public Text getName(ItemStack stack) { return Text.literal("Канистра "+(size==0?"S":size==1?"M":"L")+" ("+remaining(stack)+")"); }
}
