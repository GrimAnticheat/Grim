package ac.grim.grimac.utils.data;

public class FireworkData {
    public boolean hasApplied = false;
    long creationTime;
    // Set firework to last for 1000 seconds before we know it's actual lifespan
    long destroyTime = System.nanoTime() + 1000000000000L;
    long lifeTime;
    // Set 1000 seconds of ping before we know the actual latency of the player
    long playerPing = 1000000000000L;

    public FireworkData() {
        this.creationTime = System.nanoTime();
    }

    public void setDestroyed() {
        this.destroyTime = System.nanoTime();
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
