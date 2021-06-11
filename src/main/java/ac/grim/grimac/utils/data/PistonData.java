package ac.grim.grimac.utils.data;

import ac.grim.grimac.utils.collisions.datatypes.SimpleCollisionBox;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

import java.util.ArrayList;
import java.util.List;

public class PistonData {
    public final boolean isPush;
    public final BlockFace direction;
    public final int lastTransactionSent;

    // Calculate if the player has no-push, and when to end the possibility of applying piston
    public int ticksOfPistonBeingAlive = 0;

    // The actual blocks pushed by the piston, plus the piston head itself
    public List<SimpleCollisionBox> boxes;

    public PistonData(BlockFace direction, List<SimpleCollisionBox> pushedBlocks, int lastTransactionSent, boolean isPush) {
        this.direction = direction;
        this.boxes = pushedBlocks;
        this.lastTransactionSent = lastTransactionSent;
        this.isPush = isPush;
    }

    // We don't know when the piston has applied, or what stage of pushing it is on
    // Therefore, we need to use what we have - the number of movement packets.
    public boolean tickIfGuaranteedFinished() {
        return ++ticksOfPistonBeingAlive >= 3;
    }
}
