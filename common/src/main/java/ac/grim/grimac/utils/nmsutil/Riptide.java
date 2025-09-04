package ac.grim.grimac.utils.nmsutil;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.math.GrimMath;
import ac.grim.grimac.utils.math.Vector3dm;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.item.enchantment.type.EnchantmentTypes;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import lombok.experimental.UtilityClass;

@UtilityClass
public class Riptide {
    public static Vector3dm getRiptideVelocity(GrimPlayer player) {
        ItemStack main = player.inventory.getHeldItem();
        ItemStack off = player.inventory.getOffHand();

        final int j;
        if (main.getType() == ItemTypes.TRIDENT) {
            j = main.getEnchantmentLevel(EnchantmentTypes.RIPTIDE);
        } else if (off.getType() == ItemTypes.TRIDENT) {
            j = off.getEnchantmentLevel(EnchantmentTypes.RIPTIDE);
        } else {
            return new Vector3dm(); // Can't riptide
        }

        float yaw = GrimMath.radians(player.xRot);
        float pitch = GrimMath.radians(player.yRot);
        float pitchCos = player.trigHandler.cos(pitch);
        float f1 = -player.trigHandler.sin(yaw) * pitchCos;
        float f2 = -player.trigHandler.sin(pitch);
        float f3 = player.trigHandler.cos(yaw) * pitchCos;
        float f4 = (float) Math.sqrt(f1 * f1 + f2 * f2 + f3 * f3);
        float f5 = (3f * ((1f + j) / 4f)) / f4;

        // If the player collided vertically with the 1.199999F pushing movement, then the Y additional movement was added
        // (We switched the order around as our prediction engine isn't designed for the proper implementation)
        return new Vector3dm(f1 * f5, player.verticalCollision ? 0 : f2 * f5, f3 * f5);
    }
}
