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
import ac.grim.grimac.utils.latency.CompensatedWorld;
import ac.grim.grimac.utils.nmsImplementations.Materials;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Openable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.BitSet;

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
}
