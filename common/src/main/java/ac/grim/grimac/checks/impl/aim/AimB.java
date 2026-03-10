package ac.grim.grimac.checks.impl.aim;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.RotationCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.RotationUpdate;

@CheckData(name = "AimB", description = "Repeated identical rotation steps", decay = 0.02, experimental = true)
public class AimB extends Check implements RotationCheck {
    private float lastYawDelta;
    private float lastPitchDelta;
    private int duplicateStreak;

    public AimB(GrimPlayer player) {
        super(player);
    }

    @Override
    public void process(RotationUpdate rotationUpdate) {
        final float absYaw = rotationUpdate.getDeltaXRotABS();
        final float absPitch = rotationUpdate.getDeltaYRotABS();

        if (player.packetStateData.lastPacketWasTeleport || absYaw < 0.2F || !player.actionManager.hasAttackedSince(350)) {
            duplicateStreak = 0;
            lastYawDelta = absYaw;
            lastPitchDelta = absPitch;
            return;
        }

        if (Math.abs(absYaw - lastYawDelta) < 1.0E-4F && Math.abs(absPitch - lastPitchDelta) < 1.0E-4F) {
            if (++duplicateStreak > 10) {
                flagAndAlert("yaw=" + absYaw + ", pitch=" + absPitch + ", streak=" + duplicateStreak);
            }
        } else {
            duplicateStreak = Math.max(0, duplicateStreak - 1);
            reward();
        }

        lastYawDelta = absYaw;
        lastPitchDelta = absPitch;
    }
}
