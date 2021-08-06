package ac.grim.grimac.events.bukkit;

import ac.grim.grimac.GrimAC;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.data.ChangeBlockData;
import ac.grim.grimac.utils.data.PlayerOpenBlockData;
import ac.grim.grimac.utils.data.packetentity.latency.BlockPlayerUpdate;
import ac.grim.grimac.utils.nmsImplementations.Materials;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
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

        ChangeBlockData data = new ChangeBlockData(getPlayerTransactionForPosition(player, event.getBlockAgainst().getLocation()), block.getX(), block.getY(), block.getZ(), combinedID);
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

    // This works perfectly and supports the client changing blocks from interacting with blocks
    // This event is broken again.
    //@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBlockInteractEvent(PlayerInteractEvent event) {
        if (event.getClickedBlock() == null) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Block block = event.getClickedBlock();
        // Client side interactable -> Door, trapdoor, gate
        if (block != null && Materials.checkFlag(block.getType(), Materials.CLIENT_SIDE_INTERACTABLE)) {
            GrimPlayer player = GrimAC.playerGrimHashMap.get(event.getPlayer());
            if (player == null) return;

            PlayerOpenBlockData data = new PlayerOpenBlockData(getPlayerTransactionForPosition(player, event.getClickedBlock().getLocation()), block.getX(), block.getY(), block.getZ());
            player.compensatedWorld.openBlockData.add(data);
        }
    }
}
