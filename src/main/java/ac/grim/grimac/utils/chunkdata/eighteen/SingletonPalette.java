package ac.grim.grimac.utils.chunkdata.eighteen;

import ac.grim.grimac.utils.chunkdata.sixteen.Palette;
import com.github.steveice10.packetlib.io.NetInput;
import lombok.EqualsAndHashCode;

import java.io.IOException;

/**
 * A palette containing one state.
 * Credit to MCProtocolLib
 */
@EqualsAndHashCode
public class SingletonPalette implements Palette {
    private final int state;

    public SingletonPalette(int state) {
        this.state = state;
    }

    public SingletonPalette(NetInput in) throws IOException {
        this.state = in.readVarInt();
    }

    @Override
    public int size() {
        return 1;
    }

    @Override
    public int stateToId(int state) {
        if (this.state == state) {
            return 0;
        }
        return -1;
    }

    @Override
    public int idToState(int id) {
        if (id == 0) {
            return this.state;
        }
        return 0;
    }
}