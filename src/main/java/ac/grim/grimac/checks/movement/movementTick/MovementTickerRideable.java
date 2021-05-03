package ac.grim.grimac.checks.movement.movementTick;

import ac.grim.grimac.player.GrimPlayer;

public class MovementTickerRideable extends MovementTickerLivingVehicle {

    public MovementTickerRideable(GrimPlayer grimPlayer) {
        super(grimPlayer);

        // If the player has carrot/fungus on a stick, otherwise the player has no control
        float f = getSteeringSpeed();

        // Do stuff for boosting on a pig

        grimPlayer.speed = f;
    }

    // Pig and Strider should implement this
    public float getSteeringSpeed() {
        return -1f;
    }


}
