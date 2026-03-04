package ac.grim.legacyac.check.impl.badpackets;

import ac.grim.legacyac.LegacyAntiCheatPlugin;
import ac.grim.legacyac.check.Check;
import ac.grim.legacyac.data.PlayerData;
import org.bukkit.entity.Player;

/**
 * BadPacketsD — Impossible pitch value.
 * Flags when the client sends a pitch value outside the valid range of [-90, 90].
 * Vanilla Minecraft clamps pitch to this range.
 */
public final class BadPacketsD extends Check {
    public BadPacketsD(LegacyAntiCheatPlugin plugin) {
        super(plugin, "BadPacketsD");
    }

    public void onRotation(Player player, PlayerData data, float pitch) {
        if (!isEnabled() || isExempt(player, data)) {
            return;
        }

        if (pitch > 90.0f || pitch < -90.0f) {
            flag(player, data, 1.0, "pitch=" + pitch);
        }
    }
}
