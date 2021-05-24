package ac.grim.grimac.utils.chunkdata.twelve;

import ac.grim.grimac.utils.blockstate.MagicBlockState;
import ac.grim.grimac.utils.chunkdata.BaseChunk;
import ac.grim.grimac.utils.chunkdata.fifteen.LegacyFlexibleStorage;
import com.github.steveice10.packetlib.io.NetInput;
import com.github.steveice10.packetlib.io.NetOutput;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TwelveChunk implements BaseChunk {
    private static final MagicBlockState AIR = new MagicBlockState(0, 0);
    private final List<MagicBlockState> states;
    private int bitsPerEntry;
    private LegacyFlexibleStorage storage;


    public TwelveChunk(NetInput in) throws IOException {
        this.bitsPerEntry = in.readUnsignedByte();

        this.states = new ArrayList<>();
        int stateCount = in.readVarInt();
        for (int i = 0; i < stateCount; i++) {
            this.states.add(readBlockState(in));
        }

        this.storage = new LegacyFlexibleStorage(this.bitsPerEntry, in.readLongs(in.readVarInt()));
    }

    public TwelveChunk() {
        this.bitsPerEntry = 4;

        this.states = new ArrayList<>();
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
        out.writeVarInt((blockState.getId() << 4) | (blockState.getData() & 0xF));
    }

    public void eightChunkReader(NetInput in) throws IOException {
        for (int y = 0; y < 16; y++) {
            for (int z = 0; z < 16; z++) {
                for (int x = 0; x < 16; x++) {
                    set(x, y, z, new MagicBlockState(in.readByte(), in.readByte()));
                }
            }
        }
    }

    public MagicBlockState get(int x, int y, int z) {
        int id = this.storage.get(index(x, y, z));
        return this.bitsPerEntry <= 8 ? (id >= 0 && id < this.states.size() ? this.states.get(id) : AIR) : rawToState(id);
    }


    public void set(int x, int y, int z, int combinedID) {
        MagicBlockState blockState = new MagicBlockState(combinedID);
        //Bukkit.broadcastMessage("Setting " + x + " " + y + " " + z + " to " + blockState.getMaterial());
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
                    oldStates = new ArrayList<>(this.states);
                    this.states.clear();
                    this.bitsPerEntry = 13;
                }

                LegacyFlexibleStorage oldStorage = this.storage;
                this.storage = new LegacyFlexibleStorage(this.bitsPerEntry, this.storage.getSize());
                for (int index = 0; index < this.storage.getSize(); index++) {
                    this.storage.set(index, this.bitsPerEntry <= 8 ? oldStorage.get(index) : oldStates.get(index).getCombinedId());
                }
            }

            id = this.bitsPerEntry <= 8 ? this.states.indexOf(state) : state.getCombinedId();
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

    public LegacyFlexibleStorage getStorage() {
        return this.storage;
    }
}
