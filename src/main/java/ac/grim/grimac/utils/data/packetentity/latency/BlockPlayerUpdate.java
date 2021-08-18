package ac.grim.grimac.utils.data.packetentity.latency;

import ac.grim.grimac.GrimAPI;
import io.github.retrooper.packetevents.utils.vector.Vector3i;

public class BlockPlayerUpdate {
    public Vector3i position;
    public int transaction;
    public int tick;

    public BlockPlayerUpdate(Vector3i position, int transaction) {
        this.position = position;
        this.transaction = transaction;
        this.tick = GrimAPI.INSTANCE.getTickManager().getTick();
    }
}
