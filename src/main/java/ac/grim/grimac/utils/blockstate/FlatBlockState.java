package ac.grim.grimac.utils.blockstate;

import ac.grim.grimac.utils.latency.CompensatedWorldFlat;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;

public class FlatBlockState implements BaseBlockState {
    BlockData blockData;
    int globalID;

    // Required to init air data
    public FlatBlockState(BlockData data, int globalID) {
        this.blockData = data;
        this.globalID = globalID;
    }

    public FlatBlockState(int globalID) {
        this.blockData = CompensatedWorldFlat.globalPaletteToBlockData.get(globalID);
        this.globalID = globalID;
    }

    @Override
    public Material getMaterial() {
        return blockData.getMaterial();
    }

    public int getCombinedId() {
        return globalID;
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
