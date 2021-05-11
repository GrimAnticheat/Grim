package ac.grim.grimac.utils.data;

import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

import java.util.List;

public class PistonData {
    public final BlockFace direction;
    public final Block piston;
    public final List<Block> pushedBlocks;
    public final boolean isPush;
    public final int lastTransactionSent;

    public PistonData(BlockFace direction, Block piston, List<Block> pushedBlocks, boolean isPush, int lastTransactionSent) {
        this.direction = direction;
        this.piston = piston;
        this.pushedBlocks = pushedBlocks;
        this.isPush = isPush;
        this.lastTransactionSent = lastTransactionSent;
    }
}
