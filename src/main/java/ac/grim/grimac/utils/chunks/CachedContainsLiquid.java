package ac.grim.grimac.utils.chunks;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.collisions.types.SimpleCollisionBox;

public class CachedContainsLiquid {
    public static boolean containsLiquid(GrimPlayer player, SimpleCollisionBox var0) {
        int var1 = (int) Math.floor(var0.minX);
        int var2 = (int) Math.ceil(var0.maxX);
        int var3 = (int) Math.floor(var0.minY);
        int var4 = (int) Math.ceil(var0.maxY);
        int var5 = (int) Math.floor(var0.minZ);
        int var6 = (int) Math.ceil(var0.maxZ);

        for (int var8 = var1; var8 < var2; ++var8) {
            for (int var9 = var3; var9 < var4; ++var9) {
                for (int var10 = var5; var10 < var6; ++var10) {
                    if (player.compensatedWorld.getFluidLevelAt(var8, var9, var10) > 0) return true;
                }
            }
        }

        return false;
    }
}
