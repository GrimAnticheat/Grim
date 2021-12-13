package ac.grim.grimac.utils.inventory.slot;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.inventory.WrappedStack;
import ac.grim.grimac.utils.inventory.inventory.AbstractContainerMenu;

import java.util.Optional;

public class Slot {
    public int index;
    AbstractContainerMenu container;

    public Slot(AbstractContainerMenu container, int slot) {
        this.container = container;
        this.index = slot;
    }

    public WrappedStack getItem() {
        return container.getItem(index);
    }

    public boolean hasItem() {
        return !this.getItem().isEmpty();
    }

    public boolean mayPlace(WrappedStack itemstack) {
        return true;
    }

    public void set(WrappedStack itemstack2) {
        container.setItem(index, itemstack2);
    }

    public int getMaxStackSize() {
        return container.getMaxStackSize();
    }

    public int getMaxStackSize(WrappedStack itemstack2) {
        return Math.min(itemstack2.getMaxStackSize(), getMaxStackSize());
    }

    public boolean mayPickup() {
        return true;
    }

    public WrappedStack safeTake(int p_150648_, int p_150649_, GrimPlayer p_150650_) {
        Optional<WrappedStack> optional = this.tryRemove(p_150648_, p_150649_, p_150650_);
        optional.ifPresent((p_150655_) -> {
            this.onTake(p_150650_, p_150655_);
        });
        return optional.orElse(WrappedStack.empty());
    }

    public Optional<WrappedStack> tryRemove(int p_150642_, int p_150643_, GrimPlayer p_150644_) {
        if (!this.mayPickup(p_150644_)) {
            return Optional.empty();
        } else if (!this.allowModification(p_150644_) && p_150643_ < this.getItem().getCount()) {
            return Optional.empty();
        } else {
            p_150642_ = Math.min(p_150642_, p_150643_);
            WrappedStack itemstack = this.remove(p_150642_);
            if (itemstack.isEmpty()) {
                return Optional.empty();
            } else {
                if (this.getItem().isEmpty()) {
                    this.set(WrappedStack.empty());
                }

                return Optional.of(itemstack);
            }
        }
    }

    public WrappedStack safeInsert(WrappedStack stack, int amount) {
        if (!stack.isEmpty() && this.mayPlace(stack)) {
            WrappedStack itemstack = this.getItem();
            int i = Math.min(Math.min(amount, stack.getCount()), this.getMaxStackSize(stack) - itemstack.getCount());
            if (itemstack.isEmpty()) {
                this.set(stack.split(i));
            } else if (WrappedStack.isSameItemSameTags(itemstack, stack)) {
                stack.shrink(i);
                itemstack.grow(i);
                this.set(itemstack);
            }
            return stack;
        } else {
            return stack;
        }
    }

    public WrappedStack remove(int p_40227_) {
        return this.container.removeItem(this.index, p_40227_);
    }

    public void onTake(GrimPlayer p_150645_, WrappedStack p_150646_) {

    }

    // No override
    public boolean allowModification(GrimPlayer p_150652_) {
        return this.mayPickup(p_150652_) && this.mayPlace(this.getItem());
    }

    public boolean mayPickup(GrimPlayer p_40228_) {
        return true;
    }
}
