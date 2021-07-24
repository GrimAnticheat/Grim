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

        movementInput = new Vector(0, 0, 1);
    }

    public static void floatStrider(GrimPlayer player) {
        if (player.wasTouchingLava) {
            if (isAbove(player) && player.compensatedWorld.
                    getLavaFluidLevelAt((int) Math.floor(player.lastX), (int) Math.floor(player.lastY + 1), (int) Math.floor(player.lastZ)) == 0) {
                player.lastOnGround = true;
            } else {
                player.clientVelocity.multiply(0.5).add(new Vector(0, 0.05, 0));
            }
        }
    }

    public static boolean isAbove(GrimPlayer player) {
        return player.lastY > Math.floor(player.lastY) + 0.5 - (double) 1.0E-5F;
    }

    @Override
    public void livingEntityAIStep() {
        super.livingEntityAIStep();

        ((PacketEntityStrider) player.playerVehicle).isShaking = true;

        Material posMaterial = player.compensatedWorld.getBukkitMaterialAt(player.x, player.y, player.z);
        Material belowMaterial = BlockProperties.getOnBlock(player, new Location(null, player.x, player.y, player.z));
        ((PacketEntityStrider) player.playerVehicle).isShaking = !Tag.STRIDER_WARM_BLOCKS.isTagged(posMaterial) &&
                !Tag.STRIDER_WARM_BLOCKS.isTagged(belowMaterial) && !player.wasTouchingLava;
    }

    @Override
    public float getSteeringSpeed() {
        PacketEntityStrider strider = (PacketEntityStrider) player.playerVehicle;
        return strider.movementSpeedAttribute * (strider.isShaking ? 0.23F : 0.55F);
    }

    @Override
    public boolean canStandOnLava() {
        return true;
    }
}
