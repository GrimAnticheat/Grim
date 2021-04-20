package ac.grim.grimac.utils.nmsImplementations.tuinityVoxelShapes;

import it.unimi.dsi.fastutil.doubles.DoubleList;
import net.minecraft.server.v1_16_R3.EnumDirection;
import net.minecraft.server.v1_16_R3.MathHelper;
import net.minecraft.server.v1_16_R3.VoxelShapeDiscrete;

public final class VoxelShapeCube extends VoxelShape {
    protected VoxelShapeCube(VoxelShapeDiscrete var0) {
        super(var0);
    }

    protected DoubleList a(EnumDirection.EnumAxis var0) {
        return new VoxelShapeCubePoint(this.a.c(var0));
    }

    protected int a(EnumDirection.EnumAxis var0, double var1) {
        int var3 = this.a.c(var0);
        return MathHelper.clamp(MathHelper.floor(var1 * (double) var3), -1, var3);
    }
}
