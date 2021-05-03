package ac.grim.grimac.checks.movement;

import ac.grim.grimac.GrimPlayer;
import ac.grim.grimac.checks.movement.movementTick.MovementVelocityCheckHorse;
import org.bukkit.util.Vector;

public class AbstractHorseMovement {

    // Wow, this is actually really close to the player's movement
    public static void travel(Vector inputMovement, GrimPlayer grimPlayer) {
        new MovementVelocityCheckHorse(grimPlayer).livingEntityTravel();
    }
}
