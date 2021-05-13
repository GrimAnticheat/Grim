package ac.grim.grimac.utils.chunkdata.sixteen;

import com.github.steveice10.packetlib.io.NetInput;

import java.io.IOException;

// Credit to https://github.com/Steveice10/MCProtocolLib/blob/master/src/main/java/com/github/steveice10/mc/protocol/data/game/chunk/palette/ListPalette.java
public class ListPalette implements Palette {
    private final int maxId;
    private final int[] data;
    private int nextId;

    public ListPalette(int bitsPerEntry) {
        this.nextId = 0;
        this.maxId = (1 << bitsPerEntry) - 1;
        this.data = new int[this.maxId + 1];
    }

    public ListPalette(int bitsPerEntry, NetInput in) throws IOException {
        this(bitsPerEntry);
        int paletteLength = in.readVarInt();

        for (int i = 0; i < paletteLength; ++i) {
            this.data[i] = in.readVarInt();
        }

        this.nextId = paletteLength;
    }

    public int size() {
        return this.nextId;
    }

    public int stateToId(int state) {
        int id = -1;

        for (int i = 0; i < this.nextId; ++i) {
            if (this.data[i] == state) {
                id = i;
                break;
            }
        }

        if (id == -1 && this.size() < this.maxId + 1) {
            id = this.nextId++;
            this.data[id] = state;
        }

        return id;
    }

    @Override
    public int idToState(int id) {
        if (id >= 0 && id < this.size()) {
            return this.data[id];
        } else {
            return 0;
        }
    }
}
