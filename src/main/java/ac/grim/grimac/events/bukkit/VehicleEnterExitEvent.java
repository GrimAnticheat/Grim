package ac.grim.grimac.events.bukkit;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.player.GrimPlayer;
import io.github.retrooper.packetevents.PacketEvents;
import io.github.retrooper.packetevents.packetwrappers.play.out.entityteleport.WrappedPacketOutEntityTeleport;
import io.github.retrooper.packetevents.utils.server.ServerVersion;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vehicle;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.vehicle.VehicleDestroyEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.event.vehicle.VehicleExitEvent;

import java.util.Collections;
import java.util.List;

public class VehicleEnterExitEvent implements Listener {
    @EventHandler(priority = EventPriority.MONITOR)
    public void onVehicleEnter(VehicleEnterEvent event) {
        if (!(event.getEntered() instanceof Player)) return;

        GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer((Player) event.getEntered());
        if (player == null) return;

        player.sendTransaction();
        player.latencyUtils.addRealTimeTask(player.lastTransactionSent.get(), () -> player.inVehicle = true);
    }

    @EventHandler(priority = EventPriority.MONITOR)
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

        player.sendTransaction();
        player.latencyUtils.addRealTimeTask(player.lastTransactionSent.get(), () -> player.inVehicle = false);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onVehicleDestroy(VehicleDestroyEvent event) {
        for (final Entity entity : getPassengers(event.getVehicle())) {
            if (entity instanceof Player) {
                GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer((Player) entity);
                if (player == null) continue;

                player.sendTransaction();
                player.latencyUtils.addRealTimeTask(player.lastTransactionSent.get(), () -> player.inVehicle = false);
            }
        }
    }

    private List<Entity> getPassengers(Vehicle vehicle) {
        if (ServerVersion.getVersion().isNewerThanOrEquals(ServerVersion.v_1_9)) {
            return vehicle.getPassengers();
        } else {
            return Collections.singletonList(vehicle.getPassenger());
        }
    }
}
