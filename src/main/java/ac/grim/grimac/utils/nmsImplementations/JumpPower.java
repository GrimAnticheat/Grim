package ac.grim.grimac.utils.nmsImplementations;

import ac.grim.grimac.player.GrimPlayer;
import org.bukkit.Material;
import org.bukkit.util.Vector;

public class JumpPower {
    private static final Material honey = XMaterial.HONEY_BLOCK.parseMaterial();

    public static void jumpFromGround(GrimPlayer player, Vector vector) {
        //Player bukkitPlayer = player.bukkitPlayer;

        float f = getJumpPower(player);

        if (player.jumpAmplifier != 0) {
            f += 0.1f * (player.jumpAmplifier + 1);
        }

        vector.setY(f);

        // TODO: Use the stuff from the sprinting packet
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

        return (double) f == 1.0 ? f2 : f;
    }

    private static float getBlockJumpFactor(GrimPlayer player, Double x, Double y, Double z) {
        Material jumpBlock = player.compensatedWorld.getBukkitMaterialAt(x, y, z);

        if (jumpBlock == honey) return 0.5F;

        return 1.0F;
    }
}
