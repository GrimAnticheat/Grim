package ac.grim.grimac.utils.inventory;

import com.github.retrooper.packetevents.protocol.item.ItemStack;
import lombok.Getter;

public class InventoryStorage {
    private final ItemStack[] items;
    @Getter
    private final int size;

    public InventoryStorage(int size) {
        this.items = new ItemStack[size];
        this.size = size;

        for (int i = 0; i < size; i++) {
            items[i] = ItemStack.EMPTY;
        }
    }

    public void setItem(int item, ItemStack stack) {
        items[item] = stack == null ? ItemStack.EMPTY : stack;
    }

    public ItemStack getItem(int index) {
        return items[index];
    }

    public ItemStack removeItem(int slot, int amount) {
        return slot >= 0 && slot < size && !items[slot].isEmpty() && amount > 0 ? items[slot].split(amount) : ItemStack.EMPTY;
    }

    public int getMaxStackSize() {
        return 64;
    }
}
