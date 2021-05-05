package ac.grim.grimac.checks.predictionengine.movementTick;

import ac.grim.grimac.checks.predictionengine.predictions.PredictionEngineNormal;
import ac.grim.grimac.checks.predictionengine.predictions.PredictionEngineWater;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.enums.MoverType;
import ac.grim.grimac.utils.nmsImplementations.BlockProperties;
import org.bukkit.Bukkit;
import org.bukkit.util.Vector;

import static ac.grim.grimac.checks.predictionengine.MovementCheckRunner.getBestContinuousInput;
import static ac.grim.grimac.checks.predictionengine.MovementCheckRunner.getBestTheoreticalPlayerInput;
import static ac.grim.grimac.checks.predictionengine.predictions.PredictionEngine.getMovementResultFromInput;

// Heavily based off of MovementTickerLivingVehicle
public class MovementTickerSlow extends MovementTicker {
    boolean optimisticCrouching;
    Vector wantedMovement;
    Vector theoreticalOutput;

    public MovementTickerSlow(GrimPlayer player, boolean optimisticCrouching, Vector optimisticStuckSpeed, Vector wantedMovement, Vector theoreticalOutput) {
        super(player);
        this.player.stuckSpeedMultiplier = optimisticStuckSpeed;
        this.optimisticCrouching = optimisticCrouching;
        this.wantedMovement = wantedMovement;
        this.theoreticalOutput = theoreticalOutput;
    }

    @Override
    public void doWaterMove(float swimSpeed, boolean isFalling, float swimFriction) {
        Vector movementInput = getBestContinuousInput(player.isCrouching && optimisticCrouching, getBestTheoreticalPlayerInput(wantedMovement.clone().subtract(theoreticalOutput).divide(player.stuckSpeedMultiplier), player.speed, player.xRot));

        Vector movementInputResult = getMovementResultFromInput(movementInput, swimSpeed, player.xRot);
        addAndMove(MoverType.SELF, movementInputResult);

        PredictionEngineWater.staticVectorEndOfTick(player, player.clientVelocity, swimFriction, player.gravity, isFalling);
    }

    @Override
    public void doLavaMove() {
        Vector movementInput = getBestContinuousInput(player.isCrouching && optimisticCrouching, getBestTheoreticalPlayerInput(wantedMovement.clone().subtract(theoreticalOutput).divide(player.stuckSpeedMultiplier), player.speed, player.xRot));
        Vector movementInputResult = getMovementResultFromInput(movementInput, 0.02F, player.xRot);

        Bukkit.broadcastMessage("Movement input " + movementInput);
        Bukkit.broadcastMessage("Movement input result " + movementInputResult);

        addAndMove(MoverType.SELF, movementInputResult);

        // Lava doesn't have an end of tick thing?
        //vectorEndOfTick(grimPlayer, grimPlayer.clientVelocity);
    }

    @Override
    public void doNormalMove(float blockFriction) {
        // We don't know if the horse is on the ground
        player.friction = blockFriction * 0.91f;

        Vector movementInput = getBestContinuousInput(player.isCrouching && optimisticCrouching, getBestTheoreticalPlayerInput(wantedMovement.clone().subtract(theoreticalOutput).divide(player.stuckSpeedMultiplier), player.speed, player.xRot));
        Vector movementInputResult = getMovementResultFromInput(movementInput, BlockProperties.getFrictionInfluencedSpeed(blockFriction, player), player.xRot);

        Bukkit.broadcastMessage("Movement input " + movementInput);
        Bukkit.broadcastMessage("Movement input result " + movementInputResult);

        addAndMove(MoverType.SELF, movementInputResult);

        PredictionEngineNormal.staticVectorEndOfTick(player, player.clientVelocity);
    }

    public void addAndMove(MoverType moverType, Vector movementResult) {
        player.clientVelocity.add(movementResult);
        super.move(moverType, player.clientVelocity.clone());
    }
}
