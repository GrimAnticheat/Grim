package ac.grim.legacyac.check.impl.badpackets;

import ac.grim.legacyac.LegacyAntiCheatPlugin;
import ac.grim.legacyac.check.Check;
import ac.grim.legacyac.data.PlayerData;
import org.bukkit.entity.Player;

/**
 * CrashA — Position out of bounds (server crash prevention).
 * Flags when the client sends a position with coordinates beyond the world border
 * hard-cap, or NaN/Infinity values. These can cause server crashes or chunk-loading
 * exploits.
 */
public final class CrashA extends Check {
    private static final double MAX_XZ = 2.9999999E7;
    private static final double MAX_Y = 1.0E9;

    public CrashA(LegacyAntiCheatPlugin plugin) {
        super(plugin, "CrashA");
    }

    public void onPosition(Player player, PlayerData data, double x, double y, double z) {
        if (!isEnabled()) {
            return;
        }

        if (Double.isNaN(x) || Double.isNaN(y) || Double.isNaN(z)
                || Double.isInfinite(x) || Double.isInfinite(y) || Double.isInfinite(z)) {
            flag(player, data, 1.0, "NaN/Inf x=" + x + " y=" + y + " z=" + z);
            return;
        }

        if (Math.abs(x) > MAX_XZ || Math.abs(z) > MAX_XZ) {
            flag(player, data, 1.0, "outOfBounds x=" + x + " z=" + z);
            return;
        }

        if (Math.abs(y) > MAX_Y) {
            flag(player, data, 1.0, "outOfBoundsY y=" + y);
        }
    }

    public void onRotation(Player player, PlayerData data, float yaw, float pitch) {
        if (!isEnabled()) {
            return;
        }

        if (Float.isNaN(yaw) || Float.isNaN(pitch)
                || Float.isInfinite(yaw) || Float.isInfinite(pitch)) {
            flag(player, data, 1.0, "NaN/Inf yaw=" + yaw + " pitch=" + pitch);
        }
    }
}
