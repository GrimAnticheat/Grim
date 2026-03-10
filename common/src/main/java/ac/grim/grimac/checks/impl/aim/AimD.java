package ac.grim.grimac.checks.impl.aim;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.RotationCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.RotationUpdate;

@CheckData(name = "AimD", description = "Alternating yaw oscillation pattern", decay = 0.02, experimental = true)
public class AimD extends Check implements RotationCheck {
    private float lastSignedYaw;
    private int oscillationStreak;

    public AimD(GrimPlayer player) {
        super(player);
    }

    @Override
    public void process(RotationUpdate rotationUpdate) {
        final float signedYaw = rotationUpdate.getDeltaXRot();
        final float absYaw = Math.abs(signedYaw);
        final float absPitch = rotationUpdate.getDeltaYRotABS();

        if (player.packetStateData.lastPacketWasTeleport || absYaw < 1.5F || absPitch > 0.08F || !player.actionManager.hasAttackedSince(300)) {
            oscillationStreak = 0;
            lastSignedYaw = signedYaw;
            return;
        }

        final boolean oppositeDirection = Math.signum(signedYaw) != Math.signum(lastSignedYaw);
        final boolean sameMagnitude = Math.abs(absYaw - Math.abs(lastSignedYaw)) < 0.01F;

        if (oppositeDirection && sameMagnitude) {
            if (++oscillationStreak > 9) {
                flagAndAlert("yaw=" + absYaw + ", streak=" + oscillationStreak);
            }
        } else {
            oscillationStreak = Math.max(0, oscillationStreak - 1);
            reward();
        }

        lastSignedYaw = signedYaw;
    }
}
