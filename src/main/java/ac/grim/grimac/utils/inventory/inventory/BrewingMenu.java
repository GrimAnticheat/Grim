package ac.grim.grimac.utils.inventory.inventory;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.inventory.BrewingHelper;
import ac.grim.grimac.utils.inventory.Inventory;
import ac.grim.grimac.utils.inventory.InventoryStorage;
import ac.grim.grimac.utils.inventory.slot.Slot;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;

public class BrewingMenu extends AbstractContainerMenu {
    public BrewingMenu(GrimPlayer player, Inventory playerInventory) {
        super(player, playerInventory);

        InventoryStorage containerStorage = new InventoryStorage(4);

        addSlot(new PotionSlot(containerStorage, 0));
        addSlot(new PotionSlot(containerStorage, 1));
        addSlot(new PotionSlot(containerStorage, 2));
        addSlot(new IngredientsSlot(containerStorage, 3));

        // TODO: Pre-1.9 clients don't have this slot (ViaVersion will translate this)
        addSlot(new FuelSlot(containerStorage, 0));

        addFourRowPlayerInventory();
    }

    @Override
    public ItemStack quickMoveStack(int slotID) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(slotID);
        if (slot != null && slot.hasItem()) {
            ItemStack itemstack1 = slot.getItem();
            itemstack = itemstack1.copy();
            if ((slotID < 0 || slotID > 2) && slotID != 3 && slotID != 4) {
                if (FuelSlot.mayPlaceItem(itemstack)) {
                    if (this.moveItemStackTo(itemstack1, 4, 5, false) || IngredientsSlot.mayPlaceItem(itemstack1) && !this.moveItemStackTo(itemstack1, 3, 4, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (IngredientsSlot.mayPlaceItem(itemstack1)) {
                    if (!this.moveItemStackTo(itemstack1, 3, 4, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (PotionSlot.mayPlaceItem(itemstack) && itemstack.getAmount() == 1) {
                    if (!this.moveItemStackTo(itemstack1, 0, 3, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (slotID >= 5 && slotID < 32) {
                    if (!this.moveItemStackTo(itemstack1, 32, 41, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (slotID >= 32 && slotID < 41) {
                    if (!this.moveItemStackTo(itemstack1, 5, 32, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (!this.moveItemStackTo(itemstack1, 5, 41, false)) {
                    return ItemStack.EMPTY;
                }
            } else {
                if (!this.moveItemStackTo(itemstack1, 5, 41, true)) {
                    return ItemStack.EMPTY;
                }
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

    static class FuelSlot extends Slot {
        public FuelSlot(InventoryStorage container, int slot) {
            super(container, slot);
        }

        public static boolean mayPlaceItem(ItemStack p_39113_) {
            return p_39113_.getType() == ItemTypes.BLAZE_POWDER;
        }

        public boolean mayPlace(ItemStack p_39111_) {
            return mayPlaceItem(p_39111_);
        }

        public int getMaxStackSize() {
            return 64;
        }
    }

    static class IngredientsSlot extends Slot {
        public IngredientsSlot(InventoryStorage container, int slot) {
            super(container, slot);
        }

        public static boolean mayPlaceItem(ItemStack stack) {
            return BrewingHelper.isBaseModifier(stack.getType()) || BrewingHelper.isEffectIngredient(stack.getType());
        }

        public boolean mayPlace(ItemStack p_39121_) {
            return mayPlaceItem(p_39121_);
        }

        public int getMaxStackSize() {
            return 64;
        }
    }

    static class PotionSlot extends Slot {
        public PotionSlot(InventoryStorage container, int slot) {
            super(container, slot);
        }

        public static boolean mayPlaceItem(ItemStack p_39134_) {
            return p_39134_.getType().getName().getKey().endsWith("POTION") || p_39134_.getType() == ItemTypes.GLASS_BOTTLE;
        }

        public int getMaxStackSize() {
            return 1;
        }

        public boolean mayPlace(ItemStack p_39132_) {
            return mayPlaceItem(p_39132_);
        }

        public void onTake(GrimPlayer player, ItemStack p_150500_) {
            // Useless server sided achievement things
            super.onTake(player, p_150500_);
        }
    }
}
