package ac.grim.grimac.utils.chunkdata.fifteen;

import com.github.steveice10.packetlib.io.NetInput;
import com.github.steveice10.packetlib.io.NetOutput;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.IOException;

@Data
@AllArgsConstructor
public class BlockState {
    private final int id;

    public static BlockState read(NetInput in) throws IOException {
        return new BlockState(in.readVarInt());
    }

    public static void write(NetOutput out, BlockState blockState) throws IOException {
        out.writeVarInt(blockState.getId());
    }
}