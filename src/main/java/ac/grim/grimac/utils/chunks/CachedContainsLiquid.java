package ac.grim.grimac.utils.chunks;

import net.minecraft.server.v1_16_R3.AxisAlignedBB;
import net.minecraft.server.v1_16_R3.IBlockData;
import net.minecraft.server.v1_16_R3.MathHelper;

public class CachedContainsLiquid {
    public static boolean containsLiquid(AxisAlignedBB var0) {
        int var1 = MathHelper.floor(var0.minX);
        int var2 = MathHelper.f(var0.maxX);
        int var3 = MathHelper.floor(var0.minY);
        int var4 = MathHelper.f(var0.maxY);
        int var5 = MathHelper.floor(var0.minZ);
        int var6 = MathHelper.f(var0.maxZ);

        for (int var8 = var1; var8 < var2; ++var8) {
            for (int var9 = var3; var9 < var4; ++var9) {
                for (int var10 = var5; var10 < var6; ++var10) {
                    IBlockData var11 = ChunkCache.getBlockDataAt(var8, var9, var10);
                    if (!var11.getFluid().isEmpty()) {
                        return true;
                    }
                }
            }
        }

        return false;
    }
}
