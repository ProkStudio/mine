package com.harvester.item;

import com.harvester.HarvesterMod;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.*;
import net.minecraft.world.World;
import java.util.function.Consumer;

/** Partial refills retain unused fuel; instructions are visible on the can itself. */
public final class FuelCanItem extends Item {
    private final int size;
    public FuelCanItem(Settings settings,int size) { super(settings); this.size=size; }
    public int capacity() {
        if(HarvesterMod.CONFIG==null) return size==0?400:size==1?1200:3200;
        var c=HarvesterMod.CONFIG.fuelCans; return size==0?c.small:size==1?c.medium:c.large;
    }
    public int remaining(ItemStack stack) {
        return Math.clamp(stack.getOrDefault(DataComponentTypes.CUSTOM_DATA,NbtComponent.DEFAULT).copyNbt().getInt("FuelLeft",capacity()),0,64000);
    }
    public void consume(ItemStack stack,int amount) {
        NbtCompound n=stack.getOrDefault(DataComponentTypes.CUSTOM_DATA,NbtComponent.DEFAULT).copyNbt();
        n.putInt("FuelLeft",Math.max(0,remaining(stack)-amount)); stack.set(DataComponentTypes.CUSTOM_DATA,NbtComponent.of(n));
    }
    @Override public Text getName(ItemStack stack) { return Text.literal("Канистра "+(size==0?"S":size==1?"M":"L")+" ("+remaining(stack)+")"); }
    @Override public void appendTooltip(ItemStack stack,TooltipContext context,TooltipDisplayComponent display,Consumer<Text> text,TooltipType type) {
        super.appendTooltip(stack,context,display,text,type);
        text.accept(Text.literal("Заправка: ПКМ канистрой по поставленной технике").formatted(Formatting.YELLOW));
        text.accept(Text.literal("Без Shift — Shift + ПКМ забирает машину").formatted(Formatting.GRAY));
        text.accept(Text.literal("Остаток топлива: "+remaining(stack)).formatted(Formatting.GRAY));
        if(remaining(stack)==0) text.accept(Text.literal("Канистра пуста. Возьмите полную в creative.").formatted(Formatting.RED));
    }
    @Override public ActionResult use(World world,PlayerEntity player,Hand hand) {
        if(!world.isClient()) player.sendMessage(Text.literal("Наведитесь на поставленную машину и нажмите ПКМ без Shift. Канистра должна быть в руке."),true);
        return ActionResult.SUCCESS;
    }
}
