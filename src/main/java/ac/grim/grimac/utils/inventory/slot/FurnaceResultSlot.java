package ac.grim.grimac.utils.inventory.slot;

import ac.grim.grimac.utils.inventory.InventoryStorage;
import ac.grim.grimac.utils.inventory.WrappedStack;

public class FurnaceResultSlot extends Slot{
    public FurnaceResultSlot(InventoryStorage container, int slot) {
        super(container, slot);
    }

    @Override
    public boolean mayPlace(WrappedStack stack) {
        return false;
    }
}
