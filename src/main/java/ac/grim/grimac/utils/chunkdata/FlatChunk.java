package ac.grim.grimac.utils.chunkdata;

public interface FlatChunk {
    int get(int x, int y, int z);

    void set(int x, int y, int z, int state);
}
