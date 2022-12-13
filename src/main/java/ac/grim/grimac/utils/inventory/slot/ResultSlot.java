package ac.grim.grimac.utils.inventory.slot;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.inventory.InventoryStorage;
import com.github.retrooper.packetevents.protocol.item.ItemStack;

public class ResultSlot extends Slot {

    public ResultSlot(InventoryStorage container, int slot) {
        super(container, slot);
    }

    @Override
    public boolean mayPlace(ItemStack p_40178_) {
        return false;
    }

    @Override
    public void onTake(GrimPlayer player, ItemStack p_150639_) {
        // Resync the player's inventory
    }
}
