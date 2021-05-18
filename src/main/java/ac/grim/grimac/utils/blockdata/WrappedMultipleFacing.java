package ac.grim.grimac.utils.blockdata;

import org.bukkit.block.BlockFace;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class WrappedMultipleFacing extends WrappedBlockDataValue {
    private static final Set<BlockFace> EMPTY = new HashSet<>();
    Set<BlockFace> directions = EMPTY;

    public Set<BlockFace> getDirections() {
        return directions;
    }

    public void setDirections(Set<BlockFace> directions) {
        this.directions = directions;
    }

    public void setDirections(BlockFace directions) {
        this.directions = Collections.singleton(directions);
    }
}
