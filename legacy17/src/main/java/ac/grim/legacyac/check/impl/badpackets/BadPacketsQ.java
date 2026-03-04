package ac.grim.legacyac.check.impl.badpackets;

import ac.grim.legacyac.LegacyAntiCheatPlugin;
import ac.grim.legacyac.check.Check;
import ac.grim.legacyac.data.PlayerData;
import org.bukkit.entity.Player;

/**
 * BadPacketsQ — Invalid entity action.
 * Flags when ENTITY_ACTION packet contains an invalid entity ID (not the player's own)
 * or an impossible jump-boost value.
 * In vanilla 1.7.10, ENTITY_ACTION always uses the player's own entity ID.
 */
public final class BadPacketsQ extends Check {
    public BadPacketsQ(LegacyAntiCheatPlugin plugin) {
        super(plugin, "BadPacketsQ");
    }

    public void onEntityAction(Player player, PlayerData data, int entityId, int actionId, int jumpBoost) {
        if (!isEnabled() || isExempt(player, data)) {
            return;
        }

        // Entity ID must match the player's own
        if (entityId != player.getEntityId()) {
            flag(player, data, 1.0, "wrongEntityId=" + entityId + " expected=" + player.getEntityId());
            return;
        }

        // Jump boost values should be 0 in most cases, and small positive for horse jumps
        // Negative or absurdly large values are impossible
        if (jumpBoost < 0 || jumpBoost > 100) {
            flag(player, data, 1.0, "invalidJumpBoost=" + jumpBoost);
        }
    }
}
