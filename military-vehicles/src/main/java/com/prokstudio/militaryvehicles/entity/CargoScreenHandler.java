package com.prokstudio.militaryvehicles.entity;
import com.prokstudio.militaryvehicles.core.TruckSpec;
import net.minecraft.entity.player.*;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.*;
import net.minecraft.screen.slot.Slot;
/** Vanilla 9x3 wire/layout compatibility. The server blocks nested containers on all insertion paths. */
public final class CargoScreenHandler extends ScreenHandler {
    private final Inventory cargo;
    public CargoScreenHandler(int syncId,PlayerInventory playerInventory,Inventory cargo) {
        super(ScreenHandlerType.GENERIC_9X3,syncId);checkSize(cargo,TruckSpec.SLOTS);this.cargo=cargo;
        cargo.onOpen(playerInventory.player);
        for(int row=0;row<3;row++) for(int col=0;col<9;col++) addSlot(new Slot(cargo,col+row*9,8+col*18,18+row*18) {
            @Override public boolean canInsert(ItemStack stack) { return stack.getItem().canBeNested(); }
        });
        for(int row=0;row<3;row++) for(int col=0;col<9;col++) addSlot(new Slot(playerInventory,col+(row+1)*9,8+col*18,85+row*18));
        for(int col=0;col<9;col++) addSlot(new Slot(playerInventory,col,8+col*18,143));
    }
    @Override public boolean canUse(PlayerEntity player) { return cargo.canPlayerUse(player); }
    @Override public ItemStack quickMove(PlayerEntity player,int index) {
        if(index<0||index>=slots.size()||!canUse(player)) return ItemStack.EMPTY;
        Slot slot=slots.get(index);if(!slot.hasStack()) return ItemStack.EMPTY;
        ItemStack stack=slot.getStack(),original=stack.copy();
        if(index<TruckSpec.SLOTS) { if(!insertItem(stack,TruckSpec.SLOTS,slots.size(),true)) return ItemStack.EMPTY; }
        else if(!stack.getItem().canBeNested()||!insertItem(stack,0,TruckSpec.SLOTS,false)) return ItemStack.EMPTY;
        if(stack.isEmpty()) slot.setStack(ItemStack.EMPTY);else slot.markDirty();return original;
    }
    @Override public void onClosed(PlayerEntity player) { super.onClosed(player);cargo.onClose(player); }
}
