package ac.grim.grimac.utils.inventory.slot;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.inventory.InventoryStorage;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import org.bukkit.Bukkit;

public class ResultSlot extends Slot {

    public ResultSlot(InventoryStorage container, int slot) {
        super(container, slot);
    }

    @Override
    public boolean mayPlace(ItemStack p_40178_) {
        return false;
    }

    @Override
    public void onTake(GrimPlayer p_150638_, ItemStack p_150639_) {
        // Resync the player's inventory
        if (p_150638_.bukkitPlayer == null) return;
        Bukkit.getServer().getScheduler().runTask(GrimAPI.INSTANCE.getPlugin(), p_150638_.bukkitPlayer::updateInventory);
    }
}
