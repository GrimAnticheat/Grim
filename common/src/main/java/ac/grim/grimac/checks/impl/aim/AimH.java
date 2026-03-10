package ac.grim.grimac.checks.impl.aim;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.RotationCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.RotationUpdate;

@CheckData(name = "AimH", description = "Perfectly static pitch during repeated attacks", decay = 0.02, experimental = true)
public class AimH extends Check implements RotationCheck {
    private float lastPitch;
    private int staticPitchStreak;

    public AimH(GrimPlayer player) {
        super(player);
    }

    @Override
    public void process(RotationUpdate rotationUpdate) {
        if (!player.actionManager.hasAttackedSince(400) || player.packetStateData.lastPacketWasTeleport) {
            staticPitchStreak = 0;
            lastPitch = player.pitch;
            return;
        }

        if (Math.abs(player.pitch - lastPitch) < 1.0E-5F && rotationUpdate.getDeltaXRotABS() > 1.2F) {
            if (++staticPitchStreak > 9) {
                flagAndAlert("streak=" + staticPitchStreak + ", pitch=" + player.pitch);
            }
        } else {
            staticPitchStreak = Math.max(0, staticPitchStreak - 1);
            reward();
        }

        lastPitch = player.pitch;
    }
}
