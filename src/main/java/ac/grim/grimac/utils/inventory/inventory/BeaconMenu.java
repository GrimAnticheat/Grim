package ac.grim.grimac.utils.inventory.inventory;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.inventory.Inventory;
import ac.grim.grimac.utils.inventory.InventoryStorage;
import ac.grim.grimac.utils.inventory.WrappedStack;
import ac.grim.grimac.utils.inventory.slot.Slot;
import org.bukkit.Tag;

// Complete!
public class BeaconMenu extends AbstractContainerMenu{
    public BeaconMenu(GrimPlayer player, Inventory playerInventory) {
        super(player, playerInventory);

        InventoryStorage containerStorage = new InventoryStorage(1);

        addSlot(new Slot(containerStorage, 0) {
            @Override
            public boolean mayPlace(WrappedStack itemstack) {
                return Tag.ITEMS_BEACON_PAYMENT_ITEMS.isTagged(itemstack.getItem());
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });

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
                if (!this.moveItemStackTo(itemstack1, 1, 37, true)) {
                    return WrappedStack.empty();
                }
            } else if (!getSlot(0).hasItem() && getSlot(0).mayPlace(itemstack1) && itemstack1.getCount() == 1) {
                if (!this.moveItemStackTo(itemstack1, 0, 1, false)) {
                    return WrappedStack.empty();
                }
            } else if (slotID >= 1 && slotID < 28) {
                if (!this.moveItemStackTo(itemstack1, 28, 37, false)) {
                    return WrappedStack.empty();
                }
            } else if (slotID >= 28 && slotID < 37) {
                if (!this.moveItemStackTo(itemstack1, 1, 28, false)) {
                    return WrappedStack.empty();
                }
            } else if (!this.moveItemStackTo(itemstack1, 1, 37, false)) {
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
