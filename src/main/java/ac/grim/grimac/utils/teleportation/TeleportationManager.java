package ac.grim.grimac.utils.teleportation;

import org.bukkit.Location;
import org.bukkit.entity.Player;

public interface TeleportationManager {

    boolean teleport(Player player, Location location);

}
