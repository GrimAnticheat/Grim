package ac.grim.grimac.utils.nmsutil;

import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.util.Vector3d;
import org.bukkit.util.Vector;

public class JumpPower {
    public static void jumpFromGround(GrimPlayer player, Vector vector) {
        float f = getJumpPower(player);

        if (player.compensatedEntities.getJumpAmplifier() != null) {
            f += 0.1f * (player.compensatedEntities.getJumpAmplifier() + 1);
        }

        vector.setY(f);

        if (player.isSprinting) {
            float f2 = player.xRot * ((float) Math.PI / 180F);
            vector.add(new Vector(-player.trigHandler.sin(f2) * 0.2f, 0.0, player.trigHandler.cos(f2) * 0.2f));
        }
    }

    public static float getJumpPower(GrimPlayer player) {
        return 0.42f * getPlayerJumpFactor(player);
    }

    public static float getPlayerJumpFactor(GrimPlayer player) {
        return BlockProperties.onHoneyBlock(player, player.mainSupportingBlockData, new Vector3d(player.lastX, player.lastY, player.lastZ)) ? 0.5f : 1f;
    }
}
