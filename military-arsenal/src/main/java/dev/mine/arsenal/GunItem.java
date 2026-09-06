package dev.mine.arsenal;

import dev.mine.arsenal.core.*;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.*;
import net.minecraft.world.World;
import java.util.List;
import java.util.function.Consumer;

public final class GunItem extends Item {
    public final Weapon weapon;
    public GunItem(Settings settings,Weapon weapon) { super(settings); this.weapon=weapon; }
    public Magazine magazine(ItemStack stack) {
        var n=stack.getOrDefault(DataComponentTypes.CUSTOM_DATA,NbtComponent.DEFAULT).copyNbt();
        return Magazine.read(weapon,n.getInt("ArsenalRounds",0),n.getString("ArsenalAmmo",""),n.getString("ArsenalMode",""));
    }
    public void magazine(ItemStack stack,Magazine state) {
        NbtCompound n=stack.getOrDefault(DataComponentTypes.CUSTOM_DATA,NbtComponent.DEFAULT).copyNbt();
        n.putInt("ArsenalVersion",1); n.putInt("ArsenalRounds",state.rounds());
        n.putString("ArsenalAmmo",state.ammo().id); n.putString("ArsenalMode",state.mode().name());
        stack.set(DataComponentTypes.CUSTOM_DATA,NbtComponent.of(n));
    }
    public static void pose(ItemStack stack,int frame,boolean aim,boolean reload) {
        var next=new CustomModelDataComponent(List.of((float)frame),List.of(aim,reload),List.of(),List.of());
        if(!next.equals(stack.get(DataComponentTypes.CUSTOM_MODEL_DATA))) stack.set(DataComponentTypes.CUSTOM_MODEL_DATA,next);
    }
    public static boolean aiming(ItemStack stack) {
        return Boolean.TRUE.equals(stack.getOrDefault(DataComponentTypes.CUSTOM_MODEL_DATA,CustomModelDataComponent.DEFAULT).getFlag(0));
    }
    public static boolean reloading(ItemStack stack) {
        return Boolean.TRUE.equals(stack.getOrDefault(DataComponentTypes.CUSTOM_MODEL_DATA,CustomModelDataComponent.DEFAULT).getFlag(1));
    }
    @Override public boolean isItemBarVisible(ItemStack stack) { return magazine(stack).rounds()<weapon.capacity; }
    @Override public int getItemBarStep(ItemStack stack) { return Math.round(13f*magazine(stack).rounds()/weapon.capacity); }
    @Override public int getItemBarColor(ItemStack stack) { return magazine(stack).rounds()==0?0xe67862:0x76b89b; }
    @Override public ActionResult use(World world,PlayerEntity player,Hand hand) { return ActionResult.SUCCESS; }
    @Override public void appendTooltip(ItemStack stack,TooltipContext context,TooltipDisplayComponent display,Consumer<Text> out,TooltipType type) {
        super.appendTooltip(stack,context,display,out,type);
        Magazine m=magazine(stack);
        out.accept(Text.translatable("tooltip.arsenal.magazine",m.rounds(),weapon.capacity).formatted(Formatting.GOLD));
        out.accept(Text.translatable("item.arsenal."+m.ammo().id).formatted(Formatting.GRAY));
        out.accept(Text.translatable("mode.arsenal."+m.mode().name().toLowerCase(java.util.Locale.ROOT)).formatted(Formatting.AQUA));
        out.accept(Text.translatable("tooltip.arsenal.controls").formatted(Formatting.DARK_GRAY));
        if(weapon.ammunition.size()>1) out.accept(Text.translatable("tooltip.arsenal.ammo_cycle").formatted(Formatting.GRAY));
    }
}
