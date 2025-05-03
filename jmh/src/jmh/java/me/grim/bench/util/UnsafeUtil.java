package me.grim.bench.util;

import sun.misc.Unsafe;

import java.lang.reflect.Field;

public final class UnsafeUtil {
    private static final Unsafe U;
    static {
        try {
            Field f = Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            U = (Unsafe) f.get(null);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    public static long alloc(long bytes)           { return U.allocateMemory(bytes); }
    public static void free(long addr)             { U.freeMemory(addr); }
    public static long  getLong(long addr)         { return U.getLong(addr); }
    static void  putLong(long a,long v)     { U.putLong(a, v); }

    public static void copyFromLongArray(long[] src, int longs, long dst){
        long bytes = ((long) longs) << 3;
        U.copyMemory(src, Unsafe.ARRAY_LONG_BASE_OFFSET, null, dst, bytes);
    }
}
