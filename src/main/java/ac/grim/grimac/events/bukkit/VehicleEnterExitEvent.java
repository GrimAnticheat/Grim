package ac.grim.grimac.events.bukkit;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.data.SetBackData;
import io.github.retrooper.packetevents.PacketEvents;
import io.github.retrooper.packetevents.packetwrappers.play.out.entityteleport.WrappedPacketOutEntityTeleport;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.event.vehicle.VehicleExitEvent;

public class VehicleEnterExitEvent implements Listener {
    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerVehicleEnterEvent(VehicleEnterEvent event) {
        GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer((Player) event.getEntered());
        if (player == null) return;

        SetBackData data = player.getSetbackTeleportUtil().getRequiredSetBack();

        // Pending setback, don't let the player mount the vehicle
        // Don't block if this is another plugin teleport and not a setback
        if (data != null && !data.isComplete() && player.getSetbackTeleportUtil().lastOtherPluginTeleport != data.getTrans()) {
            event.setCancelled(true);
            return;
        }

        player.latencyUtils.addAnticheatSyncTask(player.lastTransactionSent.get(), () -> player.vehicle = event.getVehicle().getEntityId());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerExitVehicleEvent(VehicleExitEvent event) {
        if (!(event.getExited() instanceof Player)) return;

        GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer((Player) event.getExited());
        if (player == null) return;

        // Update the position of this entity to stop glitchy behavior
        // We do this by sending the player an entity teleport packet for this boat the next tick
        // (If we send it this tick, the player will ignore it!)
        // This is required due to ViaVersion incorrectly handling version differences
        Bukkit.getScheduler().runTaskLater(GrimAPI.INSTANCE.getPlugin(),
                () -> PacketEvents.get().getPlayerUtils().sendPacket(player.bukkitPlayer,
                        new WrappedPacketOutEntityTeleport(event.getVehicle().getEntityId(), event.getVehicle().getLocation(),
                                event.getVehicle().isOnGround())), 1);
        event.getVehicle().teleport(event.getVehicle().getLocation());

        player.latencyUtils.addAnticheatSyncTask(player.lastTransactionSent.get(), () -> player.vehicle = null);
    }
}
