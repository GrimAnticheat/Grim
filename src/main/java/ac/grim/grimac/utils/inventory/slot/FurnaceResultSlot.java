package ac.grim.grimac.utils.inventory.slot;

import ac.grim.grimac.utils.inventory.InventoryStorage;
import com.github.retrooper.packetevents.protocol.item.ItemStack;

public class FurnaceResultSlot extends Slot{
    public FurnaceResultSlot(InventoryStorage container, int slot) {
        super(container, slot);
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return false;
    }
}
