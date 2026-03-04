package ac.grim.legacyac.check.impl.badpackets;

import ac.grim.legacyac.LegacyAntiCheatPlugin;
import ac.grim.legacyac.check.Check;
import ac.grim.legacyac.data.PlayerData;
import org.bukkit.entity.Player;

/**
 * BadPacketsI — Flying without permission.
 * Flags when the client sends abilities packets claiming to be flying
 * while the server has not granted flight. In 1.7.10 this is detected
 * via the player abilities flag in the client packet.
 */
public final class BadPacketsI extends Check {
    public BadPacketsI(LegacyAntiCheatPlugin plugin) {
        super(plugin, "BadPacketsI");
    }

    public void onAbilitiesPacket(Player player, PlayerData data, boolean claimsFlying) {
        if (!isEnabled() || isExempt(player, data)) {
            return;
        }

        if (claimsFlying && !player.getAllowFlight()) {
            flag(player, data, 1.0, "flyingWithoutPermission");
        }
    }
}
