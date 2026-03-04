package ac.grim.legacyac.check.impl.badpackets;

import ac.grim.legacyac.LegacyAntiCheatPlugin;
import ac.grim.legacyac.check.Check;
import ac.grim.legacyac.data.PlayerData;
import org.bukkit.entity.Player;

/**
 * BadPacketsE — Excessive look-only packets without position.
 * Flags when the client sends too many consecutive LOOK/FLYING packets
 * without ever sending a position update. This pattern indicates packet
 * manipulation (e.g., timer manipulation that only sends look packets).
 */
public final class BadPacketsE extends Check {
    public BadPacketsE(LegacyAntiCheatPlugin plugin) {
        super(plugin, "BadPacketsE");
    }

    public void onFlyingPacket(Player player, PlayerData data, boolean hasPosition) {
        if (!isEnabled() || isExempt(player, data)) {
            return;
        }

        if (hasPosition) {
            data.resetConsecutiveLookOnlyPackets();
            return;
        }

        int count = data.incrementConsecutiveLookOnlyPackets();
        int maxConsecutive = plugin.getConfig().getInt("checks.BadPacketsE.max-consecutive-look-only", 30);
        if (count > maxConsecutive) {
            double buffer = increaseBuffer(data, 1.0);
            if (buffer > plugin.getConfig().getDouble("checks.BadPacketsE.buffer", 3.0)) {
                flag(player, data, 1.0, "lookOnly=" + count);
            }
        }
    }
}
