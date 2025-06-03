package ac.grim.grimac.platform.bukkit.events;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.platform.bukkit.utils.convert.BukkitConversionUtils;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.collisions.datatypes.SimpleCollisionBox;
import ac.grim.grimac.utils.data.PistonData;
import com.github.retrooper.packetevents.protocol.world.BlockFace;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.util.Vector3i;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;

import java.util.ArrayList;
import java.util.List;

public class PistonEvent implements Listener {

    private static final Material SLIME_BLOCK = Material.getMaterial("SLIME_BLOCK");
    private static final Material HONEY_BLOCK = Material.getMaterial("HONEY_BLOCK");

    // Reusable base collision box to avoid creating new instances
    private static final SimpleCollisionBox BASE_BOX = new SimpleCollisionBox(0, 0, 0, 1, 1, 1, true);

    // Using squared distances for more efficient distance checks
    private static final double MAX_HORIZ_DIST_SQ = 576.0; // 24^2
    private static final double MAX_VERT_DIST_SQ = 4096.0; // 64^2

    private static boolean isWithinRange(Vector3i pos, Vector3d playerPos) {
        double dx = pos.getX() - playerPos.getX();
        double dy = pos.getY() - playerPos.getY();
        double dz = pos.getZ() - playerPos.getZ();
        
        // Check horizontal and vertical distances using squared values
        return (dx * dx + dz * dz) <= MAX_HORIZ_DIST_SQ 
            && dy * dy <= MAX_VERT_DIST_SQ;
    }

    private void processPistonEvent(Block piston, BlockFace direction, List<Block> blocks, boolean isExtending) {
        boolean hasSlimeBlock = false;
        boolean hasHoneyBlock = false;
        // Pre-size the list for optimal memory allocation
        List<SimpleCollisionBox> boxes = new ArrayList<>(blocks.size() * 2 + 1);

        int modX = direction.getModX();
        int modY = direction.getModY();
        int modZ = direction.getModZ();
        int pistonX = piston.getX();
        int pistonY = piston.getY();
        int pistonZ = piston.getZ();

        // Special case handling for empty retract events
        if (!isExtending && blocks.isEmpty()) {
            boxes.add(BASE_BOX.offset(pistonX + modX, pistonY + modY, pistonZ + modZ));
        } else {
            for (Block block : blocks) {
                int x = block.getX();
                int y = block.getY();
                int z = block.getZ();
                
                // Add boxes for original and new positions
                boxes.add(BASE_BOX.offset(x, y, z));
                boxes.add(BASE_BOX.offset(x + modX, y + modY, z + modZ));

                Material type = block.getType();
                if (type == SLIME_BLOCK) {
                    hasSlimeBlock = true;
                } else if (type == HONEY_BLOCK) {
                    hasHoneyBlock = true;
                }
            }

            // For extend events, add the piston head position
            if (isExtending) {
                boxes.add(BASE_BOX.offset(pistonX + modX, pistonY + modY, pistonZ + modZ));
            }
        }

        final int chunkX = pistonX >> 4;
        final int chunkZ = pistonZ >> 4;
        Vector3i sourcePos = new Vector3i(pistonX, pistonY, pistonZ);
        List<GrimPlayer> players = GrimAPI.INSTANCE.getPlayerDataManager().getEntries();

        for (GrimPlayer player : players) {
            // Skip players in unloaded chunks
            if (!player.compensatedWorld.isChunkLoaded(chunkX, chunkZ)) continue;
            
            Vector3d playerPos = player.compensatedEntities.self.trackedServerPosition.getPos();
            if (isWithinRange(sourcePos, playerPos)) {
                int lastTrans = player.lastTransactionSent.get();
                PistonData data = new PistonData(direction, boxes, lastTrans, isExtending, hasSlimeBlock, hasHoneyBlock);
                // Schedule async task to update piston data
                player.latencyUtils.addRealTimeTaskAsync(lastTrans, () -> 
                    player.compensatedWorld.activePistons.add(data));
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPistonPushEvent(BlockPistonExtendEvent event) {
        BlockFace face = BukkitConversionUtils.fromBukkitFace(event.getDirection());
        processPistonEvent(event.getBlock(), face, event.getBlocks(), true);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPistonRetractEvent(BlockPistonRetractEvent event) {
        /*
         * Important note about Bukkit's piston retract event behavior:
         * - The event is called TWICE: once without blocks, and once with blocks
         * - The direction is flipped between these two calls
         * - We handle the empty call as a special case in processPistonEvent
         * 
         * This implementation gives slightly more leniency during retraction
         * but is necessary due to Bukkit's inconsistent event firing.
         * 
         * The collision system compensates for any excess leniency by
         * checking against the piston base position.
         */
        BlockFace face = BukkitConversionUtils.fromBukkitFace(event.getDirection());
        processPistonEvent(event.getBlock(), face, event.getBlocks(), false);
    }
}
