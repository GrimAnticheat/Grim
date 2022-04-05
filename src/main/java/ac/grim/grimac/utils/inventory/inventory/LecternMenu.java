package ac.grim.grimac.utils.inventory.inventory;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.inventory.Inventory;
import ac.grim.grimac.utils.inventory.InventoryStorage;
import ac.grim.grimac.utils.inventory.slot.Slot;
import com.github.retrooper.packetevents.protocol.item.ItemStack;

public class LecternMenu extends AbstractContainerMenu {
    public LecternMenu(GrimPlayer player, Inventory playerInventory) {
        super(player, playerInventory);

        InventoryStorage storage = new InventoryStorage(1);
        addSlot(new Slot(storage, 0));
    }

    @Override
    public ItemStack quickMoveStack(int slotID) {
        return ItemStack.EMPTY; // patch crash exploit, fun fact: this crash works in vanilla too
    }
}
