package ac.grim.grimac.utils.blockdata.types;

import org.bukkit.Material;
import org.bukkit.block.data.BlockData;

public class WrappedFlatBlock extends WrappedBlockDataValue {
    private static BlockData air = null;

    static {
        if (ItemTypes.isNewVersion()) {
            air = Material.AIR.createBlockData();
        }
    }

    BlockData blockData = air;

    public BlockData getBlockData() {
        return blockData;
    }

    public void setBlockData(BlockData blockData) {
        this.blockData = blockData;
    }
}
