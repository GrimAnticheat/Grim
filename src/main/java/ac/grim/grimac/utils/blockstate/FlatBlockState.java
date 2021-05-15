package ac.grim.grimac.utils.blockstate;

import org.bukkit.Material;
import org.bukkit.block.data.BlockData;

public class FlatBlockState implements BaseBlockState {
    BlockData blockData;

    public FlatBlockState(BlockData blockData) {
        this.blockData = blockData;
    }

    @Override
    public Material getMaterial() {
        return blockData.getMaterial();
    }

    public BlockData getBlockData() {
        return blockData;
    }
}
