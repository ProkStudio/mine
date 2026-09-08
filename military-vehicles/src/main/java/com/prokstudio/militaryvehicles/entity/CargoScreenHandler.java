package com.prokstudio.militaryvehicles.entity;
import net.minecraft.entity.player.*;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.*;
import net.minecraft.screen.slot.Slot;
/** Vanilla 9x1/9x2/9x3 wire/layout compatibility. Every insertion path blocks nested containers. */
public final class CargoScreenHandler extends ScreenHandler {
    private final Inventory cargo;
    private final int cargoSlots;
    private static ScreenHandlerType<?> typeFor(int size) {
        return switch(size) {case 9->ScreenHandlerType.GENERIC_9X1;case 18->ScreenHandlerType.GENERIC_9X2;case 27->ScreenHandlerType.GENERIC_9X3;default->throw new IllegalArgumentException("Unsupported cargo layout");};
    }
    public CargoScreenHandler(int syncId,PlayerInventory playerInventory,Inventory cargo) {
        super(typeFor(cargo.size()),syncId);this.cargo=cargo;cargoSlots=cargo.size();int rows=cargoSlots/9;
        checkSize(cargo,cargoSlots);cargo.onOpen(playerInventory.player);
        for(int row=0;row<rows;row++) for(int col=0;col<9;col++) addSlot(new Slot(cargo,col+row*9,8+col*18,18+row*18) {
            @Override public boolean canInsert(ItemStack stack) { return stack.getItem().canBeNested(); }
        });
        for(int row=0;row<3;row++) for(int col=0;col<9;col++) addSlot(new Slot(playerInventory,col+(row+1)*9,8+col*18,31+rows*18+row*18));
        for(int col=0;col<9;col++) addSlot(new Slot(playerInventory,col,8+col*18,89+rows*18));
    }
    @Override public boolean canUse(PlayerEntity player) { return cargo.canPlayerUse(player); }
    @Override public ItemStack quickMove(PlayerEntity player,int index) {
        if(index<0||index>=slots.size()||!canUse(player)) return ItemStack.EMPTY;
        Slot slot=slots.get(index);if(!slot.hasStack()) return ItemStack.EMPTY;
        ItemStack stack=slot.getStack(),original=stack.copy();
        if(index<cargoSlots) { if(!insertItem(stack,cargoSlots,slots.size(),true)) return ItemStack.EMPTY; }
        else if(!stack.getItem().canBeNested()||!insertItem(stack,0,cargoSlots,false)) return ItemStack.EMPTY;
        if(stack.isEmpty()) slot.setStack(ItemStack.EMPTY);else slot.markDirty();return original;
    }
    @Override public void onClosed(PlayerEntity player) { super.onClosed(player);cargo.onClose(player); }
}
