package ac.grim.grimac.predictionengine.movementTick;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.predictionengine.predictions.rideable.PredictionEngineRideableLava;
import ac.grim.grimac.predictionengine.predictions.rideable.PredictionEngineRideableNormal;
import ac.grim.grimac.predictionengine.predictions.rideable.PredictionEngineRideableWater;
import ac.grim.grimac.predictionengine.predictions.rideable.PredictionEngineRideableWaterLegacy;
import ac.grim.grimac.utils.nmsImplementations.BlockProperties;
import io.github.retrooper.packetevents.utils.player.ClientVersion;
import org.bukkit.util.Vector;

public class MovementTickerLivingVehicle extends MovementTicker {
    Vector movementInput = new Vector();

    public MovementTickerLivingVehicle(GrimPlayer player) {
        super(player);
    }

    @Override
    public void doWaterMove(float swimSpeed, boolean isFalling, float swimFriction) {
        if (player.getClientVersion().isNewerThanOrEquals(ClientVersion.v_1_13)) {
            new PredictionEngineRideableWater(movementInput).guessBestMovement(swimSpeed, player, isFalling, player.gravity, swimFriction, player.lastY);
        } else {
            new PredictionEngineRideableWaterLegacy(movementInput).guessBestMovement(swimSpeed, player, isFalling, player.gravity, swimFriction, player.lastY);
        }
    }

    @Override
    public void doLavaMove() {
        new PredictionEngineRideableLava(movementInput).guessBestMovement(0.02F, player);
    }

    @Override
    public void doNormalMove(float blockFriction) {
        new PredictionEngineRideableNormal(movementInput).guessBestMovement(BlockProperties.getFrictionInfluencedSpeed(blockFriction, player), player);
    }
}
