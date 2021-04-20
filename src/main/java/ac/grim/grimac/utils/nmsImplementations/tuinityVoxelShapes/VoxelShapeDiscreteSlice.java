package ac.grim.grimac.utils.nmsImplementations.tuinityVoxelShapes;

import net.minecraft.server.v1_16_R3.EnumDirection;

public final class VoxelShapeDiscreteSlice extends VoxelShapeDiscrete {
    private final VoxelShapeDiscrete d;
    private final int e;
    private final int f;
    private final int g;
    private final int h;
    private final int i;
    private final int j;

    protected VoxelShapeDiscreteSlice(VoxelShapeDiscrete var0, int var1, int var2, int var3, int var4, int var5, int var6) {
        super(var4 - var1, var5 - var2, var6 - var3);
        this.d = var0;
        this.e = var1;
        this.f = var2;
        this.g = var3;
        this.h = var4;
        this.i = var5;
        this.j = var6;
    }

    public boolean b(int var0, int var1, int var2) {
        return this.d.b(this.e + var0, this.f + var1, this.g + var2);
    }

    public void a(int var0, int var1, int var2, boolean var3, boolean var4) {
        this.d.a(this.e + var0, this.f + var1, this.g + var2, var3, var4);
    }

    public int a(EnumDirection.EnumAxis var0) {
        return Math.max(0, this.d.a(var0) - var0.a(this.e, this.f, this.g));
    }

    public int b(EnumDirection.EnumAxis var0) {
        return Math.min(var0.a(this.h, this.i, this.j), this.d.b(var0) - var0.a(this.e, this.f, this.g));
    }
}
