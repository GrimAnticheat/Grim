package ac.grim.grimac.checks.impl.prediction;

import ac.grim.grimac.checks.type.PostPredictionCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.PredictionComplete;

public class NoFallB extends PostPredictionCheck {

    public NoFallB(GrimPlayer player) {
        super(player);
    }

    public void onPredictionComplete(final PredictionComplete predictionComplete) {
        // Exemptions
        if (player.inVehicle || player.wasTouchingWater || player.wasTouchingLava
                || player.uncertaintyHandler.pistonX != 0 || player.uncertaintyHandler.pistonY != 0
                || player.uncertaintyHandler.pistonZ != 0 || player.uncertaintyHandler.isSteppingOnSlime
                || player.isFlying || player.uncertaintyHandler.isStepMovement
                || player.uncertaintyHandler.lastTeleportTicks > -3 || player.isDead
                || player.isInBed || player.lastInBed) return;

        boolean invalid = player.clientClaimsLastOnGround != player.onGround;

        if (invalid) {
            // TODO: Punish!
        }
    }
}
