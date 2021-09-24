package ac.grim.grimac.manager.tick.impl;

import ac.grim.grimac.checks.type.PositionCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.PositionUpdate;
import ac.grim.grimac.utils.data.TransPosData;
import io.github.retrooper.packetevents.utils.vector.Vector3d;

public class PositionTransactionSetter extends PositionCheck {
    public PositionTransactionSetter(GrimPlayer playerData) {
        super(playerData);
    }

    public void onPositionUpdate(final PositionUpdate positionUpdate) {
        if (positionUpdate.isTeleport()) return;
        tick(positionUpdate.getFrom());
    }

    public void tick(Vector3d from) {
        synchronized (player.compensatedWorld.posToTrans) {
            player.compensatedWorld.posToTrans.add(new TransPosData(from.getX(), from.getY(), from.getZ(), player.packetStateData.packetLastTransactionReceived.get()));
        }
    }
}
