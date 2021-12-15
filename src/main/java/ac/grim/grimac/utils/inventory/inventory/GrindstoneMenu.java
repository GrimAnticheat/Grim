package ac.grim.grimac.utils.inventory.inventory;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.inventory.Inventory;
import ac.grim.grimac.utils.inventory.InventoryStorage;
import ac.grim.grimac.utils.inventory.WrappedStack;
import ac.grim.grimac.utils.inventory.slot.Slot;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class GrindstoneMenu extends AbstractContainerMenu {

    public GrindstoneMenu(GrimPlayer player, Inventory playerInventory) {
        super(player, playerInventory);

        InventoryStorage storage = new InventoryStorage(3);

        addSlot(new Slot(storage, 0) {
            @Override
            public boolean mayPlace(WrappedStack p_39607_) {
                ItemStack stack = p_39607_.getStack();
                // Is damageable, is enchanted book, or is enchanted
                return (stack.getType().getMaxDurability() > 0 && stack.getItemMeta() != null && !stack.getItemMeta().isUnbreakable()
                        || stack.getType() == Material.ENCHANTED_BOOK || !stack.getEnchantments().isEmpty());
            }
        });
        addSlot(new Slot(storage, 1) {
            @Override
            public boolean mayPlace(WrappedStack p_39607_) {
                ItemStack stack = p_39607_.getStack();
                // Is damageable, is enchanted book, or is enchanted
                return (stack.getType().getMaxDurability() > 0 && stack.getItemMeta() != null && !stack.getItemMeta().isUnbreakable()
                        || stack.getType() == Material.ENCHANTED_BOOK || !stack.getEnchantments().isEmpty());
            }
        });
        addSlot(new Slot(storage, 2) {
            @Override
            public boolean mayPlace(WrappedStack p_39630_) {
                return false;
            }

            @Override
            public void onTake(GrimPlayer p_150574_, WrappedStack p_150575_) {
                storage.setItem(0, WrappedStack.empty());
                storage.setItem(1, WrappedStack.empty());
            }
        });

        addFourRowPlayerInventory();
    }
}
