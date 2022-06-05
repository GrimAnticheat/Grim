package ac.grim.grimac.checks.impl.baritone;

import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.RotationCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.RotationUpdate;
import ac.grim.grimac.utils.data.HeadRotation;
import ac.grim.grimac.utils.math.GrimMath;

@CheckData(name = "AimGCD")
public class AimGCD extends RotationCheck {
    public AimGCD(GrimPlayer playerData) {
        super(playerData);
    }

    private float lastPitchDifference;

    private int verbose;

    @Override
    public void process(final RotationUpdate rotationUpdate) {
        final HeadRotation from = rotationUpdate.getFrom();
        final HeadRotation to = rotationUpdate.getTo();

        final float deltaPitch = Math.abs(to.getPitch() - from.getPitch());

        final long gcd = GrimMath.getGcd((long) (deltaPitch * GrimMath.EXPANDER), (long) (this.lastPitchDifference * GrimMath.EXPANDER));

        // It is hard to use cinematic with delta pitch of 0
        // Plus, baritone often has a pitch of 0, so it's worth the potential falses
        if (rotationUpdate.isCinematic() && rotationUpdate.getDeltaYaw() != 0) {
            if (verbose > 0) verbose -= 7;
        }

        if (to != from && Math.abs(to.getPitch() - from.getPitch()) > 0.0 && Math.abs(to.getPitch()) != 90.0f) {
            if (gcd < 131072L) {
                if (verbose < 20) verbose++;
                if (verbose > 9) {
                    String additional = rotationUpdate.getDeltaYaw() == 0 ? " (Baritone?)" : "";
                    flagAndAlert("GCD: " + gcd + additional);
                    verbose = 0;
                }
            } else {
                if (verbose > 0) verbose--;
            }
        }

        this.lastPitchDifference = deltaPitch;
    }
}
