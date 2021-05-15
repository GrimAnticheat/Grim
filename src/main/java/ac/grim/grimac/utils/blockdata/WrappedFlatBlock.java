package ac.grim.grimac.utils.blockdata;

import org.bukkit.block.data.BlockData;

public class WrappedFlatBlock extends WrappedBlockDataValue {
    BlockData blockData;

    public BlockData getBlockData() {
        return blockData;
    }

    public void setBlockData(BlockData blockData) {
        this.blockData = blockData;
    }
}
