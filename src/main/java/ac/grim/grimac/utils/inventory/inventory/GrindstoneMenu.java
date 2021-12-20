package ac.grim.grimac.utils.inventory.inventory;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.inventory.Inventory;
import ac.grim.grimac.utils.inventory.InventoryStorage;
import ac.grim.grimac.utils.inventory.slot.Slot;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import org.bukkit.Material;

public class GrindstoneMenu extends AbstractContainerMenu {

    public GrindstoneMenu(GrimPlayer player, Inventory playerInventory) {
        super(player, playerInventory);

        InventoryStorage storage = new InventoryStorage(3);

        addSlot(new Slot(storage, 0) {
            @Override
            public boolean mayPlace(ItemStack p_39607_) {
                ItemStack stack = p_39607_.getStack();
                // Is damageable, is enchanted book, or is enchanted
                return (stack.getType().getMaxDurability() > 0 && stack.getItemMeta() != null && !stack.getItemMeta().isUnbreakable()
                        || stack.getType() == Material.ENCHANTED_BOOK || !stack.getEnchantments().isEmpty());
            }
        });
        addSlot(new Slot(storage, 1) {
            @Override
            public boolean mayPlace(ItemStack p_39607_) {
                ItemStack stack = p_39607_.getStack();
                // Is damageable, is enchanted book, or is enchanted
                return (stack.getType().getMaxDurability() > 0 && stack.getItemMeta() != null && !stack.getItemMeta().isUnbreakable()
                        || stack.getType() == Material.ENCHANTED_BOOK || !stack.getEnchantments().isEmpty());
            }
        });
        addSlot(new Slot(storage, 2) {
            @Override
            public boolean mayPlace(ItemStack p_39630_) {
                return false;
            }

            @Override
            public void onTake(GrimPlayer p_150574_, ItemStack p_150575_) {
                storage.setItem(0, ItemStack.EMPTY);
                storage.setItem(1, ItemStack.EMPTY);
            }
        });

        addFourRowPlayerInventory();
    }
}
