package ac.grim.grimac.checks.movement.movementTick;

import ac.grim.grimac.GrimPlayer;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Strider;

public class MovementTickerStrider extends MovementTickerRideable {
    public MovementTickerStrider(GrimPlayer grimPlayer) {
        super(grimPlayer);
    }

    public float getSteeringSpeed() {
        Strider strider = (Strider) grimPlayer.playerVehicle;

        // TODO: Lag compensate/listen to packets for suffocating.
        return (float) strider.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).getValue() * (strider.isShivering() ? 0.23F : 0.55F); // shivering -> suffocating
    }
}
