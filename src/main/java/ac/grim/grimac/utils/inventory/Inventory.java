package ac.grim.grimac.utils.inventory;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.inventory.inventory.AbstractContainerMenu;
import ac.grim.grimac.utils.inventory.slot.EquipmentSlot;
import ac.grim.grimac.utils.inventory.slot.ResultSlot;
import ac.grim.grimac.utils.inventory.slot.Slot;
import org.bukkit.GameMode;
import org.bukkit.inventory.ItemStack;

public class Inventory extends AbstractContainerMenu {
    public static final int SLOT_OFFHAND = 45;
    public static final int HOTBAR_OFFSET = 36;
    private static final int SLOT_HELMET = 5;
    private static final int SLOT_CHESTPLATE = 6;
    private static final int SLOT_LEGGINGS = 7;
    private static final int SLOT_BOOTS = 8;
    private static final int TOTAL_SIZE = 46;
    private static final int ITEMS_START = 9;
    private static final int ITEMS_END = 45;
    public int selected = 0;
    WrappedStack[] playerInventory;
    WrappedStack carriedItem;

    public Inventory(GrimPlayer player, WrappedStack[] playerInventory, WrappedStack carriedItem) {
        this.playerInventory = playerInventory;
        this.carriedItem = carriedItem;

        super.setPlayer(player);
        super.setPlayerInventory(this);

        for (int i = 0; i < playerInventory.length; i++) {
            playerInventory[i] = WrappedStack.empty();
        }

        // Result slot
        addSlot(new ResultSlot(this, 0));
        // Crafting slots
        for (int i = 0; i < 4; i++) {
            addSlot(new Slot(this, i));
        }
        for (int i = 0; i < 4; i++) {
            addSlot(new EquipmentSlot(EquipmentType.byArmorID(i), this, i + 4));
        }
        // Inventory slots
        for (int i = 0; i < 9 * 4; i++) {
            addSlot(new Slot(this, i + 9));
        }
        // Offhand
        addSlot(new Slot(this, 45));
    }

    public WrappedStack getHeldItem() {
        return playerInventory[selected + HOTBAR_OFFSET];
    }

    public void setHeldItem(ItemStack item) {
        playerInventory[selected + HOTBAR_OFFSET] = new WrappedStack(item);
    }

    public WrappedStack getOffhandItem() {
        return playerInventory[SLOT_OFFHAND];
    }

    public WrappedStack getCarriedItem() {
        return carriedItem;
    }

    public void setCarriedItem(WrappedStack carriedItem) {
        this.carriedItem = carriedItem;
    }

    @Override
    public WrappedStack getItem(int slot) {
        if (slot >= 0 && slot < TOTAL_SIZE)
            return playerInventory[slot];

        return WrappedStack.empty();
    }

    @Override
    public void setItem(int slot, WrappedStack item) {
        if (slot >= 0 && slot < TOTAL_SIZE)
            playerInventory[slot] = item;
    }

    public boolean add(WrappedStack p_36055_) {
        return this.add(-1, p_36055_);
    }

    public int getFreeSlot() {
        for (int i = 0; i < playerInventory.length; ++i) {
            if (getItem(i).isEmpty()) {
                return i;
            }
        }

        return -1;
    }

    public int getSlotWithRemainingSpace(WrappedStack toAdd) {
        if (this.hasRemainingSpaceForItem(getHeldItem(), toAdd)) {
            return this.selected;
        } else if (this.hasRemainingSpaceForItem(getOffhandItem(), toAdd)) {
            return 40;
        } else {
            for (int i = ITEMS_START; i <= ITEMS_END; ++i) {
                if (this.hasRemainingSpaceForItem(getItem(i), toAdd)) {
                    return i;
                }
            }

            return -1;
        }
    }

    private boolean hasRemainingSpaceForItem(WrappedStack one, WrappedStack two) {
        return !one.isEmpty() && WrappedStack.isSameItemSameTags(one, two) && one.getCount() < one.getMaxStackSize() && one.getCount() < this.getMaxStackSize();
    }

    private int addResource(WrappedStack resource) {
        int i = this.getSlotWithRemainingSpace(resource);
        if (i == -1) {
            i = this.getFreeSlot();
        }

        return i == -1 ? resource.getCount() : this.addResource(i, resource);
    }

