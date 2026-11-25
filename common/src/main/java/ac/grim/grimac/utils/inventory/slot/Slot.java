package ac.grim.grimac.utils.inventory.slot;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.inventory.InventoryStorage;
import com.github.retrooper.packetevents.protocol.item.ItemStack;

import java.util.Optional;

public class Slot {
    public final int inventoryStorageSlot;
    public int slotListIndex;
    private final InventoryStorage container;

    public Slot(InventoryStorage container, int slot) {
        this.container = container;
        this.inventoryStorageSlot = slot;
    }

    public ItemStack getItem() {
        return container.getItem(inventoryStorageSlot);
    }

    public boolean hasItem() {
        return !this.getItem().isEmpty();
    }

    public boolean mayPlace(ItemStack itemstack) {
        return true;
    }

    public void set(ItemStack itemStack) {
        container.setItem(inventoryStorageSlot, itemStack);
    }

    public int getMaxStackSize() {
        return container.getMaxStackSize();
    }

    public int getMaxStackSize(ItemStack itemStack) {
        return Math.min(itemStack.getMaxStackSize(), getMaxStackSize());
    }

    public ItemStack safeTake(int amount, int maxAmount, GrimPlayer player) {
        Optional<ItemStack> optional = this.tryRemove(amount, maxAmount, player);
        optional.ifPresent((p_150655_) -> this.onTake(player, p_150655_));
        return optional.orElse(ItemStack.EMPTY);
    }

    public Optional<ItemStack> tryRemove(int amount, int maxAmount, GrimPlayer player) {
        if (!this.mayPickup(player)) {
            return Optional.empty();
        } else if (!this.allowModification(player) && maxAmount < this.getItem().getAmount()) {
            return Optional.empty();
        } else {
            amount = Math.min(amount, maxAmount);
            ItemStack itemstack = this.remove(amount);
            if (itemstack.isEmpty()) {
                return Optional.empty();
            } else {
                if (this.getItem().isEmpty()) {
                    this.set(ItemStack.EMPTY);
                }

                return Optional.of(itemstack);
            }
        }
    }

    public ItemStack safeInsert(ItemStack stack, int amount) {
        if (!stack.isEmpty() && this.mayPlace(stack)) {
            ItemStack itemstack = this.getItem();
            int i = Math.min(Math.min(amount, stack.getAmount()), this.getMaxStackSize(stack) - itemstack.getAmount());
            if (itemstack.isEmpty()) {
                this.set(stack.split(i));
            } else if (ItemStack.isSameItemSameTags(itemstack, stack)) {
                stack.shrink(i);
                itemstack.grow(i);
                this.set(itemstack);
            }
        }
        return stack;
    }

    public ItemStack remove(int amount) {
        return this.container.removeItem(this.inventoryStorageSlot, amount);
    }

    public void onTake(GrimPlayer player, ItemStack itemStack) {}

    // No override
    public boolean allowModification(GrimPlayer player) {
        return this.mayPickup(player) && this.mayPlace(this.getItem());
    }

    // TODO: Implement for anvil and smithing table
    public boolean mayPickup(GrimPlayer player) {
        return true;
    }
}
