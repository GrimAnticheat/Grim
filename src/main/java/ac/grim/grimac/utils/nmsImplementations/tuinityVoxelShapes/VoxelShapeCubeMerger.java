package ac.grim.grimac.utils.nmsImplementations.tuinityVoxelShapes;

import com.google.common.math.IntMath;
import it.unimi.dsi.fastutil.doubles.DoubleList;

public final class VoxelShapeCubeMerger implements VoxelShapeMerger {
    private final VoxelShapeCubePoint a;
    private final int b;
    private final int c;
    private final int d;

    VoxelShapeCubeMerger(int var0, int var1) {
        this.a = new VoxelShapeCubePoint((int) VoxelShapes.a(var0, var1));
        this.b = var0;
        this.c = var1;
        this.d = IntMath.gcd(var0, var1);
    }

    public boolean a(a var0) {
        int var1 = this.b / this.d;
        int var2 = this.c / this.d;

        for (int var3 = 0; var3 <= this.a.size(); ++var3) {
            if (!var0.merge(var3 / var2, var3 / var1, var3)) {
                return false;
            }
        }

        return true;
    }

    public DoubleList a() {
        return this.a;
    }
}

