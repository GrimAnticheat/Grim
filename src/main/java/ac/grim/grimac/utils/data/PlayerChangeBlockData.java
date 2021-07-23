package ac.grim.grimac.utils.data;

import ac.grim.grimac.utils.latency.CompensatedWorldFlat;
import org.bukkit.block.data.BlockData;

public class PlayerChangeBlockData extends BasePlayerChangeBlockData{
    public BlockData data;

    public PlayerChangeBlockData(int transaction, int blockX, int blockY, int blockZ, BlockData data) {
        super(transaction, blockX, blockY, blockZ);
        this.data = data;
    }

    @Override
    public int getCombinedID() {
        return CompensatedWorldFlat.getFlattenedGlobalID(data);
    }
}
