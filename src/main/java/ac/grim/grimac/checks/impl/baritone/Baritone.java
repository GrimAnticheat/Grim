package ac.grim.grimac.checks.impl.baritone;

import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.RotationCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.RotationUpdate;
import ac.grim.grimac.utils.data.HeadRotation;
import ac.grim.grimac.utils.math.GrimMath;

@CheckData(name = "Baritone")
public class Baritone extends RotationCheck {
    public Baritone(GrimPlayer playerData) {
        super(playerData);
    }

    private float lastPitchDifference;

    private int verbose;

    @Override
    public void process(final RotationUpdate rotationUpdate) {
        final HeadRotation from = rotationUpdate.getFrom();
        final HeadRotation to = rotationUpdate.getTo();

        final float deltaPitch = Math.abs(to.getPitch() - from.getPitch());

        if (rotationUpdate.getDeltaYaw() == 0 && deltaPitch != 0.0 && Math.abs(to.getPitch()) != 90.0f) {
            final long gcd = GrimMath.getGcd((long) (deltaPitch * GrimMath.EXPANDER), (long) (this.lastPitchDifference * GrimMath.EXPANDER));

            if (gcd < 131072L) {
                verbose = Math.min(verbose + 1, 20);
                if (verbose > 9) {
                    flagAndAlert("GCD: " + gcd);
                    verbose = 0;
                }
            } else {
                verbose = Math.max(0, verbose - 1);
            }
        }

        this.lastPitchDifference = deltaPitch;
    }
}
