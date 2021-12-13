package ac.grim.grimac.utils.inventory;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class WrappedStack {
    private ItemStack stack;

    public WrappedStack(ItemStack stack) {
        this.stack = stack;
    }

    public static WrappedStack empty() {
        return new WrappedStack(new ItemStack(Material.AIR));
    }

    public static boolean isSameItemSameTags(WrappedStack p_38901_, WrappedStack item) {
        return p_38901_.isSameItemSameTags(item);
    }

    public static boolean isSame(WrappedStack p_41747_, WrappedStack p_41748_) {
        if (p_41747_ == p_41748_) {
            return true;
        } else {
            return !p_41747_.isEmpty() && !p_41748_.isEmpty() && p_41747_.sameItem(p_41748_);
        }
    }

    public boolean sameItem(WrappedStack p_41657_) {
        return !p_41657_.isEmpty() && this.is(p_41657_.getItem());
    }

    public boolean is(Material p_150931_) {
        return this.getItem() == p_150931_;
    }

    public Material getItem() {
        return stack.getType();
    }

    @NotNull
    public ItemStack getStack() {
        return isEmpty() ? new ItemStack(Material.AIR) : stack;
    }

    public void set(ItemStack stack) {
        this.stack = stack;
    }

    public int getCount() {
        return isEmpty() ? 0 : stack.getAmount();
    }

    public void setCount(int amount) {
        if (stack == null) return;
        stack.setAmount(amount);
    }

    public void shrink(int amount) {
        this.setCount(this.getCount() - amount);
    }

    public void grow(int amount) {
        this.setCount(this.getCount() + amount);
    }

    public WrappedStack split(int toTake) {
        int i = Math.min(toTake, getCount());
        WrappedStack itemstack = this.copy();
        itemstack.setCount(i);
        this.shrink(i);
        return itemstack;
    }

    public WrappedStack copy() {
        return stack == null ? empty() : new WrappedStack(stack.clone());
    }

    public boolean isEmpty() {
        if (stack == null) return true;
        if (stack.getType() == Material.AIR) return true;
        return stack.getAmount() <= 0;
    }

    public int getMaxStackSize() {
        if (stack == null) return 0;
        // NO BUKKIT, AIR HAS A MAX STACK SIZE OF 64!
        if (stack.getType() == Material.AIR) return 64;
        return stack.getMaxStackSize();
    }

    public boolean isSameItemSameTags(WrappedStack p_150731_) {
        return (isEmpty() && p_150731_.isEmpty()) || getStack().isSimilar(p_150731_.getStack());
    }

    public boolean mayPlace(WrappedStack p_40231_) {
        return true;
    }

    // TODO: Bundle support
    public boolean overrideStackedOnOther(WrappedStack other, ClickAction p_150735_) {
        return false;
    }

    public boolean overrideOtherStackedOnMe(WrappedStack other, ClickAction p_150735_, WrappedStack carried) {
        return false;
    }

    // TODO: Implement for anvil and smithing table
    // TODO: Implement curse of binding support
    public boolean mayPickup() {
        return true;
    }

    public boolean isDamaged() {
        return getStack().getDurability() != 0;
    }
}
