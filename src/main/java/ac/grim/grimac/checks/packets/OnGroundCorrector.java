package ac.grim.grimac.checks.packets;

import com.google.common.math.DoubleMath;
import io.github.retrooper.packetevents.packetwrappers.play.in.flying.WrappedPacketInFlying;

public class OnGroundCorrector {
    public static void correctMovement(WrappedPacketInFlying flying, double y) {
        // Shulker boxes false this check without the second thing, with fuzzy equals
        // Example Y axis on ground standing on extending shulker: 73.34999996423721
        // Somewhat hurts the check but it still can catch the majority of simple nofall modules
        if (flying.isOnGround() && y % (1D / 64D) != 0
                && !DoubleMath.fuzzyEquals(y % 0.01, 0, 1e-3)
                && !DoubleMath.fuzzyEquals(y % 0.01, 0.01, 1e-3)) {
            flying.setOnGround(false);
        }
    }
}