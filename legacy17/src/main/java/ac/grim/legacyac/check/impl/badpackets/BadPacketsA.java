package ac.grim.legacyac.check.impl.badpackets;

import ac.grim.legacyac.LegacyAntiCheatPlugin;
import ac.grim.legacyac.check.Check;
import ac.grim.legacyac.data.PlayerData;
import org.bukkit.entity.Player;

/**
 * BadPacketsA — Duplicate hotbar slot change.
 * Flags when the client sends HELD_ITEM_CHANGE with the same slot ID twice in a row.
 * This is impossible in vanilla — the client only sends this when the slot actually changes.
 */
public final class BadPacketsA extends Check {
    public BadPacketsA(LegacyAntiCheatPlugin plugin) {
        super(plugin, "BadPacketsA");
    }

    public void onHeldItemChange(Player player, PlayerData data, int newSlot) {
        if (!isEnabled() || isExempt(player, data)) {
            return;
        }

        int lastSlot = data.getLastHeldSlot();
        if (lastSlot == newSlot && data.getHeldSlotChangeCount() > 0) {
            flag(player, data, 1.0, "slot=" + newSlot + " duplicate");
        }
        data.setLastHeldSlot(newSlot);
        data.incrementHeldSlotChangeCount();
    }
}
