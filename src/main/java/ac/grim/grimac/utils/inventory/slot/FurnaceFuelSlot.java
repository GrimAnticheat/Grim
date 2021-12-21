package ac.grim.grimac.utils.inventory.slot;

import ac.grim.grimac.utils.inventory.InventoryStorage;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;

public class FurnaceFuelSlot extends Slot {

    public FurnaceFuelSlot(InventoryStorage container, int slot) {
        super(container, slot);
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return stack.getType().getAttributes().contains(ItemTypes.ItemAttribute.FUEL) || stack.getType() == ItemTypes.BUCKET;
    }

    @Override
    public int getMaxStackSize(ItemStack stack) {
        if (stack.getType() == ItemTypes.BUCKET) {
            return 1;
        }
        return super.getMaxStackSize(stack);
    }
}
