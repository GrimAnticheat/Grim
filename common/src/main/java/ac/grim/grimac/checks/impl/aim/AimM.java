package ac.grim.grimac.checks.impl.aim;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.RotationCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.RotationUpdate;

@CheckData(name = "AimM", description = "Rapid direction flips at similar speed", decay = 0.02, experimental = true)
public class AimM extends Check implements RotationCheck {
    private float lastSignedYaw;
    private int flipStreak;

    public AimM(GrimPlayer player) {
        super(player);
    }

    @Override
    public void process(RotationUpdate rotationUpdate) {
        if (!player.actionManager.hasAttackedSince(300) || player.packetStateData.lastPacketWasTeleport) {
            lastSignedYaw = rotationUpdate.getDeltaXRot();
            flipStreak = 0;
            return;
        }

        float signedYaw = rotationUpdate.getDeltaXRot();
        float absYaw = Math.abs(signedYaw);
        if (Math.signum(signedYaw) != Math.signum(lastSignedYaw)
                && absYaw > 1.5F
                && Math.abs(absYaw - Math.abs(lastSignedYaw)) < 0.03F) {
            if (++flipStreak > 10) {
                flagAndAlert("yaw=" + absYaw + ", flips=" + flipStreak);
            }
        } else {
            flipStreak = Math.max(0, flipStreak - 1);
            reward();
        }

        lastSignedYaw = signedYaw;
    }
}
