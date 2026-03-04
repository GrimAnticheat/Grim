package ac.grim.legacyac.check.impl;

import ac.grim.legacyac.LegacyAntiCheatPlugin;
import ac.grim.legacyac.check.Check;
import ac.grim.legacyac.data.PlayerData;
import ac.grim.legacyac.network.frame.MovementFrame;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

/**
 * GroundSpoof — Detects when the client claims onGround=true/false
 * but the server determines the opposite based on block collisions.
 * This catches NoFall hacks (claiming onGround=true while in air to avoid fall damage)
 * and ground-spoof-based speed exploits (claiming onGround=false while actually on ground
 * to gain air acceleration properties).
 *
 * Ported from Grim's GroundSpoof + NoFall checks.
 */
public final class GroundSpoofCheck extends Check {

    private int consecutiveSpoofTicks;

    public GroundSpoofCheck(LegacyAntiCheatPlugin plugin) {
        super(plugin, "GroundSpoof");
    }

    public void onMovementFrame(Player player, MovementFrame frame, Location to, PlayerData data) {
        if (!isEnabled() || isExempt(player, data)) {
            consecutiveSpoofTicks = 0;
            return;
        }

        // Only check when we have a position packet
        if (!frame.hasPosition()) {
            return;
        }

        // Skip for players in vehicles, flying, or in creative mode
        if (player.isInsideVehicle() || player.isFlying() || player.getAllowFlight()) {
            consecutiveSpoofTicks = 0;
            return;
        }

        boolean clientOnGround = frame.isOnGround();
        boolean serverOnGround = isActuallyOnGround(to, player);

        // Client claims on ground but is not — classic NoFall
        if (clientOnGround && !serverOnGround) {
            double deltaY = data.getLastDeltaY();
            int airTicks = data.getAirTicks();

            // Only flag if clearly in the air (not edge cases like stairs/slabs)
            if (airTicks > 3 && deltaY < -0.08) {
                consecutiveSpoofTicks++;
                if (consecutiveSpoofTicks >= plugin.getConfig().getInt("checks.GroundSpoof.min-ticks", 3)) {
                    double buffer = increaseBuffer(data, 1.0);
                    if (buffer > plugin.getConfig().getDouble("checks.GroundSpoof.buffer", 2.0)) {
                        flag(player, data, 1.0,
                                "claimsGround=true serverGround=false airTicks=" + airTicks
                                        + " deltaY=" + String.format("%.3f", deltaY));
                    }
                }
                return;
            }
        }

        // Client claims not on ground but actually is — less common but used for speed exploits
        if (!clientOnGround && serverOnGround) {
            double deltaY = data.getLastDeltaY();
            int groundTicks = data.getGroundTicks();

            // Only flag if solidly on ground
            if (groundTicks > 5 && Math.abs(deltaY) < 0.01) {
                consecutiveSpoofTicks++;
                if (consecutiveSpoofTicks >= plugin.getConfig().getInt("checks.GroundSpoof.min-ticks", 3)) {
                    double buffer = increaseBuffer(data, 0.5);
                    if (buffer > plugin.getConfig().getDouble("checks.GroundSpoof.buffer", 2.0)) {
                        flag(player, data, 0.5,
                                "claimsGround=false serverGround=true groundTicks=" + groundTicks);
                    }
                }
                return;
            }
        }

        // No spoof this tick
        if (consecutiveSpoofTicks > 0) {
            consecutiveSpoofTicks--;
        }
    }

    /**
     * Server-side ground check: scans blocks below the player's bounding box
     * to determine if they should be considered on-ground.
     */
    private boolean isActuallyOnGround(Location loc, Player player) {
        double playerMinX = loc.getX() - 0.3;
        double playerMaxX = loc.getX() + 0.3;
        double playerMinZ = loc.getZ() - 0.3;
        double playerMaxZ = loc.getZ() + 0.3;
        double feetY = loc.getY();

        // Check blocks at feet-level and slightly below
        for (double checkY = feetY - 0.01; checkY >= feetY - 0.5; checkY -= 0.25) {
            int minBlockX = floor(playerMinX);
            int maxBlockX = floor(playerMaxX);
            int minBlockZ = floor(playerMinZ);
            int maxBlockZ = floor(playerMaxZ);
            int blockY = floor(checkY);

            for (int bx = minBlockX; bx <= maxBlockX; bx++) {
                for (int bz = minBlockZ; bz <= maxBlockZ; bz++) {
                    try {
                        Block block = loc.getWorld().getBlockAt(bx, blockY, bz);
                        if (block != null && isSolid(block.getType())) {
                            // Simple solid block check — the block top face
                            // intersects with the player's feet position
                            double blockTop = blockY + getBlockTopHeight(block.getType());
                            if (Math.abs(feetY - blockTop) < 0.1) {
                                return true;
                            }
                        }
                    } catch (Exception ignored) {
                        // World edge or unloaded chunk
                    }
                }
            }
            break; // Only check one level below
        }
        return false;
    }

    private boolean isSolid(Material mat) {
        return mat.isSolid() && mat != Material.SIGN_POST && mat != Material.WALL_SIGN;
    }

    @SuppressWarnings("deprecation")
    private double getBlockTopHeight(Material mat) {
        switch (mat) {
            case STEP:
            case WOOD_STEP:
                return 0.5;
            case SOUL_SAND:
                return 0.875;
            case SNOW:
                return 0.125;
            case ENCHANTMENT_TABLE:
                return 0.75;
            case BED_BLOCK:
                return 0.5625;
            default:
                return 1.0;
        }
    }

    private static int floor(double value) {
        int i = (int) value;
        return value < i ? i - 1 : i;
    }
}
