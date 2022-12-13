package ac.grim.grimac.utils.inventory;

import com.github.retrooper.packetevents.protocol.item.ItemStack;

public class InventoryStorage {
    protected ItemStack[] items;
    int size;

    public InventoryStorage(int size) {
        this.items = new ItemStack[size];
        this.size = size;

        for (int i = 0; i < size; i++) {
            items[i] = ItemStack.EMPTY;
        }
    }

    public int getSize() {
        return size;
    }

    public void setItem(int item, ItemStack stack) {
        items[item] = stack == null ? ItemStack.EMPTY : stack;
    }

    public ItemStack getItem(int index) {
        return items[index];
    }

    public ItemStack removeItem(int slot, int amount) {
        return slot >= 0 && slot < items.length && !items[slot].isEmpty() && amount > 0 ? items[slot].split(amount) : ItemStack.EMPTY;
    }

    public int getMaxStackSize() {
        return 64;
    }
}
