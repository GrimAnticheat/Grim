package ac.grim.grimac.utils.nmsImplementations.tuinityVoxelShapes;

import net.minecraft.server.v1_16_R3.EnumDirection;

import java.util.BitSet;

public final class VoxelShapeBitSet extends VoxelShapeDiscrete {
    private final BitSet d;
    private int e;
    private int f;
    private int g;
    private int h;
    private int i;
    private int j;

    public VoxelShapeBitSet(int var0, int var1, int var2) {
        this(var0, var1, var2, var0, var1, var2, 0, 0, 0);
    }

    public VoxelShapeBitSet(int var0, int var1, int var2, int var3, int var4, int var5, int var6, int var7, int var8) {
        super(var0, var1, var2);
        this.d = new BitSet(var0 * var1 * var2);
        this.e = var3;
        this.f = var4;
        this.g = var5;
        this.h = var6;
        this.i = var7;
        this.j = var8;
    }

    public VoxelShapeBitSet(VoxelShapeDiscrete var0) {
        super(var0.a, var0.b, var0.c);
        if (var0 instanceof VoxelShapeBitSet) {
            this.d = (BitSet) ((VoxelShapeBitSet) var0).d.clone();
        } else {
            this.d = new BitSet(this.a * this.b * this.c);

            for (int var1 = 0; var1 < this.a; ++var1) {
                for (int var2 = 0; var2 < this.b; ++var2) {
                    for (int var3 = 0; var3 < this.c; ++var3) {
                        if (var0.b(var1, var2, var3)) {
                            this.d.set(this.a(var1, var2, var3));
                        }
                    }
                }
            }
        }

        this.e = var0.a(EnumDirection.EnumAxis.X);
        this.f = var0.a(EnumDirection.EnumAxis.Y);
        this.g = var0.a(EnumDirection.EnumAxis.Z);
        this.h = var0.b(EnumDirection.EnumAxis.X);
        this.i = var0.b(EnumDirection.EnumAxis.Y);
        this.j = var0.b(EnumDirection.EnumAxis.Z);
    }

    static VoxelShapeBitSet a(VoxelShapeDiscrete var0, VoxelShapeDiscrete var1, VoxelShapeMerger var2, VoxelShapeMerger var3, VoxelShapeMerger var4, OperatorBoolean var5) {
        VoxelShapeBitSet var6 = new VoxelShapeBitSet(var2.a().size() - 1, var3.a().size() - 1, var4.a().size() - 1);
        int[] var7 = new int[]{2147483647, 2147483647, 2147483647, -2147483648, -2147483648, -2147483648};
        var2.a((var7x, var8, var9) -> {
            boolean[] var10 = new boolean[]{false};
            boolean var11 = var3.a((var10x, var11x, var12) -> {
                boolean[] var13 = new boolean[]{false};
                boolean var14 = var4.a((var12x, var13x, var14x) -> {
                    boolean var15 = var5.apply(var0.c(var7x, var10x, var12x), var1.c(var8, var11x, var13x));
                    if (var15) {
                        var6.d.set(var6.a(var9, var12, var14x));
                        var7[2] = Math.min(var7[2], var14x);
                        var7[5] = Math.max(var7[5], var14x);
                        var13[0] = true;
                    }

                    return true;
                });
                if (var13[0]) {
                    var7[1] = Math.min(var7[1], var12);
                    var7[4] = Math.max(var7[4], var12);
                    var10[0] = true;
                }

                return var14;
            });
            if (var10[0]) {
                var7[0] = Math.min(var7[0], var9);
                var7[3] = Math.max(var7[3], var9);
            }

            return var11;
        });
        var6.e = var7[0];
        var6.f = var7[1];
        var6.g = var7[2];
        var6.h = var7[3] + 1;
        var6.i = var7[4] + 1;
        var6.j = var7[5] + 1;
        return var6;
    }

    protected int a(int var0, int var1, int var2) {
        return (var0 * this.b + var1) * this.c + var2;
    }

    public boolean b(int var0, int var1, int var2) {
        return this.d.get(this.a(var0, var1, var2));
    }

    public void a(int var0, int var1, int var2, boolean var3, boolean var4) {
        this.d.set(this.a(var0, var1, var2), var4);
        if (var3 && var4) {
            this.e = Math.min(this.e, var0);
            this.f = Math.min(this.f, var1);
            this.g = Math.min(this.g, var2);
            this.h = Math.max(this.h, var0 + 1);
            this.i = Math.max(this.i, var1 + 1);
            this.j = Math.max(this.j, var2 + 1);
        }

    }

    public boolean a() {
        return this.d.isEmpty();
    }

    public int a(EnumDirection.EnumAxis var0) {
        return var0.a(this.e, this.f, this.g);
    }

    public int b(EnumDirection.EnumAxis var0) {
        return var0.a(this.h, this.i, this.j);
    }

    protected boolean a(int var0, int var1, int var2, int var3) {
        if (var2 >= 0 && var3 >= 0 && var0 >= 0) {
            if (var2 < this.a && var3 < this.b && var1 <= this.c) {
                return this.d.nextClearBit(this.a(var2, var3, var0)) >= this.a(var2, var3, var1);
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    protected void a(int var0, int var1, int var2, int var3, boolean var4) {
        this.d.set(this.a(var2, var3, var0), this.a(var2, var3, var1), var4);
    }
}
