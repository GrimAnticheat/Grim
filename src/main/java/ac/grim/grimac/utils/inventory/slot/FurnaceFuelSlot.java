package ac.grim.grimac.utils.inventory.slot;

import ac.grim.grimac.utils.inventory.InventoryStorage;
import ac.grim.grimac.utils.inventory.WrappedStack;
import org.bukkit.Material;

public class FurnaceFuelSlot extends Slot{

    public FurnaceFuelSlot(InventoryStorage container, int slot) {
        super(container, slot);
    }

    @Override
    public boolean mayPlace(WrappedStack stack) {
        return stack.getStack().getType().isFuel() || stack.getStack().getType() == Material.BUCKET;
    }

    @Override
    public int getMaxStackSize(WrappedStack stack) {
        if (stack.getStack().getType() == Material.BUCKET) {
            return 1;
        }
        return super.getMaxStackSize(stack);
    }
}
