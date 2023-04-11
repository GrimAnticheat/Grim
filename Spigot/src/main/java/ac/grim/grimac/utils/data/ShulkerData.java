package ac.grim.grimac.utils.data;

import ac.grim.grimac.utils.collisions.datatypes.SimpleCollisionBox;
import ac.grim.grimac.utils.data.packetentity.PacketEntity;
import com.github.retrooper.packetevents.util.Vector3i;

import java.util.Objects;

public class ShulkerData {
    public final int lastTransactionSent;
    private final boolean isClosing;

    // Keep track of one of these two things, so we can remove this later
    public PacketEntity entity = null;
    public Vector3i blockPos = null;

    // Calculate if the player has no-push, and when to end the possibility of applying piston
    private int ticksOfOpeningClosing = 0;

    public ShulkerData(Vector3i position, int lastTransactionSent, boolean isClosing) {
        this.lastTransactionSent = lastTransactionSent;
        this.isClosing = isClosing;
        this.blockPos = position;
    }

    public ShulkerData(PacketEntity entity, int lastTransactionSent, boolean isClosing) {
        this.lastTransactionSent = lastTransactionSent;
        this.isClosing = isClosing;
        this.entity = entity;
    }

    // We don't know when the piston has applied, or what stage of pushing it is on
    // Therefore, we need to use what we have - the number of movement packets.
    // 25 is a very cautious number beyond
    public boolean tickIfGuaranteedFinished() {
        return isClosing && ++ticksOfOpeningClosing >= 25;
    }

    public SimpleCollisionBox getCollision() {
        if (blockPos != null) {
            return new SimpleCollisionBox(blockPos);
        }
        return entity.getPossibleCollisionBoxes();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ShulkerData that = (ShulkerData) o;
        return Objects.equals(entity, that.entity) && Objects.equals(blockPos, that.blockPos);
    }

    @Override
    public int hashCode() {
        return Objects.hash(entity, blockPos);
    }
}
