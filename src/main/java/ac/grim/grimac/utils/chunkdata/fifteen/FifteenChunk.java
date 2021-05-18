package ac.grim.grimac.utils.chunkdata.fifteen;

import ac.grim.grimac.utils.blockstate.BaseBlockState;
import ac.grim.grimac.utils.blockstate.FlatBlockState;
import ac.grim.grimac.utils.chunkdata.BaseChunk;
import ac.grim.grimac.utils.nmsImplementations.XMaterial;
import com.github.steveice10.packetlib.io.NetInput;
import com.github.steveice10.packetlib.io.NetOutput;
import lombok.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Data
@Setter(AccessLevel.NONE)
@AllArgsConstructor
public class FifteenChunk implements BaseChunk {
    private static final BlockState AIR = new BlockState(0);
    private static final int AIR_ID = 0;

    private int blockCount;
    private int bitsPerEntry;

    private @NonNull List<BlockState> states;
    private @NonNull LegacyFlexibleStorage storage;

    public static FifteenChunk read(NetInput in) throws IOException {
        int blockCount = 0;
        // 1.14 and 1.15 include block count in chunk data
        // In 1.13 we don't send that, so there is no need to keep track of it
        if (XMaterial.getVersion() != 13) {
            blockCount = in.readShort();
        }

        int bitsPerEntry = in.readUnsignedByte();

        List<BlockState> states = new ArrayList<>();
        int stateCount = bitsPerEntry > 8 ? 0 : in.readVarInt();
        for (int i = 0; i < stateCount; i++) {
            states.add(BlockState.read(in));
        }

        LegacyFlexibleStorage storage = new LegacyFlexibleStorage(bitsPerEntry, in.readLongs(in.readVarInt()));
        return new FifteenChunk(blockCount, bitsPerEntry, states, storage);
    }

    public static void write(NetOutput out, FifteenChunk chunk) throws IOException {
        // ViaVersion should handle not writing block count in 1.13, as vanilla doesn't include it
        // It would probably crash the client if we tried writing it
        if (XMaterial.getVersion() != 13) {
            out.writeShort(chunk.getBlockCount());
        }

        out.writeByte(chunk.getBitsPerEntry());

        if (chunk.getBitsPerEntry() <= 8) {
            out.writeVarInt(chunk.getStates().size());
            for (BlockState state : chunk.getStates()) {
                BlockState.write(out, state);
            }
        }

        long[] data = chunk.getStorage().getData();
        out.writeVarInt(data.length);
        out.writeLongs(data);
    }

    private static int index(int x, int y, int z) {
        return y << 8 | z << 4 | x;
    }

    public BaseBlockState get(int x, int y, int z) {
        return new FlatBlockState(getInt(x, y, z));
    }

    public int getInt(int x, int y, int z) {
        int id = this.storage.get(index(x, y, z));
        return this.bitsPerEntry <= 8 ? (id >= 0 && id < this.states.size() ? this.states.get(id).getId() : AIR_ID) : id;
    }

    public void set(int x, int y, int z, int state) {
        set(x, y, z, new BlockState(state));
    }

    public void set(int x, int y, int z, @NonNull BlockState state) {
        int id = this.bitsPerEntry <= 8 ? this.states.indexOf(state) : state.getId();
        if (id == -1) {
            this.states.add(state);
            if (this.states.size() > 1 << this.bitsPerEntry) {
                this.bitsPerEntry++;

                List<BlockState> oldStates = this.states;
                if (this.bitsPerEntry > 8) {
                    oldStates = new ArrayList<>(this.states);
                    this.states.clear();
                    this.bitsPerEntry = 13;
                }

                LegacyFlexibleStorage oldStorage = this.storage;
                this.storage = new LegacyFlexibleStorage(this.bitsPerEntry, this.storage.getSize());
                for (int index = 0; index < this.storage.getSize(); index++) {
                    this.storage.set(index, this.bitsPerEntry <= 8 ? oldStorage.get(index) : oldStates.get(index).getId());
                }
            }

            id = this.bitsPerEntry <= 8 ? this.states.indexOf(state) : state.getId();
        }

        int ind = index(x, y, z);
        int curr = this.storage.get(ind);
        if (state.getId() != AIR.getId() && curr == AIR.getId()) {
            this.blockCount++;
        } else if (state.getId() == AIR.getId() && curr != AIR.getId()) {
            this.blockCount--;
        }

        this.storage.set(ind, id);
    }

    public boolean isEmpty() {
        for (int index = 0; index < this.storage.getSize(); index++) {
            if (this.storage.get(index) != 0) {
                return false;
            }
        }

        return true;
    }
}

