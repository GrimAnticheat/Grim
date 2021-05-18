package ac.grim.grimac.utils.blockdata;

import org.bukkit.Material;
import org.bukkit.block.data.BlockData;

public class WrappedFlatBlock extends WrappedBlockDataValue {
    private static final BlockData AIR = Material.AIR.createBlockData();
    BlockData blockData = AIR;

    public BlockData getBlockData() {
        return blockData;
    }

    public void setBlockData(BlockData blockData) {
        this.blockData = blockData;
    }
}
