package ac.grim.grimac.utils.nmsutil;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.math.GrimMath;
import ac.grim.grimac.utils.math.Vector3dm;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.item.enchantment.type.EnchantmentTypes;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;

public class Riptide {
    public static Vector3dm getRiptideVelocity(GrimPlayer player) {
        ItemStack main = player.getInventory().getHeldItem();
        ItemStack off = player.getInventory().getOffHand();

        int j;
        if (main.getType() == ItemTypes.TRIDENT) {
            j = main.getEnchantmentLevel(EnchantmentTypes.RIPTIDE, PacketEvents.getAPI().getServerManager().getVersion().toClientVersion());
        } else if (off.getType() == ItemTypes.TRIDENT) {
            j = off.getEnchantmentLevel(EnchantmentTypes.RIPTIDE, PacketEvents.getAPI().getServerManager().getVersion().toClientVersion());
        } else {
            return new Vector3dm(); // Can't riptide
        }

        float f7 = player.xRot;
        float f = player.yRot;
        float f1 = -player.trigHandler.sin(GrimMath.radians(f7)) * player.trigHandler.cos(GrimMath.radians(f));
        float f2 = -player.trigHandler.sin(GrimMath.radians(f));
        float f3 = player.trigHandler.cos(GrimMath.radians(f7)) * player.trigHandler.cos(GrimMath.radians(f));
        float f4 = (float) Math.sqrt(f1 * f1 + f2 * f2 + f3 * f3);
        float f5 = 3f * ((1f + j) / 4f);
        f1 = f1 * (f5 / f4);
        f2 = f2 * (f5 / f4);
        f3 = f3 * (f5 / f4);

        // If the player collided vertically with the 1.199999F pushing movement, then the Y additional movement was added
        // (We switched the order around as our prediction engine isn't designed for the proper implementation)
        if (player.verticalCollision) return new Vector3dm(f1, 0, f3);

        return new Vector3dm(f1, f2, f3);
    }
}
