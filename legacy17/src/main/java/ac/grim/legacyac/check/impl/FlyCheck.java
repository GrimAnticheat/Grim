package ac.grim.legacyac.check.impl;

import ac.grim.legacyac.LegacyAntiCheatPlugin;
import ac.grim.legacyac.check.Check;
import ac.grim.legacyac.data.PlayerData;
import ac.grim.legacyac.network.frame.MovementFrame;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;
import java.util.Locale;

public final class FlyCheck extends Check {
    public FlyCheck(LegacyAntiCheatPlugin plugin) {
        super(plugin, "Fly");
    }

    public void onMove(PlayerMoveEvent event, PlayerData data) {
        if (event.getTo() == null) {
            return;
        }
        MovementFrame frame = new MovementFrame(System.nanoTime(), event.getTo().getX(), event.getTo().getY(), event.getTo().getZ(), event.getTo().getYaw(), event.getTo().getPitch(), event.getPlayer().isOnGround(), true, true, MovementFrame.Source.BUKKIT_MOVE_EVENT);
        onMovementFrame(event.getPlayer(), frame, event.getTo(), data);
    }

    public void onMovementFrame(Player player, MovementFrame frame, Location to, PlayerData data) {
        if (!isEnabled()) {
            return;
        }

        if (isExempt(player, data)) {
            return;
        }
        if (!data.getMovementStateSnapshot().isTeleportAligned()) {
            return;
        }
        if (player.isFlying() || player.getAllowFlight() || player.getVehicle() != null) {
            return;
        }
        if (data.getPredictionContext().isRecentRodPull() || data.getPredictionContext().isRecentVelocity()) {
            return;
        }
        if (System.currentTimeMillis() - data.getLastVelocityAt() < 1200L) {
            return;
        }

        // Check for liquid at current position AND at feet level
        // Must check both WATER/STATIONARY_WATER and LAVA/STATIONARY_LAVA
        // Also check block below for edge cases (standing on surface of water)
        Material feetBlock = to.getBlock().getType();
        Material belowBlock = to.clone().add(0.0D, -0.5D, 0.0D).getBlock().getType();
        if (frame.isOnGround() || isLiquid(feetBlock) || isLiquid(belowBlock)) {
            return;
        }

        // Check for ladders/vines
        if (feetBlock == Material.LADDER || feetBlock == Material.VINE) {
            return;
        }

        // Check for web (slows fall significantly)
        if (feetBlock == Material.WEB) {
            return;
        }

        if (data.getAirTicks() > plugin.getConfig().getInt("checks.Fly.air-ticks-threshold", 10)
                && Math.abs(data.getLastDeltaY()) < plugin.getConfig().getDouble("checks.Fly.max-dy", 0.02D)) {
            double buffer = increaseBuffer(data, 1.0D);
            if (buffer > plugin.getConfig().getDouble("checks.Fly.buffer", 2.0D)) {
                flag(player, data, 1.0D, "airTicks=" + data.getAirTicks() + " dy=" + String.format(Locale.ROOT, "%.4f", data.getLastDeltaY()));
            }
        }
    }

    private static boolean isLiquid(Material material) {
        return material == Material.WATER || material == Material.STATIONARY_WATER
                || material == Material.LAVA || material == Material.STATIONARY_LAVA;
    }
}

