package ac.grim.grimac.utils.data;

import ac.grim.grimac.utils.collisions.CollisionBox;
import ac.grim.grimac.utils.collisions.types.SimpleCollisionBox;
import ac.grim.grimac.utils.nmsImplementations.CollisionData;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

import java.util.ArrayList;
import java.util.List;

public class PistonData {
    public final BlockFace direction;
    public final Block piston;
    public final List<Block> pushedBlocks;
    public final boolean isPush;
    public final int lastTransactionSent;

    // Calculate if the player has no-push, and when to end the possibility of applying piston
    public boolean lastTickInPushZone = false;
    public boolean hasPlayerRemainedInPushZone = true;
    public boolean hasPushedPlayer = false;
    public boolean thisTickPushingPlayer = false;
    public int movementPacketSincePossible = 0;

    // The actual blocks pushed by the piston, plus the piston head itself
    public List<SimpleCollisionBox> boxes = new ArrayList<>();

    public PistonData(BlockFace direction, Block piston, List<Block> pushedBlocks, boolean isPush, int lastTransactionSent) {
        this.direction = direction;
        this.piston = piston;
        this.pushedBlocks = pushedBlocks;
        this.isPush = isPush;
        this.lastTransactionSent = lastTransactionSent;

        // We are doing some work on the main thread, be careful
        // We need to do this here otherwise the data will become desync'd as the blocks have already moved
        // Meaning that we will be grabbing bounding boxes of air

        for (org.bukkit.block.Block block : pushedBlocks) {
            CollisionBox box = CollisionData.getData(block.getType()).getMovementCollisionBox(block.getBlockData(), block.getX(), block.getY(), block.getZ(), ProtocolVersion.v1_16_5).offset(direction.getModX(), direction.getModY(), direction.getModZ());
            box.downCast(boxes);
        }

        // Add bounding box of the actual piston head pushing
        CollisionBox box = new SimpleCollisionBox(0, 0, 0, 1, 1, 1).offset(piston.getX() + direction.getModX(), piston.getY() + direction.getModY(), piston.getZ() + direction.getModZ());
        box.downCast(boxes);
    }

    // We don't know when the piston has applied, or what stage of pushing it is on
    // Therefore, we need to use what we have - the number of movement packets.
    public boolean tickIfGuaranteedFinished() {
        if (++movementPacketSincePossible >= 2) {
            if (hasPlayerRemainedInPushZone && !hasPushedPlayer) {
                Bukkit.broadcastMessage("Piston done without pushing player!  Cheating?");
            }

            return true;
        }

        return false;
    }
}
