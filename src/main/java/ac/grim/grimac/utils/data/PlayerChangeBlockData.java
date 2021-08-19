package ac.grim.grimac.utils.data;

import ac.grim.grimac.utils.latency.CompensatedWorldFlat;
import com.google.common.base.Objects;
import org.bukkit.block.data.BlockData;

public class PlayerChangeBlockData extends BasePlayerChangeBlockData {
    public BlockData data;

    public PlayerChangeBlockData(int transaction, int blockX, int blockY, int blockZ, BlockData data) {
        super(transaction, blockX, blockY, blockZ);
        this.data = data;
    }

    @Override
    public int getCombinedID() {
        return CompensatedWorldFlat.getFlattenedGlobalID(data);
    }

    @Override
    public int hashCode() {
        return uniqueID;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PlayerChangeBlockData)) return false;
        if (!super.equals(o)) return false;
        PlayerChangeBlockData that = (PlayerChangeBlockData) o;
        return Objects.equal(data, that.data);
    }
}
