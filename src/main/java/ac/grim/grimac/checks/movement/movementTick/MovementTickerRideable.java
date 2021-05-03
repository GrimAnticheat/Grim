package ac.grim.grimac.checks.movement.movementTick;

import ac.grim.grimac.GrimPlayer;

public class MovementTickerRideable extends MovementTicker {

    public MovementTickerRideable(GrimPlayer grimPlayer) {
        super(grimPlayer);

        // If the player has carrot/fungus on a stick, otherwise the player has no control
        float f = getSteeringSpeed();


    }

    // Pig and Strider should implement this
    public float getSteeringSpeed() {
        return -1f;
    }


}
