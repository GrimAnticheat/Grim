package ac.grim.legacyac.check.impl.scaffold;

import ac.grim.legacyac.LegacyAntiCheatPlugin;
import ac.grim.legacyac.check.Check;
import ac.grim.legacyac.data.PlayerData;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockPlaceEvent;

/**
 * FarPlace — Placing blocks beyond reach distance.
 * Ported from Grim's FarPlace check.
 *
 * Vanilla reach for block placement is 4.5 blocks in survival mode (5.0 in creative).
 * This check validates that the placed block is within reach distance of the player's
 * eye position.
 */
public final class FarPlaceCheck extends Check {
    public FarPlaceCheck(LegacyAntiCheatPlugin plugin) {
        super(plugin, "FarPlace");
    }

    public void onPlace(BlockPlaceEvent event, PlayerData data) {
        if (!isEnabled() || isExempt(event.getPlayer(), data)) {
            return;
        }

        Player player = event.getPlayer();
        Location eyeLocation = player.getEyeLocation();
        Location blockCenter = event.getBlockPlaced().getLocation().add(0.5, 0.5, 0.5);

        double distance = eyeLocation.distance(blockCenter);

        double maxReach;
        if (player.getGameMode() == org.bukkit.GameMode.CREATIVE) {
            maxReach = plugin.getConfig().getDouble("checks.FarPlace.max-reach-creative", 6.0);
        } else {
            maxReach = plugin.getConfig().getDouble("checks.FarPlace.max-reach", 5.0);
        }

        // Add latency compensation
        double rttMs = data.getLastTransactionRttNanos() / 1_000_000.0;
        double latencyBonus = Math.min(rttMs * 0.002, 0.5);
        maxReach += latencyBonus;

        if (distance > maxReach) {
            double excess = distance - maxReach;
            double buffer = increaseBuffer(data, excess);
            if (buffer > plugin.getConfig().getDouble("checks.FarPlace.buffer", 0.5)) {
                flag(player, data, excess,
                        "distance=" + String.format("%.2f", distance)
                                + " max=" + String.format("%.2f", maxReach));
            }
        }
    }

    public void onPacketPlace(Player player, PlayerData data, PlayerData.QueuedBlockPlaceSnapshot snapshot) {
        if (!isEnabled() || isExempt(player, data)) {
            return;
        }

        Location eyeLocation = new Location(player.getWorld(), snapshot.getOriginX(),
                snapshot.getOriginY() + player.getEyeHeight(), snapshot.getOriginZ(), snapshot.getYaw(),
                snapshot.getPitch());
        Location blockCenter = new Location(player.getWorld(), snapshot.getPlacedX() + 0.5D,
                snapshot.getPlacedY() + 0.5D, snapshot.getPlacedZ() + 0.5D);
        double distance = eyeLocation.distance(blockCenter);

        double maxReach;
        if (player.getGameMode() == org.bukkit.GameMode.CREATIVE) {
            maxReach = plugin.getConfig().getDouble("checks.FarPlace.max-reach-creative", 6.0D);
        } else {
            maxReach = plugin.getConfig().getDouble("checks.FarPlace.max-reach", 5.0D);
        }

        double rttMs = data.getLastTransactionRttNanos() / 1_000_000.0D;
        maxReach += Math.min(rttMs * 0.002D, 0.5D);

        if (distance > maxReach) {
            double excess = distance - maxReach;
            double buffer = increaseBuffer(data, excess);
            if (buffer > plugin.getConfig().getDouble("checks.FarPlace.buffer", 0.5D)) {
                flag(player, data, excess,
                        "distance=" + String.format("%.2f", distance)
                                + " max=" + String.format("%.2f", maxReach));
            }
        }
    }
}
