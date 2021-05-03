package ac.grim.grimac.utils.nmsImplementations;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.chunks.ChunkCache;
import ac.grim.grimac.utils.math.Mth;
import org.bukkit.util.Vector;

public class JumpPower {
    public static Vector jumpFromGround(GrimPlayer grimPlayer, Vector vector) {
        //Player bukkitPlayer = grimPlayer.bukkitPlayer;

        float f = getJumpPower(grimPlayer);

        if (grimPlayer.jumpAmplifier != 0) {
            f += 0.1f * (grimPlayer.jumpAmplifier + 1);
        }

        vector.setY(f);

        // TODO: Use the stuff from the sprinting packet
        if (grimPlayer.isSprinting) {
            float f2 = grimPlayer.xRot * 0.017453292f;
            vector.add(new Vector(-Mth.sin(f2) * 0.2f, 0.0, Mth.cos(f2) * 0.2f));
        }

        return vector;
    }

    public static float getJumpPower(GrimPlayer player) {
        return 0.42f * getPlayerJumpFactor(player);
    }

    private static float getPlayerJumpFactor(GrimPlayer player) {
        float f = ChunkCache.getBlockDataAt(player.lastX, player.lastY, player.lastZ).getBlock().getJumpFactor();
        float f2 = ChunkCache.getBlockDataAt(player.lastX, player.lastY - 0.5000001, player.lastZ).getBlock().getJumpFactor();

        return (double) f == 1.0 ? f2 : f;
    }
}
