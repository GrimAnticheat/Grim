package ac.grim.grimac.checks.packets;

import io.github.retrooper.packetevents.packetwrappers.play.in.flying.WrappedPacketInFlying;

public class OnGroundCorrector {
    // TODO: Hook up punishments to this, this check is, in my knowledge, 100% reliable
    public static void correctMovement(WrappedPacketInFlying flying, double y) {
        if (flying.isOnGround() && y % (1D / 64D) != 0) {
            flying.setOnGround(false);
        }
    }
}