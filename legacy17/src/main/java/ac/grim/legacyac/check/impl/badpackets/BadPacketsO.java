package ac.grim.legacyac.check.impl.badpackets;

import ac.grim.legacyac.LegacyAntiCheatPlugin;
import ac.grim.legacyac.check.Check;
import ac.grim.legacyac.data.PlayerData;
import org.bukkit.entity.Player;

/**
 * BadPacketsO — Invalid keep-alive response.
 * Flags when the client sends a KEEP_ALIVE response with an ID that
 * does not match any pending keep-alive sent by the server.
 * This detects packet fabrication or keep-alive ID manipulation.
 */
public final class BadPacketsO extends Check {
    public BadPacketsO(LegacyAntiCheatPlugin plugin) {
        super(plugin, "BadPacketsO");
    }

    /**
     * Called when a client keep-alive response is received.
     * @param matched true if the keep-alive ID matched a pending server keep-alive
     */
    public void onKeepAliveResponse(Player player, PlayerData data, boolean matched) {
        if (!isEnabled() || isExempt(player, data)) {
            return;
        }

        if (!matched) {
            flag(player, data, 1.0, "invalidKeepAliveId");
        }
    }
}
