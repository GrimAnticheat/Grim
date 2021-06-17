package ac.grim.grimac.utils.data.packetentity.latency;

import ac.grim.grimac.utils.data.packetentity.PacketEntity;

public class EntityRunnable implements Runnable {
    private final PacketEntity entity;

    public EntityRunnable(PacketEntity entity) {
        this.entity = entity;
    }

    @Override
    public void run() {

    }
}
