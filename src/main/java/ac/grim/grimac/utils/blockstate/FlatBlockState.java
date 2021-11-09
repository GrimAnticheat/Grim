package ac.grim.grimac.utils.blockstate;

import ac.grim.grimac.utils.latency.CompensatedWorldFlat;
import lombok.ToString;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;

@ToString
public class FlatBlockState implements BaseBlockState {
    BlockData blockData;
    int globalID;

    public FlatBlockState(int globalID) {
        this.blockData = CompensatedWorldFlat.globalPaletteToBlockData.get(globalID);
        this.globalID = globalID;
    }

    public FlatBlockState(BlockData blockData) {
        this.blockData = blockData;
        this.globalID = CompensatedWorldFlat.globalPaletteToBlockData.indexOf(blockData);
    }

    public FlatBlockState(Material material) {
        this(material.createBlockData());
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
