package ac.grim.grimac.utils.nmsutil;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.math.GrimMath;
import ac.grim.grimac.utils.math.Vector3dm;
import com.github.retrooper.packetevents.protocol.attribute.Attributes;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.potion.PotionTypes;
import com.github.retrooper.packetevents.util.Vector3d;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;

import java.util.OptionalInt;

@UtilityClass
public class JumpPower {
    public static void jumpFromGround(@NotNull GrimPlayer player, @NotNull Vector3dm vector) {
        float jumpPower = getJumpPower(player);

        final OptionalInt jumpBoost = player.compensatedEntities.getPotionLevelForPlayer(PotionTypes.JUMP_BOOST);
        if (jumpBoost.isPresent()) {
            jumpPower += 0.1f * (jumpBoost.getAsInt() + 1);
        }

        if (player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_20_5) && jumpPower <= 1.0E-5f)
            return;

        vector.setY(player.getClientVersion().isOlderThan(ClientVersion.V_1_21_2) ? jumpPower : Math.max(jumpPower, vector.getY()));

        if (player.isSprinting) {
            float radRotation = GrimMath.radians(player.yaw);
            if (player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_20_5)) {
                vector.add(-player.trigHandler.sin(radRotation) * 0.2, 0.0, player.trigHandler.cos(radRotation) * 0.2);
            } else {
                vector.add(-player.trigHandler.sin(radRotation) * 0.2F, 0.0, player.trigHandler.cos(radRotation) * 0.2F);
            }
        }
    }

    public static float getJumpPower(@NotNull GrimPlayer player) {
        return (float) player.compensatedEntities.self.getAttributeValue(Attributes.JUMP_STRENGTH) * getPlayerJumpFactor(player);
    }

    public static float getPlayerJumpFactor(@NotNull GrimPlayer player) {
        return BlockProperties.onHoneyBlock(player, player.mainSupportingBlockData, new Vector3d(player.lastX, player.lastY, player.lastZ)) ? 0.5f : 1f;
    }
}
