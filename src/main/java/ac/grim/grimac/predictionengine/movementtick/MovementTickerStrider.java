package ac.grim.grimac.predictionengine.movementtick;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.data.packetentity.PacketEntityStrider;
import ac.grim.grimac.utils.nmsutil.BlockProperties;
import com.github.retrooper.packetevents.protocol.world.states.defaulttags.BlockTags;
import com.github.retrooper.packetevents.protocol.world.states.type.StateType;
import org.bukkit.util.Vector;

public class MovementTickerStrider extends MovementTickerRideable {

    public MovementTickerStrider(GrimPlayer player) {
        super(player);
        movementInput = new Vector(0, 0, 1);
    }

    public static void floatStrider(GrimPlayer player) {
        if (player.wasTouchingLava) {
            if (isAbove(player) && player.getCompensatedWorld().getLavaFluidLevelAt((int) Math.floor(player.x), (int) Math.floor(player.y + 1), (int) Math.floor(player.z)) == 0) {
                player.onGround = true;
            } else {
                player.clientVelocity.multiply(0.5).add(new Vector(0, 0.05, 0));
            }
        }
    }

    public static boolean isAbove(GrimPlayer player) {
        return player.y > Math.floor(player.y) + 0.5 - 1.0E-5F;
    }

    @Override
    public void livingEntityAIStep() {
        super.livingEntityAIStep();

        ((PacketEntityStrider) player.getCompensatedEntities().getSelf().getRiding()).isShaking = true;

        StateType posMaterial = player.getCompensatedWorld().getStateTypeAt(player.x, player.y, player.z);
        StateType belowMaterial = BlockProperties.getOnBlock(player, player.x, player.y, player.z);

        ((PacketEntityStrider) player.getCompensatedEntities().getSelf().getRiding()).isShaking =
                !BlockTags.STRIDER_WARM_BLOCKS.contains(posMaterial) &&
                        !BlockTags.STRIDER_WARM_BLOCKS.contains(belowMaterial) &&
                        !player.wasTouchingLava;
    }

    @Override
    public float getSteeringSpeed() {
        PacketEntityStrider strider = (PacketEntityStrider) player.getCompensatedEntities().getSelf().getRiding();
        return strider.movementSpeedAttribute * (strider.isShaking ? 0.23F : 0.55F);
    }

    @Override
    public boolean canStandOnLava() {
        return true;
    }
}
