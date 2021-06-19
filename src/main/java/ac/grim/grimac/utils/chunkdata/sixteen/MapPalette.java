package ac.grim.grimac.utils.chunkdata.sixteen;

import com.github.steveice10.packetlib.io.NetInput;
import io.netty.util.collection.IntObjectHashMap;
import io.netty.util.collection.IntObjectMap;

import java.io.IOException;

public class MapPalette implements Palette {
    private final int maxId;
    private final int[] idToState;
    private final IntObjectMap<Integer> stateToId;
    private int nextId;

    public MapPalette(int bitsPerEntry) {
        this.stateToId = new IntObjectHashMap<>();
        this.nextId = 0;
        this.maxId = (1 << bitsPerEntry) - 1;
        this.idToState = new int[this.maxId + 1];
    }

    public MapPalette(int bitsPerEntry, NetInput in) throws IOException {
        this(bitsPerEntry);
        int paletteLength = in.readVarInt();

        for (int i = 0; i < paletteLength; ++i) {
            int state = in.readVarInt();
            this.idToState[i] = state;
            this.stateToId.putIfAbsent(state, i);
        }

        this.nextId = paletteLength;
    }

    public int size() {
        return this.nextId;
    }

    public int stateToId(int state) {
        Integer id = this.stateToId.get(state);
        if (id == null && this.size() < this.maxId + 1) {
            id = this.nextId++;
            this.idToState[id] = state;
            this.stateToId.put(state, id);
        }

        return id != null ? id : -1;
    }

    public int idToState(int id) {
        return id >= 0 && id < this.size() ? this.idToState[id] : 0;
    }
}
