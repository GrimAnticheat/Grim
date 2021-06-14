package ac.grim.grimac.utils.data.packetentity.latency;

import ac.grim.grimac.player.GrimPlayer;

public class FireworkData {
    public long creationTime;
    public long destroyTime = Long.MAX_VALUE;
    GrimPlayer player;

    public FireworkData(GrimPlayer player) {
        this.player = player;
        this.creationTime = player.lastTransactionBeforeLastMovement;
    }

    public void setDestroyed() {
        this.destroyTime = player.lastTransactionSent.get();
    }
}
