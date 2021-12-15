package ac.grim.grimac.utils.inventory.inventory;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.inventory.Inventory;
import ac.grim.grimac.utils.inventory.InventoryStorage;
import ac.grim.grimac.utils.inventory.slot.Slot;

public class LecternMenu extends AbstractContainerMenu {
    public LecternMenu(GrimPlayer player, Inventory playerInventory) {
        super(player, playerInventory);

        InventoryStorage storage = new InventoryStorage(1);
        addSlot(new Slot(storage, 0));
    }
}
