package ac.grim.grimac.utils.nmsImplementations.tuinityVoxelShapes;


import net.minecraft.server.v1_16_R3.EnumAxisCycle;
import net.minecraft.server.v1_16_R3.EnumDirection;

public abstract class VoxelShapeDiscrete {
    private static final EnumDirection.EnumAxis[] d = EnumDirection.EnumAxis.values();
    protected final int a;
    protected final int b;
    protected final int c;

    protected VoxelShapeDiscrete(int var0, int var1, int var2) {
        this.a = var0;
        this.b = var1;
        this.c = var2;
    }

    public boolean a(EnumAxisCycle var0, int var1, int var2, int var3) {
        return this.c(var0.a(var1, var2, var3, EnumDirection.EnumAxis.X), var0.a(var1, var2, var3, EnumDirection.EnumAxis.Y), var0.a(var1, var2, var3, EnumDirection.EnumAxis.Z));
    }

    public boolean c(int var0, int var1, int var2) {
        if (var0 >= 0 && var1 >= 0 && var2 >= 0) {
            return var0 < this.a && var1 < this.b && var2 < this.c && this.b(var0, var1, var2);
        } else {
            return false;
        }
    }

    public boolean b(EnumAxisCycle var0, int var1, int var2, int var3) {
        return this.b(var0.a(var1, var2, var3, EnumDirection.EnumAxis.X), var0.a(var1, var2, var3, EnumDirection.EnumAxis.Y), var0.a(var1, var2, var3, EnumDirection.EnumAxis.Z));
    }

    public abstract boolean b(int var1, int var2, int var3);

    public abstract void a(int var1, int var2, int var3, boolean var4, boolean var5);

    public boolean a() {
        EnumDirection.EnumAxis[] var1 = d;
        int var2 = var1.length;

        for (int var3 = 0; var3 < var2; ++var3) {
            EnumDirection.EnumAxis var4 = var1[var3];
            if (this.a(var4) >= this.b(var4)) {
                return true;
            }
        }

        return false;
    }

    public abstract int a(EnumDirection.EnumAxis var1);

    public abstract int b(EnumDirection.EnumAxis var1);

    public int c(EnumDirection.EnumAxis var0) {
        return var0.a(this.a, this.b, this.c);
    }

    public int b() {
        return this.c(EnumDirection.EnumAxis.X);
    }

    public int c() {
        return this.c(EnumDirection.EnumAxis.Y);
    }

    public int d() {
        return this.c(EnumDirection.EnumAxis.Z);
    }

    protected boolean a(int var0, int var1, int var2, int var3) {
        for (int var4 = var0; var4 < var1; ++var4) {
            if (!this.c(var2, var3, var4)) {
                return false;
            }
        }

        return true;
    }

    protected void a(int var0, int var1, int var2, int var3, boolean var4) {
        for (int var5 = var0; var5 < var1; ++var5) {
            this.a(var2, var3, var5, false, var4);
        }

    }

    protected boolean a(int var0, int var1, int var2, int var3, int var4) {
        for (int var5 = var0; var5 < var1; ++var5) {
            if (!this.a(var2, var3, var5, var4)) {
                return false;
            }
        }

        return true;
    }

    public void b(VoxelShapeDiscrete.b var0, boolean var1) {
        VoxelShapeDiscrete var2 = new VoxelShapeBitSet(this);

        for (int var3 = 0; var3 <= this.a; ++var3) {
            for (int var4 = 0; var4 <= this.b; ++var4) {
                int var5 = -1;

                for (int var6 = 0; var6 <= this.c; ++var6) {
                    if (var2.c(var3, var4, var6)) {
                        if (var1) {
                            if (var5 == -1) {
                                var5 = var6;
                            }
                        } else {
                            var0.consume(var3, var4, var6, var3 + 1, var4 + 1, var6 + 1);
                        }
                    } else if (var5 != -1) {
                        int var7 = var3;
                        int var8 = var3;
                        int var9 = var4;
                        int var10 = var4;
                        var2.a(var5, var6, var3, var4, false);

                        while (var2.a(var5, var6, var7 - 1, var9)) {
                            var2.a(var5, var6, var7 - 1, var9, false);
                            --var7;
                        }

                        while (var2.a(var5, var6, var8 + 1, var9)) {
                            var2.a(var5, var6, var8 + 1, var9, false);
                            ++var8;
                        }

                        int var11;
                        while (var2.a(var7, var8 + 1, var5, var6, var9 - 1)) {
                            for (var11 = var7; var11 <= var8; ++var11) {
                                var2.a(var5, var6, var11, var9 - 1, false);
                            }

                            --var9;
                        }

                        while (var2.a(var7, var8 + 1, var5, var6, var10 + 1)) {
                            for (var11 = var7; var11 <= var8; ++var11) {
                                var2.a(var5, var6, var11, var10 + 1, false);
                            }

                            ++var10;
                        }

                        var0.consume(var7, var9, var5, var8 + 1, var10 + 1, var6);
                        var5 = -1;
                    }
                }
            }
        }

    }

    public void a(VoxelShapeDiscrete.a var0) {
        this.a(var0, EnumAxisCycle.NONE);
        this.a(var0, EnumAxisCycle.FORWARD);
        this.a(var0, EnumAxisCycle.BACKWARD);
    }

    private void a(VoxelShapeDiscrete.a var0, EnumAxisCycle var1) {
        EnumAxisCycle var2 = var1.a();
        EnumDirection.EnumAxis var3 = var2.a(EnumDirection.EnumAxis.Z);
        int var4 = this.c(var2.a(EnumDirection.EnumAxis.X));
        int var5 = this.c(var2.a(EnumDirection.EnumAxis.Y));
        int var6 = this.c(var3);
        EnumDirection var7 = EnumDirection.a(var3, EnumDirection.EnumAxisDirection.NEGATIVE);
        EnumDirection var8 = EnumDirection.a(var3, EnumDirection.EnumAxisDirection.POSITIVE);

        for (int var9 = 0; var9 < var4; ++var9) {
            for (int var10 = 0; var10 < var5; ++var10) {
                boolean var11 = false;

                for (int var12 = 0; var12 <= var6; ++var12) {
                    boolean var13 = var12 != var6 && this.b(var2, var9, var10, var12);
                    if (!var11 && var13) {
                        var0.consume(var7, var2.a(var9, var10, var12, EnumDirection.EnumAxis.X), var2.a(var9, var10, var12, EnumDirection.EnumAxis.Y), var2.a(var9, var10, var12, EnumDirection.EnumAxis.Z));
                    }

                    if (var11 && !var13) {
                        var0.consume(var8, var2.a(var9, var10, var12 - 1, EnumDirection.EnumAxis.X), var2.a(var9, var10, var12 - 1, EnumDirection.EnumAxis.Y), var2.a(var9, var10, var12 - 1, EnumDirection.EnumAxis.Z));
                    }

                    var11 = var13;
                }
            }
        }

    }

    public interface a {
        void consume(EnumDirection var1, int var2, int var3, int var4);
    }

    public interface b {
        void consume(int var1, int var2, int var3, int var4, int var5, int var6);
    }
}
