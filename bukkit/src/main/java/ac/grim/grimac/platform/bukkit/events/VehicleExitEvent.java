package ac.grim.grimac.platform.bukkit.events;

import ac.grim.grimac.platform.bukkit.GrimACBukkitLoaderPlugin;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Horse;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vehicle;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

public class VehicleExitEvent implements Listener {

    public static boolean canBeRegistered() {
        Class<Player> clazz = Player.class;

        try {
            clazz.getMethod("hideEntity", Plugin.class, Entity.class);
            clazz.getMethod("showEntity", Plugin.class, Entity.class);
            return true;
        } catch (NoSuchMethodException ignored) {
            return false;
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void handle(org.bukkit.event.vehicle.VehicleExitEvent event) {
        LivingEntity exited = event.getExited();
        if (!(exited instanceof Player player)) {
            return;
        }

        Vehicle vehicle = event.getVehicle();
        if (!(vehicle instanceof Horse horse)) {
            return;
        }

        player.hideEntity(GrimACBukkitLoaderPlugin.LOADER, horse);
        player.showEntity(GrimACBukkitLoaderPlugin.LOADER, horse);
    }

}
