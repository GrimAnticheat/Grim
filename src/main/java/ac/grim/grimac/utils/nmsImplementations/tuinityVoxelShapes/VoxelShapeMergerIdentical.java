package ac.grim.grimac.utils.nmsImplementations.tuinityVoxelShapes;

import it.unimi.dsi.fastutil.doubles.DoubleList;

public class VoxelShapeMergerIdentical implements VoxelShapeMerger {
    private final DoubleList a;

    public VoxelShapeMergerIdentical(DoubleList var0) {
        this.a = var0;
    }

    public boolean a(a var0) {
        for (int var1 = 0; var1 <= this.a.size(); ++var1) {
            if (!var0.merge(var1, var1, var1)) {
                return false;
            }
        }

        return true;
    }

    public DoubleList a() {
        return this.a;
    }
}
