package ac.grim.grimac.checks.predictionengine.movementTick;

import ac.grim.grimac.checks.predictionengine.predictions.PredictionEngineNormal;
import ac.grim.grimac.checks.predictionengine.predictions.PredictionEngineWater;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.enums.MoverType;
import org.bukkit.util.Vector;

import static ac.grim.grimac.checks.predictionengine.MovementCheckRunner.getBestContinuousInput;
import static ac.grim.grimac.checks.predictionengine.MovementCheckRunner.getBestTheoreticalPlayerInput;
import static ac.grim.grimac.checks.predictionengine.predictions.PredictionEngine.getMovementResultFromInput;

// Heavily based off of MovementTickerLivingVehicle
public class MovementTickerSlow extends MovementTicker {
    Vector movementInput;

    public MovementTickerSlow(GrimPlayer player, boolean optimisticCrouching, Vector optimisticStuckSpeed, Vector wantedMovement, Vector theoreticalOutput) {
        super(player);

        movementInput = getBestContinuousInput(player.isCrouching && optimisticCrouching, getBestTheoreticalPlayerInput(wantedMovement.clone().subtract(theoreticalOutput).divide(optimisticStuckSpeed), player.speed, player.xRot));
    }

    @Override
    public void doWaterMove(float swimSpeed, boolean isFalling, float swimFriction) {
        Vector movementInputResult = getMovementResultFromInput(movementInput, swimSpeed, grimPlayer.xRot);
        addAndMove(MoverType.SELF, movementInputResult);

        PredictionEngineWater.staticVectorEndOfTick(grimPlayer, grimPlayer.clientVelocity, swimFriction, grimPlayer.gravity, isFalling);
    }

    @Override
    public void doLavaMove() {
        Vector movementInputResult = getMovementResultFromInput(movementInput, 0.02F, grimPlayer.xRot);
        addAndMove(MoverType.SELF, movementInputResult);

        // Lava doesn't have an end of tick thing?
        //vectorEndOfTick(grimPlayer, grimPlayer.clientVelocity);
    }

    @Override
    public void doNormalMove(float blockFriction) {
        // We don't know if the horse is on the ground
        // TODO: Different friction if horse is in the air
        grimPlayer.friction = blockFriction * 0.91f;

        Vector movementInputResult = getMovementResultFromInput(movementInput, grimPlayer.speed, grimPlayer.xRot);

        addAndMove(MoverType.SELF, movementInputResult);

        PredictionEngineNormal.staticVectorEndOfTick(grimPlayer, grimPlayer.clientVelocity);
    }

    public void addAndMove(MoverType moverType, Vector movementResult) {
        grimPlayer.clientVelocity.add(movementResult);
        super.move(moverType, grimPlayer.clientVelocity);
    }
}
