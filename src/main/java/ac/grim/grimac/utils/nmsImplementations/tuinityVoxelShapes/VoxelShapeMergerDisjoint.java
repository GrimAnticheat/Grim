package ac.grim.grimac.utils.nmsImplementations.tuinityVoxelShapes;

import it.unimi.dsi.fastutil.doubles.AbstractDoubleList;
import it.unimi.dsi.fastutil.doubles.DoubleList;

public class VoxelShapeMergerDisjoint extends AbstractDoubleList implements VoxelShapeMerger {
    private final DoubleList a;
    private final DoubleList b;
    private final boolean c;

    public VoxelShapeMergerDisjoint(DoubleList var0, DoubleList var1, boolean var2) {
        this.a = var0;
        this.b = var1;
        this.c = var2;
    }

    public int size() {
        return this.a.size() + this.b.size();
    }

    public boolean a(a var0) {
        return this.c ? this.b((var1, var2, var3) -> {
            return var0.merge(var2, var1, var3);
        }) : this.b(var0);
    }

    private boolean b(a var0) {
        int var1 = this.a.size() - 1;

        int var2;
        for (var2 = 0; var2 < var1; ++var2) {
            if (!var0.merge(var2, -1, var2)) {
                return false;
            }
        }

        if (!var0.merge(var1, -1, var1)) {
            return false;
        } else {
            for (var2 = 0; var2 < this.b.size(); ++var2) {
                if (!var0.merge(var1, var2, var1 + 1 + var2)) {
                    return false;
                }
            }

            return true;
        }
    }

    public double getDouble(int var0) {
        return var0 < this.a.size() ? this.a.getDouble(var0) : this.b.getDouble(var0 - this.a.size());
    }

    public DoubleList a() {
        return this;
    }
}
