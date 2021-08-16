package ac.grim.grimac.events.bukkit;

import ac.grim.grimac.GrimAC;
import ac.grim.grimac.player.GrimPlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerGameModeChangeEvent;

public class GamemodeChangeEvent implements Listener {
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onGameModeChangeEvent(PlayerGameModeChangeEvent event) {
        // How can getTo be null?
        GrimPlayer player = GrimAC.playerGrimHashMap.get(event.getPlayer());
        if (player != null) {
            player.sendAndFlushTransactionOrPingPong();
            player.latencyUtils.addRealTimeTask(player.lastTransactionSent.get(), () -> player.packetStateData.gameMode = event.getNewGameMode());
        }
    }
}
