package ac.grim.grimac.utils.data;

import org.bukkit.block.data.BlockData;

public class PlayerChangeBlockData {
    public int tick;
    public int blockX;
    public int blockY;
    public int blockZ;
    public BlockData blockData;

    public PlayerChangeBlockData(int tick, int blockX, int blockY, int blockZ, BlockData blockData) {
        this.tick = tick;
        this.blockX = blockX;
        this.blockY = blockY;
        this.blockZ = blockZ;
        this.blockData = blockData;
    }
}
