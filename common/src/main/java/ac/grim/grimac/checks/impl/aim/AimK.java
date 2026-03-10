package ac.grim.grimac.checks.impl.aim;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.RotationCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.RotationUpdate;

@CheckData(name = "AimK", description = "Excessively equal yaw and pitch magnitudes", decay = 0.02, experimental = true)
public class AimK extends Check implements RotationCheck {
    private int equalityStreak;

    public AimK(GrimPlayer player) {
        super(player);
    }

    @Override
    public void process(RotationUpdate rotationUpdate) {
        if (!player.actionManager.hasAttackedSince(350) || player.packetStateData.lastPacketWasTeleport) {
            equalityStreak = 0;
            return;
        }

        float yaw = rotationUpdate.getDeltaXRotABS();
        float pitch = rotationUpdate.getDeltaYRotABS();
        if (yaw > 0.3F && pitch > 0.3F && Math.abs(yaw - pitch) < 1.0E-3F) {
            if (++equalityStreak > 8) {
                flagAndAlert("yaw=" + yaw + ", pitch=" + pitch + ", streak=" + equalityStreak);
            }
        } else {
            equalityStreak = Math.max(0, equalityStreak - 1);
            reward();
        }
    }
}
