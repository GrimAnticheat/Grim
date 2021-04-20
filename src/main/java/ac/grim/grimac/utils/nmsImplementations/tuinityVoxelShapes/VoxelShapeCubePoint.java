package ac.grim.grimac.utils.nmsImplementations.tuinityVoxelShapes;

import it.unimi.dsi.fastutil.doubles.AbstractDoubleList;

public class VoxelShapeCubePoint extends AbstractDoubleList {
    private final int a;

    VoxelShapeCubePoint(int var0) {
        this.a = var0;
    }

    public double getDouble(int var0) {
        return (double) var0 / (double) this.a;
    }

    public int size() {
        return this.a + 1;
    }
}

