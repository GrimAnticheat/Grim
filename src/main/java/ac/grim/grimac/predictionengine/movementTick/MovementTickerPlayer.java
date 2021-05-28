package ac.grim.grimac.predictionengine.movementTick;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.predictionengine.predictions.PredictionEngineLava;
import ac.grim.grimac.predictionengine.predictions.PredictionEngineNormal;
import ac.grim.grimac.predictionengine.predictions.PredictionEngineWater;
import ac.grim.grimac.utils.nmsImplementations.BlockProperties;

public class MovementTickerPlayer extends MovementTicker {
    public MovementTickerPlayer(GrimPlayer player) {
        super(player);
    }

    public void doWaterMove(float swimSpeed, boolean isFalling, float swimFriction) {
        new PredictionEngineWater().guessBestMovement(swimSpeed, player, isFalling, player.gravity, swimFriction, player.lastY);
    }

    public void doLavaMove() {
        new PredictionEngineLava().guessBestMovement(0.02F, player);
    }

    public void doNormalMove(float blockFriction) {
        new PredictionEngineNormal().guessBestMovement(BlockProperties.getFrictionInfluencedSpeed(blockFriction, player), player);
    }
}
