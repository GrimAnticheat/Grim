package ac.grim.grimac.checks.impl.aim;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.RotationCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.RotationUpdate;

@CheckData(name = "AimC", description = "Overly perfect sensitivity quantization", decay = 0.01, experimental = true)
public class AimC extends Check implements RotationCheck {
    private int samples;
    private int suspicious;

    public AimC(GrimPlayer player) {
        super(player);
    }

    @Override
    public void process(RotationUpdate rotationUpdate) {
        if (player.packetStateData.lastPacketWasTeleport || player.inVehicle()) {
            samples = 0;
            suspicious = 0;
            return;
        }

        final double modeX = rotationUpdate.getProcessor().modeX;
        final double modeY = rotationUpdate.getProcessor().modeY;
        final float absYaw = rotationUpdate.getDeltaXRotABS();
        final float absPitch = rotationUpdate.getDeltaYRotABS();
        if (modeX <= 0 || modeY <= 0 || absYaw < 0.25F || absPitch < 0.05F || !player.actionManager.hasAttackedSince(400)) {
            return;
        }

        final double yawResidue = Math.abs((absYaw / modeX) - Math.rint(absYaw / modeX));
        final double pitchResidue = Math.abs((absPitch / modeY) - Math.rint(absPitch / modeY));

        samples++;
        if (yawResidue < 1.0E-4D && pitchResidue < 1.0E-4D) {
            suspicious++;
        }

        if (samples >= 40) {
            if (suspicious > 34) {
                flagAndAlert("perfect=" + suspicious + "/" + samples);
            } else {
                reward();
            }
            samples = 0;
            suspicious = 0;
        }
    }
}
