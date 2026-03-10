package ac.grim.grimac.checks.impl.aim;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.RotationCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.RotationUpdate;

@CheckData(name = "AimG", description = "Long micro-adjust chains during combat", decay = 0.02, experimental = true)
public class AimG extends Check implements RotationCheck {
    private int microAdjustStreak;

    public AimG(GrimPlayer player) {
        super(player);
    }

    @Override
    public void process(RotationUpdate rotationUpdate) {
        if (player.packetStateData.lastPacketWasTeleport || !player.actionManager.hasAttackedSince(350)) {
            microAdjustStreak = 0;
            return;
        }

        float yaw = rotationUpdate.getDeltaXRotABS();
        float pitch = rotationUpdate.getDeltaYRotABS();
        if (yaw > 0.01F && yaw < 0.20F && pitch > 0.01F && pitch < 0.20F) {
            if (++microAdjustStreak > 20) {
                flagAndAlert("streak=" + microAdjustStreak + ", yaw=" + yaw + ", pitch=" + pitch);
            }
        } else {
            microAdjustStreak = Math.max(0, microAdjustStreak - 2);
            reward();
        }
    }
}
