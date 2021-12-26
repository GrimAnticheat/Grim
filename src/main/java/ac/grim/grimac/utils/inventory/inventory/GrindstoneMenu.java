package ac.grim.grimac.utils.inventory.inventory;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.inventory.Inventory;
import ac.grim.grimac.utils.inventory.InventoryStorage;
import ac.grim.grimac.utils.inventory.slot.Slot;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;

public class GrindstoneMenu extends AbstractContainerMenu {

    public GrindstoneMenu(GrimPlayer player, Inventory playerInventory) {
        super(player, playerInventory);

        InventoryStorage storage = new InventoryStorage(3);

        addSlot(new Slot(storage, 0) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                // Is damageable, is enchanted book, or is enchanted
                return (stack.isDamageableItem() || stack.getType() == ItemTypes.ENCHANTED_BOOK || !stack.isEnchanted());
            }
        });
        addSlot(new Slot(storage, 1) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                // Is damageable, is enchanted book, or is enchanted
                return (stack.isDamageableItem() || stack.getType() == ItemTypes.ENCHANTED_BOOK || !stack.isEnchanted());
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
