package ac.grim.grimac.utils.chunkdata.sixteen;

import ac.grim.grimac.utils.blockstate.BaseBlockState;
import ac.grim.grimac.utils.blockstate.FlatBlockState;
import ac.grim.grimac.utils.chunkdata.BaseChunk;
import com.github.steveice10.packetlib.io.NetInput;
import com.github.steveice10.packetlib.io.NetOutput;
import lombok.NonNull;

import java.io.IOException;

// Credit to https://github.com/Steveice10/MCProtocolLib/blob/master/src/main/java/com/github/steveice10/mc/protocol/data/game/chunk/Chunk.java
public class SixteenChunk implements BaseChunk {
    private static final int CHUNK_SIZE = 4096;
    private static final int MIN_PALETTE_BITS_PER_ENTRY = 4;
    private static final int MAX_PALETTE_BITS_PER_ENTRY = 8;
    private static final int GLOBAL_PALETTE_BITS_PER_ENTRY = 14;
    private static final int AIR = 0;
    private int blockCount;
    @NonNull
    private Palette palette;
    @NonNull
    private BitStorage storage;

    public SixteenChunk() {
        this(0, new ListPalette(4), new BitStorage(4, 4096));
    }

    public SixteenChunk(int blockCount, @NonNull Palette palette, @NonNull BitStorage storage) {
        this.blockCount = blockCount;
        this.palette = palette;
        this.storage = storage;
    }

    public static SixteenChunk read(NetInput in) throws IOException {
        int blockCount = in.readShort();
        int bitsPerEntry = in.readUnsignedByte();
        Palette palette = readPalette(bitsPerEntry, in);
        BitStorage storage = new BitStorage(bitsPerEntry, 4096, in.readLongs(in.readVarInt()));
        return new SixteenChunk(blockCount, palette, storage);
    }

    public static void write(NetOutput out, SixteenChunk chunk) throws IOException {
        out.writeShort(chunk.blockCount);
        out.writeByte(chunk.storage.getBitsPerEntry());
        if (!(chunk.palette instanceof GlobalPalette)) {
            int paletteLength = chunk.palette.size();
            out.writeVarInt(paletteLength);

            for (int i = 0; i < paletteLength; ++i) {
                out.writeVarInt(chunk.palette.idToState(i));
            }
        }

        long[] data = chunk.storage.getData();
        out.writeVarInt(data.length);
        out.writeLongs(data);
    }

    private static Palette createPalette(int bitsPerEntry) {
        if (bitsPerEntry <= 4) {
            return new ListPalette(bitsPerEntry);
        } else {
            return bitsPerEntry <= 8 ? new MapPalette(bitsPerEntry) : new GlobalPalette();
        }
    }

    private static Palette readPalette(int bitsPerEntry, NetInput in) throws IOException {
        if (bitsPerEntry <= 4) {
            return new ListPalette(bitsPerEntry, in);
        } else {
            return bitsPerEntry <= 8 ? new MapPalette(bitsPerEntry, in) : new GlobalPalette();
        }
    }

    private static int index(int x, int y, int z) {
        return y << 8 | z << 4 | x;
    }

    public BaseBlockState get(int x, int y, int z) {
        int id = this.storage.get(index(x, y, z));
        return new FlatBlockState(this.palette.idToState(id));
    }

    public void set(int x, int y, int z, @NonNull int state) {
        int id = this.palette.stateToId(state);
        if (id == -1) {
            this.resizePalette();
            id = this.palette.stateToId(state);
        }

        int index = index(x, y, z);
        int curr = this.storage.get(index);
        if (state != 0 && curr == 0) {
            ++this.blockCount;
        } else if (state == 0 && curr != 0) {
            --this.blockCount;
        }

        this.storage.set(index, id);
    }

    public boolean isEmpty() {
        return this.blockCount == 0;
    }

    private int sanitizeBitsPerEntry(int bitsPerEntry) {
        return bitsPerEntry <= 8 ? Math.max(4, bitsPerEntry) : 14;
    }

    private void resizePalette() {
        Palette oldPalette = this.palette;
        BitStorage oldData = this.storage;
        int bitsPerEntry = this.sanitizeBitsPerEntry(oldData.getBitsPerEntry() + 1);
        this.palette = createPalette(bitsPerEntry);
        this.storage = new BitStorage(bitsPerEntry, 4096);

        for (int i = 0; i < 4096; ++i) {
            this.storage.set(i, this.palette.stateToId(oldPalette.idToState(oldData.get(i))));
        }
    }

    public int getBlockCount() {
        return this.blockCount;
    }

    @NonNull
    public Palette getPalette() {
        return this.palette;
    }

    @NonNull
    public BitStorage getStorage() {
        return this.storage;
    }
}
