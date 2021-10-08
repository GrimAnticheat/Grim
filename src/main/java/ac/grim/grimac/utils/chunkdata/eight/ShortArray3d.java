package ac.grim.grimac.utils.chunkdata.eight;

public class ShortArray3d {
    private final short[] data;

    public ShortArray3d(int size) {
        this.data = new short[size];
    }

    public void set(int x, int y, int z, int val) {
        this.data[y << 8 | z << 4 | x] = (short) val;
    }

    public int get(int x, int y, int z) {
        return this.data[y << 8 | z << 4 | x];
    }

    public short[] getData() {
        return this.data;
    }
}