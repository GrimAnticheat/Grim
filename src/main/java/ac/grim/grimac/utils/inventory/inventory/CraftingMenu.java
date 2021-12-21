package ac.grim.grimac.utils.inventory.inventory;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.inventory.Inventory;
import ac.grim.grimac.utils.inventory.InventoryStorage;
import ac.grim.grimac.utils.inventory.slot.ResultSlot;
import ac.grim.grimac.utils.inventory.slot.Slot;
import com.github.retrooper.packetevents.protocol.item.ItemStack;

public class CraftingMenu extends AbstractContainerMenu {
    public CraftingMenu(GrimPlayer player, Inventory playerInventory) {
        super(player, playerInventory);

        InventoryStorage storage = new InventoryStorage(10);

        addSlot(new ResultSlot(storage, 0));

        for (int i = 0; i < 9; i++) {
            addSlot(new Slot(storage, i + 1));
        }

        addFourRowPlayerInventory();
    }

    @Override
    public ItemStack quickMoveStack(int slotID) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(slotID);
        if (slot != null && slot.hasItem()) {
            ItemStack itemstack1 = slot.getItem();
            itemstack = itemstack1.copy();
            if (slotID == 0) {
                if (!this.moveItemStackTo(itemstack1, 10, 46, true)) {
                    return ItemStack.EMPTY;
                }
            } else if (slotID >= 10 && slotID < 46) {
                if (!this.moveItemStackTo(itemstack1, 1, 10, false)) {
                    if (slotID < 37) {
                        if (!this.moveItemStackTo(itemstack1, 37, 46, false)) {
                            return ItemStack.EMPTY;
                        }
                    } else if (!this.moveItemStackTo(itemstack1, 10, 37, false)) {
                        return ItemStack.EMPTY;
                    }
                }
            } else if (!this.moveItemStackTo(itemstack1, 10, 46, false)) {
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
