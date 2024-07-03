package ac.grim.grimac.utils.nmsutil;

import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.util.Vector3d;
import org.bukkit.util.Vector;

public class JumpPower {
    public static void jumpFromGround(GrimPlayer player, Vector vector) {
        float jumpPower = getJumpPower(player);

        if (player.compensatedEntities.getJumpAmplifier() != null) {
            jumpPower += 0.1f * (player.compensatedEntities.getJumpAmplifier() + 1);
        }

        if (player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_20_5) && jumpPower <= 1.0E-5F) return;

        vector.setY(jumpPower);

        if (player.isSprinting) {
            float radRotation = player.xRot * ((float) Math.PI / 180F);
            vector.add(new Vector(-player.trigHandler.sin(radRotation) * 0.2f, 0.0, player.trigHandler.cos(radRotation) * 0.2f));
        }
    }

    public static float getJumpPower(GrimPlayer player) {
        return player.compensatedEntities.getSelf().getJumpStrength() * getPlayerJumpFactor(player);
    }

    public static float getPlayerJumpFactor(GrimPlayer player) {
        return BlockProperties.onHoneyBlock(player, player.mainSupportingBlockData, new Vector3d(player.lastX, player.lastY, player.lastZ)) ? 0.5f : 1f;
    }
}
