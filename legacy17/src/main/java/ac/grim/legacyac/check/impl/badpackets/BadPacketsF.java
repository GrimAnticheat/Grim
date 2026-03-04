package ac.grim.legacyac.check.impl.badpackets;

import ac.grim.legacyac.LegacyAntiCheatPlugin;
import ac.grim.legacyac.check.Check;
import ac.grim.legacyac.data.PlayerData;
import org.bukkit.entity.Player;

/**
 * BadPacketsF — Duplicate sprint status.
 * Flags when the client sends START_SPRINTING when already sprinting,
 * or STOP_SPRINTING when already not sprinting via ENTITY_ACTION packets.
 * Vanilla clients only toggle, never duplicate.
 */
public final class BadPacketsF extends Check {
    public BadPacketsF(LegacyAntiCheatPlugin plugin) {
        super(plugin, "BadPacketsF");
    }

    /**
     * @param startSprinting true if START_SPRINTING, false if STOP_SPRINTING
     */
    public void onSprintAction(Player player, PlayerData data, boolean startSprinting) {
        if (!isEnabled() || isExempt(player, data)) {
            return;
        }

        boolean wasSprinting = data.getLastSprintActionState();
        if (startSprinting == wasSprinting && data.getSprintActionCount() > 0) {
            double buffer = increaseBuffer(data, 1.0);
            if (buffer > plugin.getConfig().getDouble("checks.BadPacketsF.buffer", 2.0)) {
                flag(player, data, 1.0, "duplicateSprint=" + startSprinting);
            }
        }
        data.setLastSprintActionState(startSprinting);
        data.incrementSprintActionCount();
    }
}
