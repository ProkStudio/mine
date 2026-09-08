package com.prokstudio.militaryvehicles.item;
import com.prokstudio.militaryvehicles.core.TruckSpec;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.*;
import net.minecraft.item.*;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import java.util.function.Consumer;
public final class FuelCanItem extends Item {
    public FuelCanItem(Settings settings) { super(settings); }
    public int remaining(ItemStack stack) { var n=stack.getOrDefault(DataComponentTypes.CUSTOM_DATA,NbtComponent.DEFAULT).copyNbt();return Math.clamp(n.getInt("Fuel",TruckSpec.CAN_FUEL),0,TruckSpec.CAN_FUEL); }
    public void consume(ItemStack stack,int used) {
        if(used<0||used>remaining(stack)) throw new IllegalArgumentException("Fuel transfer exceeds can");
        var n=stack.getOrDefault(DataComponentTypes.CUSTOM_DATA,NbtComponent.DEFAULT).copyNbt();n.putInt("Fuel",remaining(stack)-used);
        stack.set(DataComponentTypes.CUSTOM_DATA,NbtComponent.of(n));
    }
    @Override public void appendTooltip(ItemStack stack,TooltipContext context,TooltipDisplayComponent display,Consumer<Text> text,TooltipType type) {
        super.appendTooltip(stack,context,display,text,type);text.accept(Text.translatable("tooltip.militaryvehicles.can",remaining(stack),TruckSpec.CAN_FUEL).formatted(Formatting.GRAY));
    }
}
