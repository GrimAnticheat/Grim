package ac.grim.grimac.checks.movement.movementTick;

import ac.grim.grimac.GrimPlayer;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Strider;
import org.bukkit.util.Vector;

public class MovementTickerStrider extends MovementTickerRideable {
    public MovementTickerStrider(GrimPlayer grimPlayer) {
        super(grimPlayer);

        movementInput = new Vector(0, 0, 1);

    }

    public float getSteeringSpeed() {
        Strider strider = (Strider) grimPlayer.playerVehicle;

        // TODO: Lag compensate/listen to packets for suffocating.
        return (float) strider.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).getValue() * (strider.isShivering() ? 0.23F : 0.55F); // shivering -> suffocating
    }
}
