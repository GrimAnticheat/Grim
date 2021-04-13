package ac.grim.grimac.utils.data;

import ac.grim.grimac.GrimPlayer;

public class FireworkData {
    public boolean hasApplied = false;
    long creationTime;
    // Set firework to last for 1000 seconds before we know it's actual lifespan
    long destroyTime = System.nanoTime() + 1000000000000L;
    long lifeTime;
    // Set 1000 seconds of ping before we know the actual latency of the player
    long playerPing;

    // TODO: Don't calculate the player's ping for simplicity and to stop hacks that change individual latency settings

    public FireworkData(GrimPlayer grimPlayer) {
        this.creationTime = System.nanoTime();
        this.playerPing = (long) (grimPlayer.getPing() * 1.0E6);
    }

    public void setDestroyed() {
        // Give 80 ms of extra life because of latency
        this.destroyTime = (long) (System.nanoTime() + (80 * 1E6));
        lifeTime = destroyTime - creationTime;
    }

    public void setApplied() {
        this.playerPing = System.nanoTime() - creationTime;
        hasApplied = true;
    }

    public long getLagCompensatedDestruction() {
        return destroyTime + playerPing;
    }
}
