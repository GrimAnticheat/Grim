package ac.grim.grimac.utils.data.interpolation;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.data.packetentity.PacketEntity;
import ac.grim.grimac.utils.data.packetentity.PacketEntityTrackXRot;
import com.github.retrooper.packetevents.protocol.vector.positionpath.PositionPath;

public final class EntityMovementTransaction implements Runnable {

    private final GrimPlayer player;
    private final int entityId;
    private final PositionPath path;
    private final Float yaw;
    private final Float pitch;
    private final boolean relative;
    private final boolean hasPos;
    private final double x, y, z;
    private final boolean positionSync;
    private final int transaction;
    private boolean started;

    public EntityMovementTransaction(GrimPlayer player, int entityId, PositionPath path, boolean relative, boolean hasPos,
                                     double x, double y, double z, Float yaw, Float pitch,
                                     boolean positionSync, int transaction) {
        this.player = player;
        this.entityId = entityId;
        this.path = path;
        this.yaw = yaw;
        this.pitch = pitch;
        this.relative = relative;
        this.hasPos = hasPos;
        this.x = x;
        this.y = y;
        this.z = z;
        this.positionSync = positionSync;
        this.transaction = transaction;
    }

    @Override
    public void run() {
        PacketEntity receiver = player.compensatedEntities.getEntity(entityId);
        if (started) {
            if (receiver != null) receiver.onSecondTransaction(transaction);
            return;
        }

        started = true;
        if (receiver == null) {
            return;
        }

        if (receiver instanceof PacketEntityTrackXRot rotation && yaw != null) {
            rotation.packetYaw = yaw;
            rotation.steps = receiver.isBoat ? 10 : 3;
        }

        receiver.onFirstTransaction(relative, hasPos, x, y, z, yaw, pitch, player, path, positionSync, transaction);
    }
}
