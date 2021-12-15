package ac.grim.grimac.utils.inventory.inventory;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.inventory.Inventory;
import ac.grim.grimac.utils.inventory.InventoryStorage;
import ac.grim.grimac.utils.inventory.WrappedStack;
import ac.grim.grimac.utils.inventory.slot.Slot;
import org.bukkit.Material;

public class EnchantmentMenu extends AbstractContainerMenu{
    public EnchantmentMenu(GrimPlayer player, Inventory inventory) {
        super(player, inventory);

        InventoryStorage storage = new InventoryStorage(2);

        addSlot(new Slot(storage, 0) {
            @Override
            public boolean mayPlace(WrappedStack p_39508_) {
                return true;
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });

        addSlot(new Slot(storage, 1) {
            @Override
            public boolean mayPlace(WrappedStack p_39508_) {
                return p_39508_.getItem() == Material.LAPIS_LAZULI;
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
                if (!this.moveItemStackTo(itemstack1, 2, 38, true)) {
                    return WrappedStack.empty();
                }
            } else if (slotID == 1) {
                if (!this.moveItemStackTo(itemstack1, 2, 38, true)) {
                    return WrappedStack.empty();
                }
            } else if (itemstack1.getItem() == Material.LAPIS_LAZULI) {
                if (!this.moveItemStackTo(itemstack1, 1, 2, true)) {
                    return WrappedStack.empty();
                }
            } else {
                if (this.slots.get(0).hasItem() || !this.slots.get(0).mayPlace(itemstack1)) {
                    return WrappedStack.empty();
                }

                WrappedStack itemstack2 = itemstack1.copy();
                itemstack2.setCount(1);
                itemstack1.shrink(1);
                this.slots.get(0).set(itemstack2);
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
