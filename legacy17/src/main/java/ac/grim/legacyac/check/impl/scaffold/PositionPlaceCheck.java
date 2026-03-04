package ac.grim.legacyac.check.impl.scaffold;

import ac.grim.legacyac.LegacyAntiCheatPlugin;
import ac.grim.legacyac.check.Check;
import ac.grim.legacyac.data.PlayerData;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.util.Vector;

/**
 * PositionPlace — Block face occluded from player position.
 * Ported from Grim's PositionPlace check.
 *
 * Validates that the face the player claims to place against is actually
 * visible from the player's position. Scaffold hacks often place against
 * hidden/impossible block faces.
 */
public final class PositionPlaceCheck extends Check {
    public PositionPlaceCheck(LegacyAntiCheatPlugin plugin) {
        super(plugin, "PositionPlace");
    }

    public void onPlace(BlockPlaceEvent event, PlayerData data) {
        if (!isEnabled() || isExempt(event.getPlayer(), data)) {
            return;
        }

        Player player = event.getPlayer();
        Block against = event.getBlockAgainst();
        Block placed = event.getBlockPlaced();
        if (against == null || placed == null) {
            return;
        }

        // Determine which face was clicked based on relative position
        int dx = placed.getX() - against.getX();
        int dy = placed.getY() - against.getY();
        int dz = placed.getZ() - against.getZ();

        // Get the center of the face that was clicked
        double faceX = against.getX() + 0.5 + dx * 0.5;
        double faceY = against.getY() + 0.5 + dy * 0.5;
        double faceZ = against.getZ() + 0.5 + dz * 0.5;

        Location eyeLocation = player.getEyeLocation();

        // Check if the player's eye can "see" the face center
        // The face normal must point towards the player (dot product > 0)
        double toPlayerX = eyeLocation.getX() - faceX;
        double toPlayerY = eyeLocation.getY() - faceY;
        double toPlayerZ = eyeLocation.getZ() - faceZ;

        // Face normal is the direction from against block to placed block
        double dot = toPlayerX * dx + toPlayerY * dy + toPlayerZ * dz;

        if (dot < -0.01) {
            // Player is behind the face they claim to click — impossible in vanilla
            double buffer = increaseBuffer(data, 1.0);
            if (buffer > plugin.getConfig().getDouble("checks.PositionPlace.buffer", 2.0)) {
                flag(player, data, 1.0,
                        "behindFace dot=" + String.format("%.2f", dot)
                                + " face=" + dx + "," + dy + "," + dz);
            }
        }
    }
}
