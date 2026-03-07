package ac.grim.legacyac.check.impl.badpackets;

import ac.grim.legacyac.LegacyAntiCheatPlugin;
import ac.grim.legacyac.check.Check;
import ac.grim.legacyac.data.PlayerData;
import org.bukkit.entity.Player;

/**
 * BadPacketsA — Duplicate hotbar slot change.
 * Flags when the client sends HELD_ITEM_CHANGE with the same slot ID twice in a row.
 * This is impossible in vanilla — the client only sends this when the slot actually changes.
 *
 * However, in 1.7 clients there are legitimate edge cases:
 * - After consuming a potion/food, the client may re-send the slot to
 *   sync state with the server after the item is removed from the slot.
 * - During rapid inventory interactions that cause client-server desync.
 * - After slot switch grace period (from compensation state).
 *
 * Uses a buffer with grace period to prevent false positives.
 */
public final class BadPacketsA extends Check {
    /** Grace period in ms after which a duplicate slot is suspicious */
    private static final long DUPLICATE_GRACE_MS = 500L;

    public BadPacketsA(LegacyAntiCheatPlugin plugin) {
        super(plugin, "BadPacketsA");
    }

    public void onHeldItemChange(Player player, PlayerData data, int newSlot) {
        if (!isEnabled() || isExempt(player, data)) {
            return;
        }

        int lastSlot = data.getLastHeldSlot();
        if (lastSlot == newSlot && data.getHeldSlotChangeCount() > 0) {
            // Check if player is in slot switch grace period (after consuming items, etc.)
            if (data.isInSlotSwitchGrace()) {
                // Tolerate — this is a known 1.7 edge case after item consumption
                data.setLastHeldSlot(newSlot);
                data.incrementHeldSlotChangeCount();
                return;
            }

            // Check if enough time has passed since last slot change
            long timeSinceLastSwitch = System.currentTimeMillis() - data.getLastSlotSwitchAt();
            if (timeSinceLastSwitch < DUPLICATE_GRACE_MS) {
                // Too soon — might be a desync from item consumption
                data.setLastHeldSlot(newSlot);
                data.incrementHeldSlotChangeCount();
                return;
            }

            double buffer = increaseBuffer(data, 1.0D);
            double threshold = plugin.getConfig().getDouble("checks.BadPacketsA.buffer-threshold", 2.0D);
            if (buffer > threshold) {
                flag(player, data, 1.0, "slot=" + newSlot + " duplicate");
            }
        } else {
            decreaseBuffer(data, 0.5D);
        }
        data.setLastHeldSlot(newSlot);
        data.incrementHeldSlotChangeCount();
    }
}
