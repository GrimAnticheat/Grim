package ac.grim.legacyac.check.impl.badpackets;

import ac.grim.legacyac.LegacyAntiCheatPlugin;
import ac.grim.legacyac.check.Check;
import ac.grim.legacyac.data.PlayerData;
import org.bukkit.entity.Player;

/**
 * BadPacketsG — Duplicate sneak status.
 * Flags when the client sends START_SNEAKING when already sneaking,
 * or STOP_SNEAKING when already not sneaking.
 *
 * 1.7 clients can legitimately send duplicate sneak packets in edge cases:
 * - During dimension changes / respawns (server resets state, client doesn't)
 * - When riding entities (dismount can trigger sneak reset)
 * - Due to lag compensation during mid-air sneak toggles
 * - After inventory interactions that desync entity state
 *
 * Uses a buffer-based approach so that occasional duplicates are tolerated
 * but sustained/rapid duplicate sneaking (like a mod) is still flagged.
 */
public final class BadPacketsG extends Check {
    /** Minimum required duplicate sneak packets before we start flagging */
    private static final int MIN_ACTIONS_BEFORE_CHECK = 3;

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
        boolean isRespawn = data.isRecentRespawn();

        // Allow a grace period after join, teleport, vehicle exit, or respawn
        long joinAge = System.currentTimeMillis() - data.getJoinAt();
        boolean isRecent = joinAge < 3000L;
        long teleportAge = System.currentTimeMillis() - data.getLastTeleportAt();
        boolean isRecentTeleport = teleportAge < 2000L;

        if (startSneaking == wasSneaking && data.getSneakActionCount() >= MIN_ACTIONS_BEFORE_CHECK
                && !isRespawn && !isRecent && !isRecentTeleport) {
            // Use a buffer so occasional duplicates are tolerated
            double buffer = increaseBuffer(data, 1.0D);
            double threshold = plugin.getConfig().getDouble("checks.BadPacketsG.buffer-threshold", 4.0D);
            if (buffer > threshold) {
                flag(player, data, 1.0, "duplicateSneak=" + startSneaking);
            }
        } else {
            // Valid state transition or grace period — update tracked state and decay buffer
            data.setLastSneakActionState(startSneaking);
            decreaseBuffer(data, 0.5D);
        }
        data.clearRecentRespawn();
        data.incrementSneakActionCount();
    }
}
