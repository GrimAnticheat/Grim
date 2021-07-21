package ac.grim.grimac.utils.data;

import io.github.retrooper.packetevents.utils.player.Hand;

public class ServerToClientEating {
    public int transaction;
    public boolean isEating;
    public Hand hand;

    public ServerToClientEating(int transaction, boolean isEating, boolean hand) {
        this.transaction = transaction;
        this.isEating = isEating;
        this.hand = hand ? Hand.MAIN_HAND : Hand.OFF_HAND;
    }
}
