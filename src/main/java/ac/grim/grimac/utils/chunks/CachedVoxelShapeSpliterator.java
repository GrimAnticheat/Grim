package ac.grim.grimac.utils.chunks;

import net.minecraft.server.v1_16_R3.*;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.Spliterators;
import java.util.function.BiPredicate;
import java.util.function.Consumer;

public class CachedVoxelShapeSpliterator extends Spliterators.AbstractSpliterator<VoxelShape> {
    @Nullable
    private final Entity a;
    private final AxisAlignedBB b;
    private final VoxelShapeCollision c;
    private final CursorPosition d;
    private final BlockPosition.MutableBlockPosition e;
    private final VoxelShape f;
    private final ICollisionAccess g;
    private final BiPredicate<IBlockData, BlockPosition> i;
    private boolean h;

    public CachedVoxelShapeSpliterator(ICollisionAccess var0, @Nullable Entity var1, AxisAlignedBB var2) {
        this(var0, var1, var2, (var0x, var1x) -> {
            return true;
        });
    }

    public CachedVoxelShapeSpliterator(ICollisionAccess var0, @Nullable Entity var1, AxisAlignedBB var2, BiPredicate<IBlockData, BlockPosition> var3) {
        super(9223372036854775807L, 1280);
        this.c = var1 == null ? VoxelShapeCollision.a() : VoxelShapeCollision.a(var1);
        this.e = new BlockPosition.MutableBlockPosition();
        this.f = VoxelShapes.a(var2);
        this.g = var0;
        this.h = var1 != null;
        this.a = var1;
        this.b = var2;
        this.i = var3;
        int var4 = MathHelper.floor(var2.minX - 1.0E-7D) - 1;
        int var5 = MathHelper.floor(var2.maxX + 1.0E-7D) + 1;
        int var6 = MathHelper.floor(var2.minY - 1.0E-7D) - 1;
        int var7 = MathHelper.floor(var2.maxY + 1.0E-7D) + 1;
        int var8 = MathHelper.floor(var2.minZ - 1.0E-7D) - 1;
        int var9 = MathHelper.floor(var2.maxZ + 1.0E-7D) + 1;
        this.d = new CursorPosition(var4, var6, var8, var5, var7, var9);
    }

    public boolean tryAdvance(Consumer<? super VoxelShape> var0) {
        return this.h && this.b(var0) || this.a(var0);
    }

    boolean b(Consumer<? super VoxelShape> var0) {
        Objects.requireNonNull(this.a);
        this.h = false;
        WorldBorder var1 = this.g.getWorldBorder();
        AxisAlignedBB var2 = this.a.getBoundingBox();
        if (!a(var1, var2)) {
            VoxelShape var3 = var1.c();
            if (!b(var3, var2) && a(var3, var2)) {
                var0.accept(var3);
                return true;
            }
        }

        return false;
    }

    boolean a(Consumer<? super VoxelShape> var0) {
        while (true) {
            if (this.d.a()) {
                int var1 = this.d.b();
                int var2 = this.d.c();
                int var3 = this.d.d();
                int var4 = this.d.e();
                if (var4 == 3) {
                    continue;
                }

                this.e.d(var1, var2, var3);
                IBlockData var6 = ChunkCache.getBlockDataAt(e.getX(), e.getY(), e.getZ());
                if (!this.i.test(var6, this.e) || var4 == 1 && !var6.d() || var4 == 2 && !var6.a(Blocks.MOVING_PISTON)) {
                    continue;
                }

                VoxelShape var7 = var6.b(this.g, this.e, this.c);
                if (var7 == VoxelShapes.b()) {
                    if (!this.b.a(var1, var2, var3, (double) var1 + 1.0D, (double) var2 + 1.0D, (double) var3 + 1.0D)) {
                        continue;
                    }

                    var0.accept(var7.a(var1, var2, var3));
                    return true;
                }

                VoxelShape var8 = var7.a(var1, var2, var3);
                if (!VoxelShapes.c(var8, this.f, OperatorBoolean.AND)) {
                    continue;
                }

                var0.accept(var8);
                return true;
            }

            return false;
        }
    }

    public static boolean a(WorldBorder var0, AxisAlignedBB var1) {
        double var2 = MathHelper.floor(var0.e());
        double var4 = MathHelper.floor(var0.f());
        double var6 = MathHelper.f(var0.g());
        double var8 = MathHelper.f(var0.h());
        return var1.minX > var2 && var1.minX < var6 && var1.minZ > var4 && var1.minZ < var8 && var1.maxX > var2 && var1.maxX < var6 && var1.maxZ > var4 && var1.maxZ < var8;
    }

    private static boolean b(VoxelShape var0, AxisAlignedBB var1) {
        return VoxelShapes.c(var0, VoxelShapes.a(var1.shrink(1.0E-7D)), OperatorBoolean.AND);
    }

    private static boolean a(VoxelShape var0, AxisAlignedBB var1) {
        return VoxelShapes.c(var0, VoxelShapes.a(var1.g(1.0E-7D)), OperatorBoolean.AND);
    }

    /*@Nullable
    private IBlockAccess a(int var0, int var1) {
        int var2 = var0 >> 4;
        int var3 = var1 >> 4;
        return this.g.c(var2, var3);
    }*/
}
