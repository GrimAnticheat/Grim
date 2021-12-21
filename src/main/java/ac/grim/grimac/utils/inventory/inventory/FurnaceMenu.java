package ac.grim.grimac.utils.inventory.inventory;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.inventory.Inventory;
import ac.grim.grimac.utils.inventory.InventoryStorage;
import ac.grim.grimac.utils.inventory.slot.FurnaceFuelSlot;
import ac.grim.grimac.utils.inventory.slot.FurnaceResultSlot;
import ac.grim.grimac.utils.inventory.slot.Slot;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;

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

                // TODO: Smelting recipes (Sent to the player but still painful)

                if (canSmelt.get()) {
                    if (!this.moveItemStackTo(itemstack1, 0, 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (itemstack1.getType().getAttributes().contains(ItemTypes.ItemAttribute.FUEL)) {
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
