package ac.grim.grimac.events.bukkit;

import ac.grim.grimac.GrimAC;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.data.ItemUseEnum;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemConsumeEvent;

public class PlayerConsumeItem implements Listener {

    // Prevents slowed by item desync when player lets go of right click the same tick as finishing consuming an item
    @EventHandler(ignoreCancelled = true)
    public void onPlayerConsumeEvent(PlayerItemConsumeEvent event) {
        GrimPlayer player = GrimAC.playerGrimHashMap.get(event.getPlayer());
        if (player == null) return;

        player.packetStateData.slowedByUsingItem = ItemUseEnum.MAYBE;
    }
}
