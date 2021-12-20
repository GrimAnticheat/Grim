package ac.grim.grimac.utils.inventory.slot;

import ac.grim.grimac.utils.inventory.InventoryStorage;
import ac.grim.grimac.utils.inventory.ItemStack;
import org.bukkit.Material;

public class FurnaceFuelSlot extends Slot{

    public FurnaceFuelSlot(InventoryStorage container, int slot) {
        super(container, slot);
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return stack.getStack().getType().isFuel() || stack.getStack().getType() == Material.BUCKET;
    }

    @Override
    public int getMaxStackSize(ItemStack stack) {
        if (stack.getStack().getType() == Material.BUCKET) {
            return 1;
        }
        return super.getMaxStackSize(stack);
    }
}
