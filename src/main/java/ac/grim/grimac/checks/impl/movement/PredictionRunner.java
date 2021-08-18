package ac.grim.grimac.checks.impl.movement;

import ac.grim.grimac.checks.type.PositionCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.predictionengine.MovementCheckRunner;
import ac.grim.grimac.utils.anticheat.update.PositionUpdate;
import ac.grim.grimac.utils.data.PredictionData;

public class PredictionRunner extends PositionCheck {
    public PredictionRunner(GrimPlayer playerData) {
        super(playerData);
    }

    @Override
    public void onPositionUpdate(final PositionUpdate positionUpdate) {
        PredictionData data = new PredictionData(player, positionUpdate.getTo().getX(), positionUpdate.getTo().getY(), positionUpdate.getTo().getZ(), player.packetStateData.packetPlayerXRot, player.packetStateData.packetPlayerYRot, positionUpdate.isOnGround(), positionUpdate.isTeleport());
        MovementCheckRunner.processAndCheckMovementPacket(data);
    }
}
