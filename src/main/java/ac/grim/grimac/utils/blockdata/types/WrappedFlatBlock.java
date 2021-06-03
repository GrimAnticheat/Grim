package ac.grim.grimac.utils.blockdata.types;

import ac.grim.grimac.utils.nmsImplementations.XMaterial;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;

public class WrappedFlatBlock extends WrappedBlockDataValue {
    private static BlockData AIR = null;

    static {
        if (XMaterial.isNewVersion()) {
            AIR = Material.AIR.createBlockData();
        }
    }

    BlockData blockData = AIR;

    public BlockData getBlockData() {
        return blockData;
    }

    public void setBlockData(BlockData blockData) {
        this.blockData = blockData;
    }
}
