package ac.grim.grimac.events.bukkit;

import ac.grim.grimac.GrimAC;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.blockdata.WrappedBlockData;
import ac.grim.grimac.utils.blockdata.types.WrappedBlockDataValue;
import ac.grim.grimac.utils.blockdata.types.WrappedDoor;
import ac.grim.grimac.utils.blockdata.types.WrappedFenceGate;
import ac.grim.grimac.utils.blockdata.types.WrappedTrapdoor;
import ac.grim.grimac.utils.blockstate.MagicBlockState;
import ac.grim.grimac.utils.data.ChangeBlockData;
import ac.grim.grimac.utils.nmsImplementations.Materials;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public class MagicPlayerBlockBreakPlace implements Listener {

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBlockPlaceEvent(BlockPlaceEvent event) {
        GrimPlayer player = GrimAC.playerGrimHashMap.get(event.getPlayer());
        if (player == null) return;
        Block block = event.getBlock();
        int materialID = block.getType().getId();
        int blockData = block.getData();

        int combinedID = materialID + (blockData << 12);

        ChangeBlockData data = new ChangeBlockData(player.lastTransactionAtStartOfTick, block.getX(), block.getY(), block.getZ(), combinedID);
        player.compensatedWorld.changeBlockQueue.add(data);

    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBlockBreakEvent(BlockBreakEvent event) {
        GrimPlayer player = GrimAC.playerGrimHashMap.get(event.getPlayer());
        if (player == null) return;
        Block block = event.getBlock();

        // Even when breaking waterlogged stuff, the client assumes it will turn into air (?)
        // So in 1.12 everything probably turns into air when broken
        ChangeBlockData data = new ChangeBlockData(player.lastTransactionAtStartOfTick, block.getX(), block.getY(), block.getZ(), 0);
        player.compensatedWorld.changeBlockQueue.add(data);
    }

    // This doesn't work perfectly, but is an attempt to support the client changing blocks from interacting with blocks
    // It also suffers the same issues as other listeners in this class, where the lastTransactionAtStartOfTick
    // doesn't actually represent when the block was applied.
    //
    // It's much better than nothing though, and works sort of fine.
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBlockInteractEvent(PlayerInteractEvent event) {
        if (event.getClickedBlock() == null) return;

        Block block = event.getClickedBlock();
        if (block != null && Materials.checkFlag(block.getType(), Materials.CLIENT_SIDE_INTERACTABLE)) {
            GrimPlayer player = GrimAC.playerGrimHashMap.get(event.getPlayer());
            if (player == null) return;

            WrappedBlockDataValue wrappedData = WrappedBlockData.getMaterialData(new MagicBlockState(block.getType().getId(), block.getData()));
            wrappedData.getWrappedData(new MagicBlockState(block.getType().getId(), block.getData()));

            if (wrappedData instanceof WrappedDoor) {
                if (!((WrappedDoor) wrappedData).isBottom()) {
                    // The block below this has the data to show if it is open
                    Block belowBlock = event.getClickedBlock().getRelative(BlockFace.DOWN);
                    ChangeBlockData data = new ChangeBlockData(player.lastTransactionAtStartOfTick, belowBlock.getX(), belowBlock.getY(), belowBlock.getZ(),
                            new MagicBlockState(belowBlock.getType().getId(), belowBlock.getData() ^ 0x4).getCombinedId());
                    player.compensatedWorld.changeBlockQueue.add(data);
                } else {
                    // If there isn't a bottom door, then it's impossible for the player to open the door
                    // If a 1.13 player is on a 1.12 server, we literally cannot store the data
                    // It's an almost unfixable false positive due to chunk storage limitations
                    //
                    // On 1.12 a door's data automatically combines with the one above or below it
                    // It just doesn't have the data required to store everything in one block
                    // Doors, trapdoors, and fence gates all use this bit to represent being open
                    // So use an xor bit operator to flip it.
                    ChangeBlockData data = new ChangeBlockData(player.lastTransactionAtStartOfTick, block.getX(), block.getY() + (((WrappedDoor) wrappedData).isBottom() ? 1 : -1), block.getZ(),
                            new MagicBlockState(block.getType().getId(), block.getData() ^ 0x4).getCombinedId());
                    player.compensatedWorld.changeBlockQueue.add(data);

                }
            }

            if (wrappedData instanceof WrappedFenceGate || wrappedData instanceof WrappedTrapdoor) {
                // See previous comment
                int newData = block.getData() ^ 0x4;

                ChangeBlockData data = new ChangeBlockData(player.lastTransactionAtStartOfTick, block.getX(), block.getY(), block.getZ(),
                        new MagicBlockState(block.getType().getId(), newData).getCombinedId());
                player.compensatedWorld.changeBlockQueue.add(data);
            }
        }
    }
}
