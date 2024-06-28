package ac.grim.grimac.utils.teleportation;

import org.bukkit.Location;
import org.bukkit.entity.Player;

public interface TeleportationManager {

    default boolean teleport(Player player, Location location) {
        return player.teleport(location);
    }

}
