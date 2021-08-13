package ac.grim.grimac.events.bukkit;

import ac.grim.grimac.GrimAC;
import ac.grim.grimac.player.GrimPlayer;
import io.github.retrooper.packetevents.utils.player.ClientVersion;
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

        // 1.15+ clients have client sided elytra start
        // Use this as a backup to inventory desync
        if (event.isGliding() && player.getClientVersion().isNewerThanOrEquals(ClientVersion.v_1_15)) {
            player.compensatedElytra.tryAddStatus(player.compensatedElytra.lastToggleElytra, true);
        }

        // Support the player ending flight themselves by beginning to fly
        if (((Player) event.getEntity()).isFlying() && !event.isGliding()) {
            player.compensatedElytra.tryAddStatus(player.compensatedElytra.lastToggleFly, false);
        }
    }
}
