package dev.mine.arsenal;

import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.*;
import net.minecraft.world.World;
import java.util.function.Consumer;

/** Hand-thrown contact grenade, separate from the Arc G40 ammunition. */
public final class GrenadeItem extends Item {
    public GrenadeItem(Settings settings) { super(settings); }
    @Override public ActionResult use(World world,PlayerEntity player,Hand hand) {
        if(player instanceof ServerPlayerEntity server) Arsenal.SERVICE.throwGrenade(server,hand);
        return ActionResult.SUCCESS;
    }
    @Override public void appendTooltip(ItemStack stack,TooltipContext context,TooltipDisplayComponent display,Consumer<Text> out,TooltipType type) {
        super.appendTooltip(stack,context,display,out,type);
        out.accept(Text.translatable("tooltip.arsenal.throw").formatted(Formatting.GRAY));
        out.accept(Text.translatable("tooltip.arsenal.contact").formatted(Formatting.DARK_GRAY));
    }
}
