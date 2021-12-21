package ac.grim.grimac.utils.inventory.slot;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.inventory.InventoryStorage;
import com.github.retrooper.packetevents.protocol.item.ItemStack;

import java.util.Optional;

public class Slot {
    public final int inventoryStorageSlot;
    public int slotListIndex;
    InventoryStorage container;

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

    public void set(ItemStack itemstack2) {
        container.setItem(inventoryStorageSlot, itemstack2);
    }

    public int getMaxStackSize() {
        return container.getMaxStackSize();
    }

    public int getMaxStackSize(ItemStack itemstack2) {
        return Math.min(itemstack2.getMaxStackSize(), getMaxStackSize());
    }

    // TODO: Implement for anvil and smithing table
    // TODO: Implement curse of binding support
    public boolean mayPickup() {
        return true;
    }

    public ItemStack safeTake(int p_150648_, int p_150649_, GrimPlayer p_150650_) {
        Optional<ItemStack> optional = this.tryRemove(p_150648_, p_150649_, p_150650_);
        optional.ifPresent((p_150655_) -> {
            this.onTake(p_150650_, p_150655_);
        });
        return optional.orElse(ItemStack.EMPTY);
    }

    public Optional<ItemStack> tryRemove(int p_150642_, int p_150643_, GrimPlayer p_150644_) {
        if (!this.mayPickup(p_150644_)) {
            return Optional.empty();
        } else if (!this.allowModification(p_150644_) && p_150643_ < this.getItem().getAmount()) {
            return Optional.empty();
        } else {
            p_150642_ = Math.min(p_150642_, p_150643_);
            ItemStack itemstack = this.remove(p_150642_);
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
            return stack;
        } else {
            return stack;
        }
    }

    public ItemStack remove(int p_40227_) {
        return this.container.removeItem(this.inventoryStorageSlot, p_40227_);
    }

    public void onTake(GrimPlayer p_150645_, ItemStack p_150646_) {

    }

    // No override
    public boolean allowModification(GrimPlayer p_150652_) {
        return this.mayPickup(p_150652_) && this.mayPlace(this.getItem());
    }

    public boolean mayPickup(GrimPlayer p_40228_) {
        return true;
    }
}
