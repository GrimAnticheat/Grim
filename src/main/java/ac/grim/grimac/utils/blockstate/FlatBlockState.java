package ac.grim.grimac.utils.blockstate;

import ac.grim.grimac.utils.latency.CompensatedWorldFlat;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;

public class FlatBlockState implements BaseBlockState {
    BlockData blockData;

    public FlatBlockState(BlockData blockData) {
        this.blockData = blockData;
    }

    public FlatBlockState(int globalID) {
        this.blockData = CompensatedWorldFlat.globalPaletteToBlockData.get(globalID);
    }

    @Override
    public Material getMaterial() {
        return blockData.getMaterial();
    }

    public int getCombinedId() {
        return CompensatedWorldFlat.getFlattenedGlobalID(blockData);
    }

    public BlockData getBlockData() {
        return blockData;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FlatBlockState)) return false;

        FlatBlockState that = (FlatBlockState) o;
        return getCombinedId() == that.getCombinedId();
    }
}
