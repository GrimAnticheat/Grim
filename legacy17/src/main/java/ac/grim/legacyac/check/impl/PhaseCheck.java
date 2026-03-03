package ac.grim.legacyac.check.impl;

import ac.grim.legacyac.LegacyAntiCheatPlugin;
import ac.grim.legacyac.check.Check;
import ac.grim.legacyac.data.PlayerData;
import ac.grim.legacyac.network.frame.MovementFrame;
import ac.grim.legacyac.util.collision.LegacyBlockBoxResolver;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;

public final class PhaseCheck extends Check {
    private static final String PHASE_STREAK_KEY = "Phase:overlap-streak";

    public PhaseCheck(LegacyAntiCheatPlugin plugin) {
        super(plugin, "Phase");
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
        if (player.getVehicle() != null || player.isInsideVehicle()) {
            return;
        }

        LegacyBlockBoxResolver.Box playerBox = buildPlayerBox(player, to);
        double playerVolume = playerBox.volume();
        if (playerVolume <= 0.0D) {
            return;
        }

        OverlapResult overlap = findMaxOverlap(player, playerBox, playerVolume);
        if (overlap.maxRatio <= 0.0D) {
            data.scaleBuffer(PHASE_STREAK_KEY, 0.0D);
            return;
        }

        double minOverlapRatio = plugin.getConfig().getDouble("checks.Phase.min-overlap-ratio", 0.08D);
        int minConsecutiveTicks = plugin.getConfig().getInt("checks.Phase.min-consecutive-ticks", 2);
        if (overlap.thinCollision) {
            minConsecutiveTicks = Math.max(minConsecutiveTicks, 3);
        }

        if (overlap.maxRatio < minOverlapRatio) {
            data.scaleBuffer(PHASE_STREAK_KEY, 0.0D);
            return;
        }

        int streak = (int) data.addBuffer(PHASE_STREAK_KEY, 1.0D);
        if (streak < minConsecutiveTicks) {
            return;
        }

        double buffer = increaseBuffer(data, overlap.maxRatio);
        if (buffer > plugin.getConfig().getDouble("checks.Phase.buffer", 1.5D)) {
            flag(player, data, overlap.maxRatio,
                "inside=" + overlap.material.name()
                    + ",ratio=" + String.format(java.util.Locale.ROOT, "%.4f", overlap.maxRatio)
                    + ",streak=" + streak);
        }
    }

    private LegacyBlockBoxResolver.Box buildPlayerBox(Player player, Location to) {
        double halfWidth = 0.3D;
        double height = player.isSneaking() ? 1.65D : 1.8D;
        return new LegacyBlockBoxResolver.Box(
            to.getX() - halfWidth,
            to.getY(),
            to.getZ() - halfWidth,
            to.getX() + halfWidth,
            to.getY() + height,
            to.getZ() + halfWidth);
    }

    private OverlapResult findMaxOverlap(Player player, LegacyBlockBoxResolver.Box playerBox, double playerVolume) {
        int minX = floor(playerBox.getMinX());
        int maxX = floor(playerBox.getMaxX());
        int minY = floor(playerBox.getMinY());
        int maxY = floor(playerBox.getMaxY());
        int minZ = floor(playerBox.getMinZ());
        int maxZ = floor(playerBox.getMaxZ());

        double maxRatio = 0.0D;
        Material maxMaterial = Material.AIR;
        boolean thin = false;

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    Block block = player.getWorld().getBlockAt(x, y, z);
                    List<LegacyBlockBoxResolver.Box> boxes = LegacyBlockBoxResolver.resolve(block);
                    if (boxes.isEmpty()) {
                        continue;
                    }
                    for (LegacyBlockBoxResolver.Box blockBox : boxes) {
                        double overlapVolume = playerBox.overlapVolume(blockBox);
                        if (overlapVolume <= 0.0D) {
                            continue;
                        }
                        double ratio = overlapVolume / playerVolume;
                        if (ratio > maxRatio) {
                            maxRatio = ratio;
                            maxMaterial = block.getType();
                            thin = LegacyBlockBoxResolver.isThinCollision(maxMaterial);
                        }
                    }
                }
            }
        }

        return new OverlapResult(maxRatio, maxMaterial, thin);
    }

    private int floor(double value) {
        return (int) Math.floor(value);
    }

    private static final class OverlapResult {
        private final double maxRatio;
        private final Material material;
        private final boolean thinCollision;

        private OverlapResult(double maxRatio, Material material, boolean thinCollision) {
            this.maxRatio = maxRatio;
            this.material = material;
            this.thinCollision = thinCollision;
        }
    }
}
