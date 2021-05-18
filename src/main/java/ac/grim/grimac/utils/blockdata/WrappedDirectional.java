package ac.grim.grimac.utils.blockdata;

import org.bukkit.block.BlockFace;

public class WrappedDirectional extends WrappedBlockDataValue {
    BlockFace direction = BlockFace.NORTH;

    public BlockFace getDirection() {
        return direction;
    }

    public void setDirection(BlockFace direction) {
        this.direction = direction;
    }
}
