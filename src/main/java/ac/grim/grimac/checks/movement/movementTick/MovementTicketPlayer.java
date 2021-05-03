package ac.grim.grimac.checks.movement.movementTick;

import ac.grim.grimac.checks.movement.predictions.PredictionEngineLava;
import ac.grim.grimac.checks.movement.predictions.PredictionEngineNormal;
import ac.grim.grimac.checks.movement.predictions.PredictionEngineWater;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.nmsImplementations.BlockProperties;

public class MovementTicketPlayer extends MovementTicker {
    public MovementTicketPlayer(GrimPlayer grimPlayer) {
        super(grimPlayer);
    }

    public void doWaterMove(float swimSpeed, boolean isFalling, float swimFriction) {
        new PredictionEngineWater().guessBestMovement(swimSpeed, grimPlayer, isFalling, grimPlayer.gravity, swimFriction, grimPlayer.lastY);
    }

    public void doLavaMove() {
        new PredictionEngineLava().guessBestMovement(0.02F, grimPlayer);
    }

    public void doNormalMove(float blockFriction) {
        new PredictionEngineNormal().guessBestMovement(BlockProperties.getFrictionInfluencedSpeed(blockFriction, grimPlayer), grimPlayer);
    }
}
