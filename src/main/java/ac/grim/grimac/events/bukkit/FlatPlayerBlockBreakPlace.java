package ac.grim.grimac.events.bukkit;

import ac.grim.grimac.GrimAC;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.data.ChangeBlockData;
import ac.grim.grimac.utils.latency.CompensatedWorld;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

public class FlatPlayerBlockBreakPlace implements Listener {

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBlockPlaceEvent(BlockPlaceEvent event) {
        GrimPlayer player = GrimAC.playerGrimHashMap.get(event.getPlayer());
        if (player == null) return;
        Block block = event.getBlock();
        ChangeBlockData data = new ChangeBlockData(player.packetStateData.packetLastTransactionReceived, block.getX(), block.getY(), block.getZ(), CompensatedWorld.getFlattenedGlobalID(block.getBlockData()));
        player.compensatedWorld.changeBlockQueue.add(data);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBlockBreakEvent(BlockBreakEvent event) {
        GrimPlayer player = GrimAC.playerGrimHashMap.get(event.getPlayer());
        if (player == null) return;
        Block block = event.getBlock();

        // Even when breaking waterlogged stuff, the client assumes it will turn into air (?)
        ChangeBlockData data = new ChangeBlockData(player.packetStateData.packetLastTransactionReceived, block.getX(), block.getY(), block.getZ(), 0);
        player.compensatedWorld.changeBlockQueue.add(data);
    }
}
