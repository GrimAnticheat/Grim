package ac.grim.grimac.utils.blockstate;

import ac.grim.grimac.utils.latency.CompensatedWorld;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;

public class FlatBlockState implements BaseBlockState {
    BlockData blockData;

    public FlatBlockState(BlockData blockData) {
        this.blockData = blockData;
    }

    public FlatBlockState(int globalID) {
        this.blockData = CompensatedWorld.globalPaletteToBlockData.get(globalID);
    }

    @Override
    public Material getMaterial() {
        return blockData.getMaterial();
    }

    public BlockData getBlockData() {
        return blockData;
    }

    public int getCombinedId() {
        return CompensatedWorld.getFlattenedGlobalID(blockData);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FlatBlockState)) return false;

        FlatBlockState that = (FlatBlockState) o;
        return getCombinedId() == that.getCombinedId();
    }
}
