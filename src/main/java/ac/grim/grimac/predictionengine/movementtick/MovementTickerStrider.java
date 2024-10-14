package ac.grim.grimac.predictionengine.movementtick;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.data.attribute.ValuedAttribute;
import ac.grim.grimac.utils.data.packetentity.PacketEntityStrider;
import ac.grim.grimac.utils.nmsutil.BlockProperties;
import com.github.retrooper.packetevents.protocol.attribute.Attributes;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.world.states.defaulttags.BlockTags;
import com.github.retrooper.packetevents.protocol.world.states.type.StateType;
import com.github.retrooper.packetevents.resources.ResourceLocation;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerUpdateAttributes;
import org.bukkit.util.Vector;

import java.util.ArrayList;

public class MovementTickerStrider extends MovementTickerRideable {

    public MovementTickerStrider(GrimPlayer player) {
        super(player);
        movementInput = new Vector(0, 0, 1);
    }

    public static void floatStrider(GrimPlayer player) {
        if (player.wasTouchingLava) {
            if (isAbove(player) && player.compensatedWorld.getLavaFluidLevelAt((int) Math.floor(player.x), (int) Math.floor(player.y + 1), (int) Math.floor(player.z)) == 0) {
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

        StateType posMaterial = player.compensatedWorld.getStateTypeAt(player.x, player.y, player.z);
        StateType belowMaterial = BlockProperties.getOnPos(player, player.mainSupportingBlockData, new Vector3d(player.x, player.y, player.z));

        final PacketEntityStrider strider = (PacketEntityStrider) player.compensatedEntities.getSelf().getRiding();
        strider.isShaking = !BlockTags.STRIDER_WARM_BLOCKS.contains(posMaterial) &&
                        !BlockTags.STRIDER_WARM_BLOCKS.contains(belowMaterial) &&
                        !player.wasTouchingLava;
    }

    private static final WrapperPlayServerUpdateAttributes.PropertyModifier SUFFOCATING_MODIFIER = new WrapperPlayServerUpdateAttributes.PropertyModifier(
            ResourceLocation.minecraft("suffocating"), -0.34F, WrapperPlayServerUpdateAttributes.PropertyModifier.Operation.MULTIPLY_BASE);

    @Override
    public float getSteeringSpeed() {
        PacketEntityStrider strider = (PacketEntityStrider) player.compensatedEntities.getSelf().getRiding();
        // Unsure which version the speed changed in
        final boolean newSpeed = player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_20);
        final float coldSpeed = newSpeed ? 0.35F : 0.23F;

        // Client desyncs the attribute
        // Again I don't know when this was changed, or whether it always existed, so I will just put it behind 1.20+
        final ValuedAttribute movementSpeedAttr = strider.getAttribute(Attributes.GENERIC_MOVEMENT_SPEED).get();
        float updatedMovementSpeed = (float) movementSpeedAttr.get();
        if (newSpeed) {
            final WrapperPlayServerUpdateAttributes.Property lastProperty = movementSpeedAttr.property().orElse(null);
            if (lastProperty != null && (!strider.isShaking || lastProperty.getModifiers().stream().noneMatch(mod -> mod.getName().getKey().equals("suffocating")))) {
                WrapperPlayServerUpdateAttributes.Property newProperty = new WrapperPlayServerUpdateAttributes.Property(lastProperty.getAttribute(), lastProperty.getValue(), new ArrayList<>(lastProperty.getModifiers()));
                if (!strider.isShaking) {
                    newProperty.getModifiers().removeIf(modifier -> modifier.getName().getKey().equals("suffocating"));
                } else {
                    newProperty.getModifiers().add(SUFFOCATING_MODIFIER);
                }
                movementSpeedAttr.with(newProperty);
                updatedMovementSpeed = (float) movementSpeedAttr.get();
                movementSpeedAttr.with(lastProperty);
            }
        }

        return updatedMovementSpeed * (strider.isShaking ? coldSpeed : 0.55F);
    }

    @Override
    public boolean canStandOnLava() {
        return true;
    }
}
