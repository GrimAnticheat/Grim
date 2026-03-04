package ac.grim.legacyac.check.impl.badpackets;

import ac.grim.legacyac.LegacyAntiCheatPlugin;
import ac.grim.legacyac.check.Check;
import ac.grim.legacyac.data.PlayerData;
import org.bukkit.entity.Player;

/**
 * BadPacketsL — Impossible dig action.
 * Flags when the client sends an invalid digging action state transition.
 * E.g., STOP_DESTROY_BLOCK without a preceding START_DESTROY_BLOCK,
 * or ABORT_DESTROY_BLOCK without an active dig session.
 */
public final class BadPacketsL extends Check {
    public BadPacketsL(LegacyAntiCheatPlugin plugin) {
        super(plugin, "BadPacketsL");
    }

    /**
     * @param action 0=START_DESTROY, 1=ABORT_DESTROY, 2=STOP_DESTROY,
     *               3=DROP_ALL_ITEMS, 4=DROP_ITEM, 5=RELEASE_USE_ITEM
     */
    public void onDigAction(Player player, PlayerData data, int action) {
        if (!isEnabled() || isExempt(player, data)) {
            return;
        }

        boolean digging = data.isDiggingActive();

        switch (action) {
            case 0: // START_DESTROY_BLOCK
                data.setDiggingActive(true);
                break;
            case 1: // ABORT_DESTROY_BLOCK
                if (!digging) {
                    double buffer = increaseBuffer(data, 1.0);
                    if (buffer > plugin.getConfig().getDouble("checks.BadPacketsL.buffer", 2.0)) {
                        flag(player, data, 1.0, "abortWithoutStart");
                    }
                }
                data.setDiggingActive(false);
                break;
            case 2: // STOP_DESTROY_BLOCK
                if (!digging) {
                    // Creative mode instant-break can send STOP without START, so only flag in survival
                    if (player.getGameMode() == org.bukkit.GameMode.SURVIVAL) {
                        double buffer = increaseBuffer(data, 1.0);
                        if (buffer > plugin.getConfig().getDouble("checks.BadPacketsL.buffer", 2.0)) {
                            flag(player, data, 1.0, "stopWithoutStart");
                        }
                    }
                }
                data.setDiggingActive(false);
                break;
            default:
                // Actions 3-5 are item actions, not dig state related
                break;
        }
    }
}
