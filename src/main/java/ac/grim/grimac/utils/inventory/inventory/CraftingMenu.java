package ac.grim.grimac.utils.inventory.inventory;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.inventory.Inventory;
import ac.grim.grimac.utils.inventory.InventoryStorage;
import ac.grim.grimac.utils.inventory.WrappedStack;
import ac.grim.grimac.utils.inventory.slot.ResultSlot;
import ac.grim.grimac.utils.inventory.slot.Slot;

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
    public WrappedStack quickMoveStack(int slotID) {
        WrappedStack itemstack = WrappedStack.empty();
        Slot slot = this.slots.get(slotID);
        if (slot != null && slot.hasItem()) {
            WrappedStack itemstack1 = slot.getItem();
            itemstack = itemstack1.copy();
            if (slotID == 0) {
                if (!this.moveItemStackTo(itemstack1, 10, 46, true)) {
                    return WrappedStack.empty();
                }
            } else if (slotID >= 10 && slotID < 46) {
                if (!this.moveItemStackTo(itemstack1, 1, 10, false)) {
                    if (slotID < 37) {
                        if (!this.moveItemStackTo(itemstack1, 37, 46, false)) {
                            return WrappedStack.empty();
                        }
                    } else if (!this.moveItemStackTo(itemstack1, 10, 37, false)) {
                        return WrappedStack.empty();
                    }
                }
            } else if (!this.moveItemStackTo(itemstack1, 10, 46, false)) {
                return WrappedStack.empty();
            }

            if (itemstack1.isEmpty()) {
                slot.set(WrappedStack.empty());
            }

            if (itemstack1.getCount() == itemstack.getCount()) {
                return WrappedStack.empty();
            }

            slot.onTake(player, itemstack1);
        }

        return itemstack;
    }

}
