package ac.grim.grimac.predictionengine.movementTick;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.collisions.datatypes.HexCollisionBox;
import ac.grim.grimac.utils.collisions.datatypes.SimpleCollisionBox;
import ac.grim.grimac.utils.data.PredictionData;
import ac.grim.grimac.utils.data.VectorData;
import ac.grim.grimac.utils.data.packetentity.PacketEntityStrider;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;

public class MovementTickerStrider extends MovementTickerRideable {
    SimpleCollisionBox STABLE_SHAPE = new HexCollisionBox(0.0D, 0.0D, 0.0D, 16.0D, 8.0D, 16.0D);

    public MovementTickerStrider(GrimPlayer player) {
        super(player);

        movementInput = new Vector(0, 0, player.speed);
    }

    @Override
    public float getSteeringSpeed() {
        PacketEntityStrider strider = (PacketEntityStrider) player.playerVehicle;

        // Idk why you have to multiply by 10... I blame bukkit.
        return (float) PredictionData.getMovementSpeedAttribute((LivingEntity) strider.entity) * 10 * (strider.isShaking ? 0.23F : 0.55F);
    }

    private void floatStrider() {
        if (player.wasTouchingLava) {
            if (isAbove(STABLE_SHAPE) && player.compensatedWorld.getFluidLevelAt(player.x, player.y + 1, player.z) == 0) {
                player.lastOnGround = true;
                player.uncertaintyHandler.striderOnGround = true;
                // This is a hack because I believe there is something wrong with order of collision stuff.
                // that doesn't affect players but does affect things that artificially change onGround status
                player.clientVelocity.setY(0);
            } else {
                player.clientVelocity.multiply(0.5).add(new Vector(0, 0.05, 0));
                player.uncertaintyHandler.striderOnGround = false;
            }
        }
    }

    @Override
    public void livingEntityTravel() {
        super.livingEntityTravel();

        floatStrider();
    }

    public boolean isAbove(SimpleCollisionBox box) {
        return player.lastY > (int)player.lastY + box.maxY - (double)1.0E-5F;
    }

    @Override
    public boolean canStandOnLava() {
        return true;
    }
}
