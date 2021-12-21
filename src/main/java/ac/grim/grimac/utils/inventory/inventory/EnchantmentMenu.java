package ac.grim.grimac.utils.inventory.inventory;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.inventory.Inventory;
import ac.grim.grimac.utils.inventory.InventoryStorage;
import ac.grim.grimac.utils.inventory.slot.Slot;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;

public class EnchantmentMenu extends AbstractContainerMenu{
    public EnchantmentMenu(GrimPlayer player, Inventory inventory) {
        super(player, inventory);

        InventoryStorage storage = new InventoryStorage(2);

        addSlot(new Slot(storage, 0) {
            @Override
            public boolean mayPlace(ItemStack p_39508_) {
                return true;
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });

        addSlot(new Slot(storage, 1) {
            @Override
            public boolean mayPlace(ItemStack p_39508_) {
                return p_39508_.getType() == ItemTypes.LAPIS_LAZULI;
            }
        });

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
                if (!this.moveItemStackTo(itemstack1, 2, 38, true)) {
                    return ItemStack.EMPTY;
                }
            } else if (slotID == 1) {
                if (!this.moveItemStackTo(itemstack1, 2, 38, true)) {
                    return ItemStack.EMPTY;
                }
            } else if (itemstack1.getType() == ItemTypes.LAPIS_LAZULI) {
                if (!this.moveItemStackTo(itemstack1, 1, 2, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                if (this.slots.get(0).hasItem() || !this.slots.get(0).mayPlace(itemstack1)) {
                    return ItemStack.EMPTY;
                }

                ItemStack itemstack2 = itemstack1.copy();
                itemstack2.setAmount(1);
                itemstack1.shrink(1);
                this.slots.get(0).set(itemstack2);
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
