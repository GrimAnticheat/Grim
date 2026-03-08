package ac.grim.legacyac.check.impl;

import ac.grim.legacyac.LegacyAntiCheatPlugin;
import ac.grim.legacyac.check.Check;
import ac.grim.legacyac.data.PlayerData;
import ac.grim.legacyac.network.frame.MovementFrame;
import ac.grim.legacyac.util.collision.LegacyBlockBoxResolver;
import ac.grim.legacyac.world.LegacyBlockState;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;

public final class PhaseCheck extends Check {
    private static final String PHASE_STREAK_KEY = "Phase:overlap-streak";
    private static final String THIN_PHASE_STREAK_KEY = "Phase:thin-overlap-streak";

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

        OverlapResult overlap = findMaxOverlap(player, data, playerBox, playerVolume);
        if (overlap.maxRatio <= 0.0D || overlap.maxVolume <= 0.0D) {
            data.scaleBuffer(PHASE_STREAK_KEY, 0.0D);
            data.scaleBuffer(THIN_PHASE_STREAK_KEY, 0.0D);
            return;
        }

        double minOverlapRatio = plugin.getConfig().getDouble("checks.Phase.min-overlap-ratio", 0.08D);
        double minOverlapVolume = plugin.getConfig().getDouble("checks.Phase.min-overlap-volume", 0.018D);
        int minConsecutiveTicks = plugin.getConfig().getInt("checks.Phase.min-consecutive-ticks", 2);
        int thinMinConsecutiveTicks = plugin.getConfig().getInt("checks.Phase.thin-min-consecutive-ticks", 4);

        if (overlap.maxRatio < minOverlapRatio || overlap.maxVolume < minOverlapVolume) {
            data.scaleBuffer(PHASE_STREAK_KEY, 0.0D);
            data.scaleBuffer(THIN_PHASE_STREAK_KEY, 0.0D);
            return;
        }

        String streakKey = overlap.thinCollision ? THIN_PHASE_STREAK_KEY : PHASE_STREAK_KEY;
        int requiredTicks = overlap.thinCollision ? Math.max(thinMinConsecutiveTicks, minConsecutiveTicks) : minConsecutiveTicks;
        if (!overlap.thinCollision) {
            data.scaleBuffer(THIN_PHASE_STREAK_KEY, 0.0D);
        }

        int streak = (int) data.addBuffer(streakKey, 1.0D);
        if (streak < requiredTicks) {
            return;
        }

        double buffer = increaseBuffer(data, overlap.maxRatio);
        if (buffer > plugin.getConfig().getDouble("checks.Phase.buffer", 1.5D)) {
            flag(player, data, overlap.maxRatio,
                    "inside=" + overlap.material.name()
                            + ",volume=" + String.format(java.util.Locale.ROOT, "%.4f", overlap.maxVolume)
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

    private OverlapResult findMaxOverlap(final Player player, final PlayerData data,
            LegacyBlockBoxResolver.Box playerBox, double playerVolume) {
        int minX = floor(playerBox.getMinX());
        int maxX = floor(playerBox.getMaxX());
        int minY = floor(playerBox.getMinY());
        int maxY = floor(playerBox.getMaxY());
        int minZ = floor(playerBox.getMinZ());
        int maxZ = floor(playerBox.getMaxZ());

        double maxRatio = 0.0D;
        double maxVolume = 0.0D;
        Material maxMaterial = Material.AIR;
        boolean thin = false;

        LegacyBlockBoxResolver.BlockAccess access = new LegacyBlockBoxResolver.BlockAccess() {
            @Override
            public LegacyBlockState getBlockState(int x, int y, int z) {
                return data.getCompensatedBlockState(player, x, y, z);
            }
        };

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    LegacyBlockState state = data.getCompensatedBlockState(player, x, y, z);
                    List<LegacyBlockBoxResolver.Box> boxes = LegacyBlockBoxResolver.resolve(state, x, y, z, access);
                    if (!boxes.isEmpty()) {
                        for (LegacyBlockBoxResolver.Box blockBox : boxes) {
                            double overlapVolume = playerBox.overlapVolume(blockBox);
                            if (overlapVolume <= 0.0D) {
                                continue;
                            }
                            double ratio = overlapVolume / playerVolume;
                            if (ratio > maxRatio) {
                                maxRatio = ratio;
                                maxVolume = overlapVolume;
                                maxMaterial = state.getType();
                                thin = LegacyBlockBoxResolver.isThinCollision(maxMaterial);
                            }
                        }
                    } else if (isSolid(state.getType())) {
                        LegacyBlockBoxResolver.Box fallback = new LegacyBlockBoxResolver.Box(x, y, z, x + 1.0D, y + 1.0D, z + 1.0D);
                        double overlapVolume = playerBox.overlapVolume(fallback);
                        if (overlapVolume <= 0.0D) {
                            continue;
                        }
                        double ratio = overlapVolume / playerVolume;
                        if (ratio > maxRatio) {
                            maxRatio = ratio;
                            maxVolume = overlapVolume;
                            maxMaterial = state.getType();
                            thin = false;
                        }
                    }
                }
            }
        }

        return new OverlapResult(maxRatio, maxVolume, maxMaterial, thin);
    }

    private int floor(double value) {
        return (int) Math.floor(value);
    }

    private boolean isPhaseBlacklist(Material material) {
        String name = material.name();
        return "THIN_GLASS".equals(name) || "GLASS_PANE".equals(name);
    }

    private boolean isSolid(Material material) {
        if (material.isSolid()) {
            return true;
        }
        String name = material.name();
        return name.contains("GLASS")
                || name.contains("FENCE")
                || name.contains("WALL")
                || name.contains("STAIRS")
                || name.contains("SLAB")
                || name.contains("STEP")
                || "CARPET".equals(name);
    }

    private static final class OverlapResult {
        private final double maxRatio;
        private final double maxVolume;
        private final Material material;
        private final boolean thinCollision;

        private OverlapResult(double maxRatio, double maxVolume, Material material, boolean thinCollision) {
            this.maxRatio = maxRatio;
            this.maxVolume = maxVolume;
            this.material = material;
            this.thinCollision = thinCollision;
        }
    }
}

