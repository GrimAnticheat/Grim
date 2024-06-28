package dev.tieseler.teleportation;

import ac.grim.grimac.utils.teleportation.TeleportationManager;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class FoliaTeleporter implements TeleportationManager {

    @Override
    public boolean teleport(Player player, Location location) {
        return player.teleportAsync(location).join();
    }

}
