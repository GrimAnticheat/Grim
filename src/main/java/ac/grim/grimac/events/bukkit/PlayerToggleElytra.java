package ac.grim.grimac.events.bukkit;

import ac.grim.grimac.GrimAC;
import ac.grim.grimac.player.GrimPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityToggleGlideEvent;

// This is needed as players could fake elytra flight with packets
// It controls client -> server elytra communication
public class PlayerToggleElytra implements Listener {
    @EventHandler(priority = EventPriority.MONITOR)
    public void onElytraToggleEvent(EntityToggleGlideEvent event) {
        if (!(event.getEntity() instanceof Player)) return;

        GrimPlayer player = GrimAC.playerGrimHashMap.get((Player) event.getEntity());

        if (player == null) return;

        if (player.compensatedElytra.playerToggledElytra && event.isGliding()) {
            player.compensatedElytra.lagCompensatedIsGlidingMap.put(player.packetStateData.packetLastTransactionReceived, true);
        }
    }
}