    private int addResource(int slot, WrappedStack stack) {
        int i = stack.getCount();
        WrappedStack itemstack = getItem(slot);

        if (itemstack.isEmpty()) {
            itemstack = stack.copy();
            itemstack.setCount(0);
            setItem(slot, itemstack);
        }

        int j = i;
        if (i > itemstack.getMaxStackSize() - itemstack.getCount()) {
            j = itemstack.getMaxStackSize() - itemstack.getCount();
        }

        if (j > this.getMaxStackSize() - itemstack.getCount()) {
            j = this.getMaxStackSize() - itemstack.getCount();
        }

        if (j == 0) {
            return i;
        } else {
            i = i - j;
            itemstack.grow(j);
            return i;
        }
    }

    public boolean add(int p_36041_, WrappedStack p_36042_) {
        if (p_36042_.isEmpty()) {
            return false;
        } else {
            if (p_36042_.isDamaged()) {
                if (p_36041_ == -1) {
                    p_36041_ = this.getFreeSlot();
                }

                if (p_36041_ >= 0) {
                    setItem(p_36041_, p_36042_.copy().getStack());
                    p_36042_.setCount(0);
                    return true;
                } else if (player.gamemode == GameMode.CREATIVE) {
                    p_36042_.setCount(0);
                    return true;
                } else {
                    return false;
                }
            } else {
                int i;
                do {
                    i = p_36042_.getCount();
                    if (p_36041_ == -1) {
                        p_36042_.setCount(this.addResource(p_36042_));
                    } else {
                        p_36042_.setCount(this.addResource(p_36041_, p_36042_));
                    }
                } while (!p_36042_.isEmpty() && p_36042_.getCount() < i);

                if (p_36042_.getCount() == i && player.gamemode == GameMode.CREATIVE) {
                    p_36042_.setCount(0);
                    return true;
                } else {
                    return p_36042_.getCount() < i;
                }
            }
        }
    }

    @Override
    public WrappedStack quickMoveStack(int slotID) {
        WrappedStack original = WrappedStack.empty();
        Slot slot = getSlots().get(slotID);

        if (slot != null && slot.hasItem()) {
            WrappedStack toMove = slot.getItem();
            original = toMove.copy();
            EquipmentType equipmentslot = EquipmentType.getEquipmentSlotForItem(original);
            if (slotID == 0) {
                if (!this.moveItemStackTo(toMove, 9, 45, true)) {
                    return WrappedStack.empty();
                }
            } else if (slotID >= 1 && slotID < 5) {
                if (!this.moveItemStackTo(toMove, 9, 45, false)) {
                    return WrappedStack.empty();
                }
            } else if (slotID >= 5 && slotID < 9) {
                if (!this.moveItemStackTo(toMove, 9, 45, false)) {
                    return WrappedStack.empty();
                }
            } else if (equipmentslot.isArmor() && !getSlots().get(8 - equipmentslot.getIndex()).hasItem()) {
                int i = 8 - equipmentslot.getIndex();
                if (!this.moveItemStackTo(toMove, i, i + 1, false)) {
                    return WrappedStack.empty();
                }
            } else if (equipmentslot == EquipmentType.OFFHAND && !getSlots().get(45).hasItem()) {
                if (!this.moveItemStackTo(toMove, 45, 46, false)) {
                    return WrappedStack.empty();
                }
            } else if (slotID >= 9 && slotID < 36) {
                if (!this.moveItemStackTo(toMove, 36, 45, false)) {
                    return WrappedStack.empty();
                }
            } else if (slotID >= 36 && slotID < 45) {
                if (!this.moveItemStackTo(toMove, 9, 36, false)) {
                    return WrappedStack.empty();
                }
            } else if (!this.moveItemStackTo(toMove, 9, 45, false)) {
                return WrappedStack.empty();
            }

            if (toMove.isEmpty()) {
                slot.set(WrappedStack.empty());
            }

            if (toMove.getCount() == original.getCount()) {
                return WrappedStack.empty();
            }
        }

        return original;
    }

    @Override
    public WrappedStack removeItem(int index, int amount) {
        return removeItem(playerInventory, index, amount);
    }
}
