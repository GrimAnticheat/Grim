package ac.grim.grimac.predictionengine.movementTick;

import ac.grim.grimac.player.GrimPlayer;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Pig;
import org.bukkit.util.Vector;

public class MovementTickerPig extends MovementTickerRideable {
    public MovementTickerPig(GrimPlayer player) {
        super(player);

        movementInput = new Vector(0, 0, 1);
    }

    // Pig and Strider should implement this
    public float getSteeringSpeed() {
        Pig pig = (Pig) player.playerVehicle;
        return (float) (pig.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).getValue() * 0.225F);
    }
}
