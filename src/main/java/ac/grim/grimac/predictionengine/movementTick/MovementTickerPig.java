package ac.grim.grimac.predictionengine.movementTick;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.data.PredictionData;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Pig;
import org.bukkit.util.Vector;

public class MovementTickerPig extends MovementTickerRideable {
    public MovementTickerPig(GrimPlayer player) {
        super(player);
        player.movementSpeed = 0.05625f;

        movementInput = new Vector(0, 0, 1);
    }

    // Pig and Strider should implement this
    public float getSteeringSpeed() {
        Entity pig = player.playerVehicle.entity;
        return (float) PredictionData.getMovementSpeedAttribute((LivingEntity) pig);
    }
}
