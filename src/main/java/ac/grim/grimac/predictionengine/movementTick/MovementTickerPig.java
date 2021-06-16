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

        movementInput = new Vector(0, 0, 1);
    }

    // Pig and Strider should implement this
    public float getSteeringSpeed() { // Idk why the * 0.225 is needed lmao, send help
        Entity pig = player.playerVehicle.entity;
        return (float) PredictionData.getMovementSpeedAttribute((LivingEntity) pig) * 0.225f;
    }
}
