package ac.grim.grimac.predictionengine.predictions.input.impl;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.predictionengine.predictions.input.DoubleInput;
import ac.grim.grimac.utils.math.GrimMath;
import ac.grim.grimac.utils.math.Vec2;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.component.ComponentTypes;
import com.github.retrooper.packetevents.protocol.component.builtin.item.ItemUseEffects;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;

public class ModernInputTransformer extends DoubleInputTransformer { // 1.21.5+
    @Override
    public DoubleInput transformInputsToVector(GrimPlayer player, int sideways, int vertical, int forward) {
        Vec2 moveVector = new Vec2(sideways, forward).normalized();
        Vec2 input = modifyInput(player, moveVector);
        return new DoubleInput(input.x(), 0, input.y());
    }

    public Vec2 modifyInput(GrimPlayer player, Vec2 moveVector) {
        if (moveVector.lengthSquared() == 0.0F) {
            return moveVector;
        } else {
            Vec2 input = moveVector.scale(0.98F);
            if (player.packetStateData.isSlowedByUsingItem() && !player.inVehicle()) {
                input = input.scale(getItemUseSpeedMultiplier(player));
            }

            if (player.isSlowMovement) {
                input = input.scale(player.sneakingSpeedMultiplier);
            }

            return modifyInputSpeedForSquareMovement(input);
        }
    }

    private Vec2 modifyInputSpeedForSquareMovement(Vec2 input) {
        float length = input.length();
        if (length <= 0.0F) {
            return input;
        } else {
            Vec2 multiplied = input.scale(1.0F / length);
            float distance = distanceToUnitSquare(multiplied);
            float min = Math.min(length * distance, 1.0F);
            return multiplied.scale(min);
        }
    }

    private float distanceToUnitSquare(Vec2 input) {
        float x = Math.abs(input.x());
        float z = Math.abs(input.y());
        float additional = z > x ? x / z : z / x;
        return GrimMath.sqrt(1.0F + GrimMath.square(additional));
    }

    private static final boolean USE_EFFECTS_COMPONENT_EXISTS = PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_21_11);

    private float getItemUseSpeedMultiplier(GrimPlayer player) {
        if (player.getClientVersion().isOlderThan(ClientVersion.V_1_21_11) || !USE_EFFECTS_COMPONENT_EXISTS)
            return 0.2F;

        ItemStack itemInHand = player.inventory.getItemInHand(player.packetStateData.itemInUseHand);
        ItemUseEffects useEffects = itemInHand.getComponentOr(ComponentTypes.USE_EFFECTS, null);
        return useEffects == null ? 0.2F : useEffects.getSpeedMultiplier();
    }

}
