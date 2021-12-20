package ac.grim.grimac.utils.inventory.inventory;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.inventory.Inventory;
import ac.grim.grimac.utils.inventory.InventoryStorage;
import ac.grim.grimac.utils.inventory.ItemStack;
import ac.grim.grimac.utils.inventory.slot.FurnaceFuelSlot;
import ac.grim.grimac.utils.inventory.slot.FurnaceResultSlot;
import ac.grim.grimac.utils.inventory.slot.Slot;
import org.bukkit.Bukkit;
import org.bukkit.inventory.FurnaceRecipe;

import java.util.concurrent.atomic.AtomicBoolean;

public class FurnaceMenu extends AbstractContainerMenu {
    public FurnaceMenu(GrimPlayer player, Inventory playerInventory) {
        super(player, playerInventory);

        InventoryStorage containerStorage = new InventoryStorage(3);

        // Top slot, any item can go here
        addSlot(new Slot(containerStorage, 0));
        addSlot(new FurnaceFuelSlot(containerStorage, 1));
        addSlot(new FurnaceResultSlot(containerStorage, 2));

        addFourRowPlayerInventory();
    }

    @Override
    public ItemStack quickMoveStack(int slotID) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(slotID);
        if (slot != null && slot.hasItem()) {
            ItemStack itemstack1 = slot.getItem();
            itemstack = itemstack1.copy();
            if (slotID == 2) {
                if (!this.moveItemStackTo(itemstack1, 3, 39, true)) {
                    return ItemStack.EMPTY;
                }
            } else if (slotID != 1 && slotID != 0) {
                AtomicBoolean canSmelt = new AtomicBoolean(false);

                // Check if the item can be smelted
                Bukkit.recipeIterator().forEachRemaining((recipe) -> {
                    if (recipe instanceof FurnaceRecipe) {
                        FurnaceRecipe furnaceRecipe = (FurnaceRecipe) recipe;
                        if (furnaceRecipe.getInput().isSimilar(itemstack1.getStack())) {
                            canSmelt.set(true);
                        }
                    }
                });

                if (canSmelt.get()) {
                    if (!this.moveItemStackTo(itemstack1, 0, 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (itemstack1.getItem().isFuel()) {
                    if (!this.moveItemStackTo(itemstack1, 1, 2, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (slotID >= 3 && slotID < 30) {
                    if (!this.moveItemStackTo(itemstack1, 30, 39, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (slotID >= 30 && slotID < 39 && !this.moveItemStackTo(itemstack1, 3, 30, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(itemstack1, 3, 39, false)) {
                return ItemStack.EMPTY;
            }

            if (itemstack1.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            }

            if (itemstack1.getAmount() == itemstack.getAmount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(player, itemstack1);
        }

        return itemstack;
    }
}
