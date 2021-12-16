package ac.grim.grimac.utils.chunkdata.twelve;

import ac.grim.grimac.utils.blockstate.MagicBlockState;
import ac.grim.grimac.utils.chunkdata.BaseChunk;
import ac.grim.grimac.utils.chunkdata.fifteen.LegacyFlexibleStorage;
import com.github.steveice10.packetlib.io.NetInput;
import com.github.steveice10.packetlib.io.NetOutput;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.io.IOException;
import java.nio.ShortBuffer;
import java.util.List;

public class TwelveChunk implements BaseChunk {
    private static final MagicBlockState AIR = new MagicBlockState(0, 0);
    private final List<MagicBlockState> states;
    private int bitsPerEntry;
    private LegacyFlexibleStorage storage;

    public TwelveChunk(NetInput in) throws IOException {
        this.bitsPerEntry = in.readUnsignedByte();

        this.states = new ObjectArrayList<>();
        int stateCount = in.readVarInt();
        for (int i = 0; i < stateCount; i++) {
            this.states.add(readBlockState(in));
        }

        this.storage = new LegacyFlexibleStorage(this.bitsPerEntry, in.readLongs(in.readVarInt()));
    }

    public TwelveChunk(ShortBuffer in) {
        Int2IntMap reversePalette = new Int2IntOpenHashMap(32, 0.5f);
        reversePalette.defaultReturnValue(-1);

        states = new ObjectArrayList<>();
        states.add(AIR);
        reversePalette.put(0, 0);

        this.bitsPerEntry = 4;
        this.storage = new LegacyFlexibleStorage(bitsPerEntry, 4096);

        int lastNext = -1;
        int lastID = -1;

        for (int i = 0; i < 4096; i++) {
            int next = in.get();

            if (next != lastNext) {
                lastNext = next;
                next = ((next & 15) << 12) | (next >> 4);
                lastID = this.bitsPerEntry <= 8 ? reversePalette.get(next) : next;

                if (lastID == -1) {
                    reversePalette.put(next, reversePalette.size());
                    states.add(new MagicBlockState(next));

                    if (reversePalette.size() > 1 << this.bitsPerEntry) {
                        this.bitsPerEntry++;

                        List<MagicBlockState> oldStates = this.states;
                        if (this.bitsPerEntry > 8) {
                            oldStates = new ObjectArrayList<>(this.states);
                            this.states.clear();
                            reversePalette.clear();
                            this.bitsPerEntry = 16;
                        }

                        LegacyFlexibleStorage oldStorage = this.storage;
                        this.storage = new LegacyFlexibleStorage(this.bitsPerEntry, this.storage.getSize());
                        for (int index = 0; index < this.storage.getSize(); index++) {
                            this.storage.set(index, this.bitsPerEntry <= 8 ? oldStorage.get(index) : oldStates.get(oldStorage.get(index)).getCombinedId());
                        }
                    }

                    lastID = this.bitsPerEntry <= 8 ? reversePalette.get(next) : next;
                }
            }

            this.storage.set(i, lastID);
        }
    }

    public TwelveChunk() {
        this.bitsPerEntry = 4;

        this.states = new ObjectArrayList<>();
        this.states.add(AIR);

        this.storage = new LegacyFlexibleStorage(this.bitsPerEntry, 4096);
    }

    private static int index(int x, int y, int z) {
        return y << 8 | z << 4 | x;
    }

    private static MagicBlockState rawToState(int raw) {
        return new MagicBlockState(raw & 0xFF, raw >> 12);
    }

    public static MagicBlockState readBlockState(NetInput in) throws IOException {
        int rawId = in.readVarInt();
        return new MagicBlockState(rawId >> 4, rawId & 0xF);
    }

    public static void writeBlockState(NetOutput out, MagicBlockState blockState) throws IOException {
        out.writeVarInt((blockState.getId() << 4) | (blockState.getBlockData() & 0xF));
    }

    public MagicBlockState get(int x, int y, int z) {
        int id = this.storage.get(index(x, y, z));
        return this.bitsPerEntry <= 8 ? (id >= 0 && id < this.states.size() ? this.states.get(id) : AIR) : rawToState(id);
    }

    // This method only works post-flattening
    // This is due to the palette system
    @Override
    public boolean isKnownEmpty() {
        return false;
    }

    public void set(int x, int y, int z, int combinedID) {
        set(x, y, z, new MagicBlockState(combinedID));
    }

    public void set(int x, int y, int z, MagicBlockState state) {
        int id = this.bitsPerEntry <= 8 ? this.states.indexOf(state) : state.getCombinedId();
        if (id == -1) {
            this.states.add(state);
            if (this.states.size() > 1 << this.bitsPerEntry) {
                this.bitsPerEntry++;

                List<MagicBlockState> oldStates = this.states;
                if (this.bitsPerEntry > 8) {
                    oldStates = new ObjectArrayList<>(this.states);
                    this.states.clear();
                    this.bitsPerEntry = 16;
                }

                LegacyFlexibleStorage oldStorage = this.storage;
                this.storage = new LegacyFlexibleStorage(this.bitsPerEntry, this.storage.getSize());
                for (int index = 0; index < this.storage.getSize(); index++) {
                    this.storage.set(index, this.bitsPerEntry <= 8 ? oldStorage.get(index) : oldStates.get(oldStorage.get(index)).getCombinedId());
                }
            }

            id = this.bitsPerEntry <= 8 ? this.states.indexOf(state) : state.getCombinedId();
        }

        this.storage.set(index(x, y, z), id);
    }
}
