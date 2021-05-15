package ac.grim.grimac.utils.chunkdata.twelve;

import ac.grim.grimac.utils.blockstate.MagicBlockState;
import ac.grim.grimac.utils.chunkdata.BaseChunk;
import ac.grim.grimac.utils.chunkdata.MagicChunk;
import com.github.steveice10.packetlib.io.NetInput;
import com.github.steveice10.packetlib.io.NetOutput;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TwelveChunk extends BaseChunk implements MagicChunk {
    private static final MagicBlockState AIR = new MagicBlockState(0, 0);
    private final List<MagicBlockState> states;
    private int bitsPerEntry;
    private TwelveFlexibleStorage storage;

    public TwelveChunk() {
        this.bitsPerEntry = 4;

        this.states = new ArrayList<MagicBlockState>();
        this.states.add(AIR);

        this.storage = new TwelveFlexibleStorage(this.bitsPerEntry, 4096);
    }

    public TwelveChunk(NetInput in) throws IOException {
        this.bitsPerEntry = in.readUnsignedByte();

        this.states = new ArrayList<>();
        int stateCount = in.readVarInt();
        for (int i = 0; i < stateCount; i++) {
            this.states.add(readBlockState(in));
        }

        this.storage = new TwelveFlexibleStorage(this.bitsPerEntry, in.readLongs(in.readVarInt()));
    }

    private static int index(int x, int y, int z) {
        return y << 8 | z << 4 | x;
    }

    private static MagicBlockState rawToState(int raw) {
        return new MagicBlockState(raw >> 4, raw & 0xF);
    }

    private static int stateToRaw(MagicBlockState state) {
        return (state.getId() << 4) | (state.getData() & 0xF);
    }

    public static MagicBlockState readBlockState(NetInput in) throws IOException {
        int rawId = in.readVarInt();
        return new MagicBlockState(rawId >> 4, rawId & 0xF);
    }

    public static void writeBlockState(NetOutput out, MagicBlockState blockState) throws IOException {
        out.writeVarInt((blockState.getId() << 4) | (blockState.getData() & 0xF));
    }

    public MagicBlockState get(int x, int y, int z) {
        int id = this.storage.get(index(x, y, z));
        return this.bitsPerEntry <= 8 ? (id >= 0 && id < this.states.size() ? this.states.get(id) : AIR) : rawToState(id);
    }

    public void set(int x, int y, int z, MagicBlockState state) {
        int id = this.bitsPerEntry <= 8 ? this.states.indexOf(state) : stateToRaw(state);
        if (id == -1) {
            this.states.add(state);
            if (this.states.size() > 1 << this.bitsPerEntry) {
                this.bitsPerEntry++;

                List<MagicBlockState> oldStates = this.states;
                if (this.bitsPerEntry > 8) {
                    oldStates = new ArrayList<MagicBlockState>(this.states);
                    this.states.clear();
                    this.bitsPerEntry = 13;
                }

                TwelveFlexibleStorage oldStorage = this.storage;
                this.storage = new TwelveFlexibleStorage(this.bitsPerEntry, this.storage.getSize());
                for (int index = 0; index < this.storage.getSize(); index++) {
                    this.storage.set(index, this.bitsPerEntry <= 8 ? oldStorage.get(index) : stateToRaw(oldStates.get(index)));
                }
            }

            id = this.bitsPerEntry <= 8 ? this.states.indexOf(state) : stateToRaw(state);
        }

        this.storage.set(index(x, y, z), id);
    }

    public void write(NetOutput out) throws IOException {
        out.writeByte(this.bitsPerEntry);

        out.writeVarInt(this.states.size());
        for (MagicBlockState state : this.states) {
            writeBlockState(out, state);
        }

        long[] data = this.storage.getData();
        out.writeVarInt(data.length);
        out.writeLongs(data);
    }

    public int getBitsPerEntry() {
        return this.bitsPerEntry;
    }

    public List<MagicBlockState> getStates() {
        return Collections.unmodifiableList(this.states);
    }

    public TwelveFlexibleStorage getStorage() {
        return this.storage;
    }
}
