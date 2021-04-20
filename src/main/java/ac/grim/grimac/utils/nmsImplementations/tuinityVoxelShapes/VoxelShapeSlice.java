package ac.grim.grimac.utils.nmsImplementations.tuinityVoxelShapes;

import it.unimi.dsi.fastutil.doubles.DoubleList;
import net.minecraft.server.v1_16_R3.EnumDirection;

public class VoxelShapeSlice extends VoxelShape {
    private static final DoubleList d = new VoxelShapeCubePoint(1);
    private final VoxelShape b;
    private final EnumDirection.EnumAxis c;

    public VoxelShapeSlice(VoxelShape var0, EnumDirection.EnumAxis var1, int var2) {
        super(a(var0.a, var1, var2));
        this.b = var0;
        this.c = var1;
    }

    private static VoxelShapeDiscrete a(VoxelShapeDiscrete var0, EnumDirection.EnumAxis var1, int var2) {
        return new VoxelShapeDiscreteSlice(var0, var1.a(var2, 0, 0), var1.a(0, var2, 0), var1.a(0, 0, var2), var1.a(var2 + 1, var0.a, var0.a), var1.a(var0.b, var2 + 1, var0.b), var1.a(var0.c, var0.c, var2 + 1));
    }

    protected DoubleList a(EnumDirection.EnumAxis var0) {
        return var0 == this.c ? d : this.b.a(var0);
    }
}