package ac.grim.grimac.utils.nmsutil;

import ac.grim.grimac.player.GrimPlayer;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

public class Riptide {
    public static Vector getRiptideVelocity(GrimPlayer player) {
        ItemStack main = player.bukkitPlayer.getInventory().getItemInMainHand();
        ItemStack off = player.bukkitPlayer.getInventory().getItemInOffHand();

        int j;
        if (main.getType() == Material.TRIDENT) {
            j = main.getEnchantmentLevel(Enchantment.RIPTIDE);
        } else if (off.getType() == Material.TRIDENT) {
            j = off.getEnchantmentLevel(Enchantment.RIPTIDE);
        } else {
            return new Vector(); // Can't riptide
        }

        float f7 = player.xRot;
        float f = player.yRot;
        float f1 = -player.trigHandler.sin(f7 * ((float) Math.PI / 180F)) * player.trigHandler.cos(f * ((float) Math.PI / 180F));
        float f2 = -player.trigHandler.sin(f * ((float) Math.PI / 180F));
        float f3 = player.trigHandler.cos(f7 * ((float) Math.PI / 180F)) * player.trigHandler.cos(f * ((float) Math.PI / 180F));
        float f4 = (float) Math.sqrt(f1 * f1 + f2 * f2 + f3 * f3);
        float f5 = 3.0F * ((1.0F + j) / 4.0F);
        f1 = f1 * (f5 / f4);
        f2 = f2 * (f5 / f4);
        f3 = f3 * (f5 / f4);

        // If the player collided vertically with the 1.199999F pushing movement, then the Y additional movement was added
        // (We switched the order around as our prediction engine isn't designed for the proper implementation)
        if (player.verticalCollision) return new Vector(f1, 0, f3);

        return new Vector(f1, f2, f3);
    }
}
