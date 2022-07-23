package ac.grim.grimac.checks.impl.baritone;

import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.impl.aim.processor.AimProcessor;
import ac.grim.grimac.checks.type.RotationCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.RotationUpdate;
import ac.grim.grimac.utils.data.HeadRotation;
import ac.grim.grimac.utils.math.GrimMath;

@CheckData(name = "Baritone")
public class Baritone extends RotationCheck {
    private int verbose;

    public Baritone(GrimPlayer playerData) {
        super(playerData);
    }

    @Override
    public void process(final RotationUpdate rotationUpdate) {
        final HeadRotation from = rotationUpdate.getFrom();
        final HeadRotation to = rotationUpdate.getTo();

        final float deltaPitch = Math.abs(to.getPitch() - from.getPitch());
        System.out.println(deltaPitch + " | " + rotationUpdate.getDeltaXRot() + " | " + (Math.abs(to.getPitch()) != 90.0f));
        // Baritone works with small degrees, limit to 1 degrees to pick up on baritone slightly moving aim to bypass anticheats
        // I changed the rotationUpdate.getDeltaXRot() == 0 to <= 0, it may be triggered sometimes but it wont alert
        // I tried baritone, it's never 0, it's always less than 0 (https://prnt.sc/RXxjRA_kca97)
        // - Real6dq
        if (rotationUpdate.getDeltaXRot() <= 0 && deltaPitch > 0 && deltaPitch < 1 && Math.abs(to.getPitch()) != 90.0f) {
            if (rotationUpdate.getProcessor().divisorY < GrimMath.MINIMUM_DIVISOR) {
                verbose++;
                if (verbose > 8) {
                    flagAndAlert("Divisor " + AimProcessor.convertToSensitivity(rotationUpdate.getProcessor().divisorX));
                }
            } else {
                verbose = 0;
            }
        }
    }
}
