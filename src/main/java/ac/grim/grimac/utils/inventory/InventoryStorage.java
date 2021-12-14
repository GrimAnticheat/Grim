package ac.grim.grimac.utils.inventory;

public class InventoryStorage {
    WrappedStack[] items;

    public InventoryStorage(int size) {
        this.items = new WrappedStack[size];

        for (int i = 0; i < size; i++) {
            items[i] = WrappedStack.empty();
        }
    }

    public void setItem(int item, WrappedStack stack) {
        items[item] = stack;
    }

    public WrappedStack getItem(int index) {
        return items[index];
    }

    public WrappedStack removeItem(int slot, int amount) {
        return slot >= 0 && slot < items.length && !items[slot].isEmpty() && amount > 0 ? items[slot].split(amount) : WrappedStack.empty();
    }

    public int getMaxStackSize() {
        return 64;
    }
}
