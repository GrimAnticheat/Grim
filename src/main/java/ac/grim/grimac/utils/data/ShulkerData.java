package ac.grim.grimac.utils.data;

import ac.grim.grimac.utils.data.packetentity.PacketEntity;
import io.github.retrooper.packetevents.utils.vector.Vector3i;

public class ShulkerData {
    public final int lastTransactionSent;
    public final Vector3i position;
    public boolean isClosing;
    public PacketEntity entity;

    // Calculate if the player has no-push, and when to end the possibility of applying piston
    public int ticksOfOpeningClosing = 0;

    public ShulkerData(Vector3i position, int lastTransactionSent, boolean isClosing) {
        this.lastTransactionSent = lastTransactionSent;
        this.position = position;
        this.isClosing = isClosing;
    }

    public ShulkerData(PacketEntity entity, int lastTransactionSent, boolean isClosing) {
        this.lastTransactionSent = lastTransactionSent;
        this.position = new Vector3i((int) Math.floor(entity.position.getX()), (int) Math.floor(entity.position.getY()), (int) Math.floor(entity.position.getZ()));
        this.isClosing = isClosing;
        this.entity = entity;
    }

    // We don't know when the piston has applied, or what stage of pushing it is on
    // Therefore, we need to use what we have - the number of movement packets.
    // 25 is a very cautious number beyond
    public boolean tickIfGuaranteedFinished() {
        return isClosing && ++ticksOfOpeningClosing >= 25;
    }
}
