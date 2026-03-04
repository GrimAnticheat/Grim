package ac.grim.legacyac.check.impl.badpackets;

import ac.grim.legacyac.LegacyAntiCheatPlugin;
import ac.grim.legacyac.check.Check;
import ac.grim.legacyac.data.PlayerData;
import org.bukkit.entity.Player;

/**
 * BadPacketsG — Duplicate sneak status.
 * Flags when the client sends START_SNEAKING when already sneaking,
 * or STOP_SNEAKING when already not sneaking.
 */
public final class BadPacketsG extends Check {
    public BadPacketsG(LegacyAntiCheatPlugin plugin) {
        super(plugin, "BadPacketsG");
    }

    /**
     * @param startSneaking true if START_SNEAKING, false if STOP_SNEAKING
     */
    public void onSneakAction(Player player, PlayerData data, boolean startSneaking) {
        if (!isEnabled() || isExempt(player, data)) {
            return;
        }

        boolean wasSneaking = data.getLastSneakActionState();
        if (startSneaking == wasSneaking && data.getSneakActionCount() > 0) {
            double buffer = increaseBuffer(data, 1.0);
            if (buffer > plugin.getConfig().getDouble("checks.BadPacketsG.buffer", 2.0)) {
                flag(player, data, 1.0, "duplicateSneak=" + startSneaking);
            }
        }
        data.setLastSneakActionState(startSneaking);
        data.incrementSneakActionCount();
    }
}
