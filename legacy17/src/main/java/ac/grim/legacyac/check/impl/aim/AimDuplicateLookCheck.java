package ac.grim.legacyac.check.impl.aim;

import ac.grim.legacyac.LegacyAntiCheatPlugin;
import ac.grim.legacyac.check.Check;
import ac.grim.legacyac.data.PlayerData;
import ac.grim.legacyac.network.frame.MovementFrame;
import org.bukkit.entity.Player;

public final class AimDuplicateLookCheck extends Check {
    public AimDuplicateLookCheck(LegacyAntiCheatPlugin plugin) {
        super(plugin, "AimDuplicateLook");
    }

    public void onMovementFrame(Player player, MovementFrame frame, PlayerData data) {
        if (!frame.hasLook() || isExempt(player, data)) {
            return;
        }
        if (player.getVehicle() != null || player.isInsideVehicle()) {
            return;
        }
        if (Math.abs(data.getLastYawDelta()) < 1.0E-4F && Math.abs(data.getLastPitchDelta()) < 1.0E-4F) {
            double buffer = increaseBuffer(data, 0.5D);
            if (buffer > plugin.getConfig().getDouble("checks.AimDuplicateLook.buffer", 1.25D)) {
                flag(player, data, 0.5D, "duplicate-look");
            }
        } else {
            coolDownScore(data);
        }
    }
}
