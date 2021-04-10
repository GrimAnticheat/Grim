package ac.grim.grimac.utils.nmsImplementations;

import ac.grim.grimac.GrimPlayer;
import ac.grim.grimac.utils.chunks.ChunkCache;
import ac.grim.grimac.utils.math.Mth;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

public class JumpPower {
    public static Vector jumpFromGround(GrimPlayer grimPlayer) {
        //Player bukkitPlayer = grimPlayer.bukkitPlayer;

        Vector clonedClientVelocity = grimPlayer.clientVelocity.clone();
        float f = getJumpPower(grimPlayer);

        if (grimPlayer.bukkitPlayer.hasPotionEffect(PotionEffectType.JUMP)) {
            f += 0.1f * (float) (grimPlayer.bukkitPlayer.getPotionEffect(PotionEffectType.JUMP).getAmplifier() + 1);
        }

        clonedClientVelocity.setY(f);

        // TODO: Use the stuff from the sprinting packet
        if (grimPlayer.isSprinting) {
            float f2 = grimPlayer.xRot * 0.017453292f;
            clonedClientVelocity.add(new Vector(-Mth.sin(f2) * 0.2f, 0.0, Mth.cos(f2) * 0.2f));
        }

        return clonedClientVelocity;
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
