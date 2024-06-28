package ac.grim.grimac.utils.teleportation;

import org.bukkit.Location;
import org.bukkit.entity.Player;

public class DefaultTeleporter implements TeleportationManager {

    @Override
    public boolean teleport(Player player, Location location) {
        return player.teleport(location);
    }
}
