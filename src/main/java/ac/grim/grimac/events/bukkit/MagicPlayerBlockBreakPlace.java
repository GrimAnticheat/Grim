package ac.grim.grimac.events.bukkit;

import ac.grim.grimac.GrimAC;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.data.ChangeBlockData;
import ac.grim.grimac.utils.data.packetentity.latency.BlockPlayerUpdate;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

public class MagicPlayerBlockBreakPlace implements Listener {

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBlockPlaceEvent(BlockPlaceEvent event) {
        GrimPlayer player = GrimAC.playerGrimHashMap.get(event.getPlayer());
        if (player == null) return;
        Block block = event.getBlock();
        int materialID = block.getType().getId();
        int blockData = block.getData();

        int combinedID = materialID + (blockData << 12);

        // It can take two ticks for the block place packet to be processed
        // Better to be one tick early than one tick late for block placing
        // as the player can't place a block inside themselves
        ChangeBlockData data = new ChangeBlockData(getPlayerTransactionForPosition(player, block.getLocation()), block.getX(), block.getY(), block.getZ(), combinedID);
        player.compensatedWorld.changeBlockQueue.add(data);

    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBlockBreakEvent(BlockBreakEvent event) {
        GrimPlayer player = GrimAC.playerGrimHashMap.get(event.getPlayer());
        if (player == null) return;
        Block block = event.getBlock();

        // Even when breaking waterlogged stuff, the client assumes it will turn into air (?)
        // So in 1.12 everything probably turns into air when broken
        ChangeBlockData data = new ChangeBlockData(getPlayerTransactionForPosition(player, block.getLocation()), block.getX(), block.getY(), block.getZ(), 0);
        player.compensatedWorld.changeBlockQueue.add(data);
    }

    public static int getPlayerTransactionForPosition(GrimPlayer player, Location location) {
        int transaction = player.lastTransactionAtStartOfTick;
        for (BlockPlayerUpdate update : player.compensatedWorld.packetBlockPositions) {
            if (update.position.getX() == location.getBlockX()
                    && update.position.getY() == location.getBlockY()
                    && update.position.getZ() == location.getBlockZ()) {
                transaction = update.transaction;
            }
        }

        return transaction;
    }
}
