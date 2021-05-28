package ac.grim.grimac.predictionengine.movementTick;

import ac.grim.grimac.player.GrimPlayer;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Strider;
import org.bukkit.util.Vector;

public class MovementTickerStrider extends MovementTickerRideable {
    public MovementTickerStrider(GrimPlayer player) {
        super(player);

        movementInput = new Vector(0, 0, 1);

    }

    public float getSteeringSpeed() {
        Strider strider = (Strider) player.playerVehicle;

        // TODO: Lag compensate/listen to packets for suffocating.
        return (float) strider.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).getValue() * (strider.isShivering() ? 0.23F : 0.55F); // shivering -> suffocating
    }

    @Override
    public boolean canStandOnLava() {
        return true;
    }
}
