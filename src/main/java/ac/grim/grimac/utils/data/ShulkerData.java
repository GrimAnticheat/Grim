package ac.grim.grimac.utils.data;

import io.github.retrooper.packetevents.utils.vector.Vector3i;

public class ShulkerData {
    public final int lastTransactionSent;
    public final Vector3i position;
    public boolean isClosing = false;

    // Calculate if the player has no-push, and when to end the possibility of applying piston
    public int ticksOfOpeningClosing = 0;

    public ShulkerData(Vector3i position, int lastTransactionSent, boolean isClosing) {
        this.lastTransactionSent = lastTransactionSent;
        this.position = position;
        this.isClosing = isClosing;
    }

    // We don't know when the piston has applied, or what stage of pushing it is on
    // Therefore, we need to use what we have - the number of movement packets.
    // 10 is a very cautious number
    public boolean tickIfGuaranteedFinished() {
        return isClosing && ++ticksOfOpeningClosing >= 15;
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof ShulkerData) {
            return position.equals(((ShulkerData) other).position);
        }

        return false;
    }
}
