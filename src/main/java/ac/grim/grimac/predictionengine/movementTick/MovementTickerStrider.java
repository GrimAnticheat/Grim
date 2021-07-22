package ac.grim.grimac.predictionengine.movementTick;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.data.packetentity.PacketEntityStrider;
import ac.grim.grimac.utils.enums.Pose;
import ac.grim.grimac.utils.nmsImplementations.BlockProperties;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.util.Vector;

public class MovementTickerStrider extends MovementTickerRideable {

    public MovementTickerStrider(GrimPlayer player) {
        super(player);

        if (player.playerVehicle.pose == Pose.DYING) {
            player.clientVelocity = new Vector();
            return;
        }

        ((PacketEntityStrider) player.playerVehicle).isShaking = true;
        // Blocks are stored in YZX order

        Material posMaterial = player.compensatedWorld.getBukkitMaterialAt(player.x, player.y, player.z);
        Material belowMaterial = BlockProperties.getOnBlock(player, new Location(null, player.x, player.y, player.z));
        ((PacketEntityStrider) player.playerVehicle).isShaking = !Tag.STRIDER_WARM_BLOCKS.isTagged(posMaterial) &&
                !Tag.STRIDER_WARM_BLOCKS.isTagged(belowMaterial) && !player.wasTouchingLava;

        movementInput = new Vector(0, 0, player.speed);
    }

    @Override
    public void setMovementSpeed() {
        player.movementSpeed = 0.1f;
    }

    @Override
    public float getSteeringSpeed() { // Don't question why we have to multiply by 10
        PacketEntityStrider strider = (PacketEntityStrider) player.playerVehicle;
        return strider.movementSpeedAttribute * (strider.isShaking ? 0.23F : 0.55F) * 10f;
    }

    public static void floatStrider(GrimPlayer player) {
        if (player.wasTouchingLava) {
            if (isAbove(player) && player.compensatedWorld.
                    getLavaFluidLevelAt((int) Math.floor(player.lastX), (int) Math.floor(player.lastY + 1), (int) Math.floor(player.lastZ)) == 0) {
                player.uncertaintyHandler.striderOnGround = true;
                // This is a hack because I believe there is something wrong with order of collision stuff.
                // that doesn't affect players but does affect things that artificially change onGround status
                player.clientVelocity.setY(0);
            } else {
                player.clientVelocity.multiply(0.5).add(new Vector(0, 0.05, 0));
                player.uncertaintyHandler.striderOnGround = false;
            }
        } else {
            player.uncertaintyHandler.striderOnGround = false;
        }
    }

    public static boolean isAbove(GrimPlayer player) {
        return player.lastY > Math.floor(player.lastY) + 0.5 - (double) 1.0E-5F;
    }

    @Override
    public boolean canStandOnLava() {
        return true;
    }
}
