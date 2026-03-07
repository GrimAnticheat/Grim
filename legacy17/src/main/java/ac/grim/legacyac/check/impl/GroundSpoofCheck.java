package ac.grim.legacyac.check.impl;

import ac.grim.legacyac.LegacyAntiCheatPlugin;
import ac.grim.legacyac.check.Check;
import ac.grim.legacyac.data.PlayerData;
import ac.grim.legacyac.network.frame.MovementFrame;
import ac.grim.legacyac.util.collision.LegacyBlockBoxResolver;
import ac.grim.legacyac.world.LegacyBlockState;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;

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

        if (!frame.hasPosition()) {
            return;
        }

        if (player.isInsideVehicle() || player.isFlying() || player.getAllowFlight()) {
            consecutiveSpoofTicks = 0;
            return;
        }

        boolean clientOnGround = frame.isOnGround();
        boolean serverOnGround = isActuallyOnGround(to, player, data);

        if (clientOnGround && !serverOnGround) {
            double deltaY = data.getLastDeltaY();
            int airTicks = data.getAirTicks();
            if (airTicks > 3 && deltaY < -0.08D) {
                consecutiveSpoofTicks++;
                if (consecutiveSpoofTicks >= plugin.getConfig().getInt("checks.GroundSpoof.min-ticks", 3)) {
                    double buffer = increaseBuffer(data, 1.0D);
                    if (buffer > plugin.getConfig().getDouble("checks.GroundSpoof.buffer", 2.0D)) {
                        flag(player, data, 1.0D,
                                "claimsGround=true serverGround=false airTicks=" + airTicks
                                        + " deltaY=" + String.format("%.3f", deltaY));
                    }
                }
                return;
            }
        }

        if (!clientOnGround && serverOnGround) {
            double deltaY = data.getLastDeltaY();
            int groundTicks = data.getGroundTicks();
            if (groundTicks > 5 && Math.abs(deltaY) < 0.01D) {
                consecutiveSpoofTicks++;
                if (consecutiveSpoofTicks >= plugin.getConfig().getInt("checks.GroundSpoof.min-ticks", 3)) {
                    double buffer = increaseBuffer(data, 0.5D);
                    if (buffer > plugin.getConfig().getDouble("checks.GroundSpoof.buffer", 2.0D)) {
                        flag(player, data, 0.5D,
                                "claimsGround=false serverGround=true groundTicks=" + groundTicks);
                    }
                }
                return;
            }
        }

        if (consecutiveSpoofTicks > 0) {
            consecutiveSpoofTicks--;
        }
    }

    private boolean isActuallyOnGround(Location loc, Player player, PlayerData data) {
        double playerMinX = loc.getX() - 0.3D;
        double playerMaxX = loc.getX() + 0.3D;
        double playerMinZ = loc.getZ() - 0.3D;
        double playerMaxZ = loc.getZ() + 0.3D;
        double feetY = loc.getY();

        int minBlockX = floor(playerMinX);
        int maxBlockX = floor(playerMaxX);
        int minBlockZ = floor(playerMinZ);
        int maxBlockZ = floor(playerMaxZ);
        int blockY = floor(feetY - 0.01D);

        for (int bx = minBlockX; bx <= maxBlockX; bx++) {
            for (int bz = minBlockZ; bz <= maxBlockZ; bz++) {
                LegacyBlockState state = data.getCompensatedBlockState(player, bx, blockY, bz);
                if (state == null || !isSolid(state.getType())) {
                    continue;
                }
                double blockTop = blockY + LegacyBlockBoxResolver.getTopHeight(state.getType(), state.getData());
                if (Math.abs(feetY - blockTop) < 0.1D) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isSolid(Material mat) {
        return mat.isSolid() && mat != Material.SIGN_POST && mat != Material.WALL_SIGN;
    }

    private static int floor(double value) {
        int i = (int) value;
        return value < i ? i - 1 : i;
    }
}
