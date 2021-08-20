package ac.grim.grimac.events.bukkit;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.player.GrimPlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerGameModeChangeEvent;

public class GamemodeChangeEvent implements Listener {
    @EventHandler(priority = EventPriority.MONITOR)
    public void onGameModeChangeEvent(PlayerGameModeChangeEvent event) {
        GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getPlayer());
        if (player != null) {
            player.sendAndFlushTransactionOrPingPong();
            player.latencyUtils.addRealTimeTask(player.lastTransactionSent.get(), () -> player.packetStateData.gameMode = event.getNewGameMode());
        }
    }
}
