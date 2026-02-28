package ac.grim.legacyac.check.impl;

import ac.grim.legacyac.LegacyAntiCheatPlugin;
import ac.grim.legacyac.check.Check;
import ac.grim.legacyac.data.PlayerData;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerVelocityEvent;
import org.bukkit.util.Vector;
import java.util.Locale;

public final class VelocityCheck extends Check {
    public VelocityCheck(LegacyAntiCheatPlugin plugin) {
        super(plugin, "Velocity");
    }

    public void onVelocity(PlayerVelocityEvent event, PlayerData data) {
        if (!isEnabled()) {
            return;
        }

        Vector velocity = event.getVelocity();
        if (velocity == null) {
            return;
        }

        int ticks = plugin.getConfig().getInt("checks.Velocity.window-ticks", 8);
        data.armVelocityWindow(velocity, ticks);
    }

    public void onMove(PlayerMoveEvent event, PlayerData data) {
        if (!isEnabled()) {
            return;
        }

        Player player = event.getPlayer();
        if (isExempt(player, data, true)) {
            return;
        }

        if (data.hasPendingVelocityWindow()) {
            return;
        }

        if (!data.hasCompletedVelocityWindow()) {
            return;
        }

        double expectedXZ = data.getExpectedVelocityXZ();
        double observedXZ = data.getObservedVelocityXZ();
        double expectedY = data.getExpectedVelocityY();
        double observedY = data.getObservedVelocityY();

        double minExpectedXZ = plugin.getConfig().getDouble("checks.Velocity.min-expected-xz", 0.12D);
        double minRatioXZ = plugin.getConfig().getDouble("checks.Velocity.min-response-ratio-xz", 0.35D);
        double minRatioY = plugin.getConfig().getDouble("checks.Velocity.min-response-ratio-y", 0.20D);

        boolean failXZ = expectedXZ >= minExpectedXZ && observedXZ < expectedXZ * minRatioXZ;
        boolean failY = expectedY > 0.05D && observedY < expectedY * minRatioY;

        if (failXZ || failY) {
            double buffer = increaseBuffer(data, 1.0D);
            if (buffer > plugin.getConfig().getDouble("checks.Velocity.buffer", 1.5D)) {
                flag(player, data, 1.0D,
                    "expXZ=" + String.format(Locale.ROOT, "%.3f", expectedXZ) + " obsXZ=" + String.format(Locale.ROOT, "%.3f", observedXZ)
                        + " expY=" + String.format(Locale.ROOT, "%.3f", expectedY) + " obsY=" + String.format(Locale.ROOT, "%.3f", observedY));
            }
        }

        data.clearVelocityWindow();
    }
}
