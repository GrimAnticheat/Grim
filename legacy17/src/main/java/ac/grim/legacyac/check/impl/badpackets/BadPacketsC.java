package ac.grim.legacyac.check.impl.badpackets;

import ac.grim.legacyac.LegacyAntiCheatPlugin;
import ac.grim.legacyac.check.Check;
import ac.grim.legacyac.data.PlayerData;
import org.bukkit.entity.Player;

/**
 * BadPacketsC — Self-interaction.
 * Flags when USE_ENTITY targets the player's own entity ID.
 * This is impossible in vanilla and indicates a modified client.
 */
public final class BadPacketsC extends Check {
    public BadPacketsC(LegacyAntiCheatPlugin plugin) {
        super(plugin, "BadPacketsC");
    }

    public void onUseEntity(Player player, PlayerData data, int targetEntityId) {
        if (!isEnabled() || isExempt(player, data)) {
            return;
        }

        if (targetEntityId == player.getEntityId()) {
            flag(player, data, 1.0, "self-interact entityId=" + targetEntityId);
        }
    }
}
