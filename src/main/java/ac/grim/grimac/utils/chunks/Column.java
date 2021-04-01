package ac.grim.grimac.utils.chunks;

import com.github.steveice10.mc.protocol.data.game.chunk.Chunk;

public class Column {
    public final int x;
    public final int z;
    public final Chunk[] chunks;

    public Column(int x, int z, Chunk[] chunks) {
        this.chunks = chunks;
        this.x = x;
        this.z = z;
    }

    public Chunk[] getChunks() {
        return chunks;
    }
}
