package ac.grim.grimac.utils.chunkdata.eight;

import java.util.Arrays;

public class ShortArray3d {
    private final char[] data;

    public ShortArray3d(char[] array) {
        this.data = Arrays.copyOf(array, array.length);
    }

    public void set(int x, int y, int z, int val) {
        this.data[y << 8 | z << 4 | x] = (char) val;
    }

    public int get(int x, int y, int z) {
        return this.data[y << 8 | z << 4 | x];
    }
}