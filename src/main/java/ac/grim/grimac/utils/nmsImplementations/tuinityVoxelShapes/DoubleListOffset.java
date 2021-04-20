package ac.grim.grimac.utils.nmsImplementations.tuinityVoxelShapes;

import it.unimi.dsi.fastutil.doubles.AbstractDoubleList;
import it.unimi.dsi.fastutil.doubles.DoubleList;

public class DoubleListOffset extends AbstractDoubleList {
    private final DoubleList a;
    private final double b;

    public DoubleListOffset(DoubleList var0, double var1) {
        this.a = var0;
        this.b = var1;
    }

    public double getDouble(int var0) {
        return this.a.getDouble(var0) + this.b;
    }

    public int size() {
        return this.a.size();
    }
}
