package ac.grim.grimac.utils.data;

import ac.grim.grimac.player.GrimPlayer;

public class FireworkData {
    public long creationTime;
    public long destroyTime = Long.MAX_VALUE;
    GrimPlayer grimPlayer;

    public FireworkData(GrimPlayer grimPlayer) {
        this.grimPlayer = grimPlayer;
        this.creationTime = grimPlayer.lastTransactionReceived;
    }

    public void setDestroyed() {
        this.destroyTime = grimPlayer.lastTransactionSent.get();
    }
}
