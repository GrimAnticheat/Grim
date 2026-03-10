package ac.grim.grimac.checks.impl.aim;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.RotationCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.RotationUpdate;

@CheckData(name = "AimI", description = "High-precision rounded yaw deltas", decay = 0.02, experimental = true)
public class AimI extends Check implements RotationCheck {
    private int samples;
    private int roundedHits;

    public AimI(GrimPlayer player) {
        super(player);
    }

    @Override
    public void process(RotationUpdate rotationUpdate) {
        float yaw = rotationUpdate.getDeltaXRotABS();
        if (!player.actionManager.hasAttackedSince(400) || player.packetStateData.lastPacketWasTeleport || yaw < 0.25F) {
            return;
        }

        samples++;
        if (Math.abs(yaw * 1000.0F - Math.round(yaw * 1000.0F)) < 1.0E-4F) {
            roundedHits++;
        }

        if (samples >= 60) {
            if (roundedHits > 52) {
                flagAndAlert("rounded=" + roundedHits + "/" + samples);
            } else {
                reward();
            }
            samples = 0;
            roundedHits = 0;
        }
    }
}
