package ac.grim.grimac.utils.nmsutil;

import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.world.states.type.StateType;
import com.github.retrooper.packetevents.protocol.world.states.type.StateTypes;
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
        float f = getBlockJumpFactor(player, player.lastX, player.lastY, player.lastZ);
        float f2 = getBlockJumpFactor(player, player.lastX, player.lastY - 0.5000001, player.lastZ);

        return f == 1.0D ? f2 : f;
    }

    private static float getBlockJumpFactor(GrimPlayer player, double x, double y, double z) {
        StateType jumpBlock = player.compensatedWorld.getStateTypeAt(x, y, z);

        if (jumpBlock == StateTypes.HONEY_BLOCK && player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_15))
            return 0.5F;

        return 1.0F;
    }
}
