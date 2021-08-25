package ac.grim.grimac.events.bukkit;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.data.ChangeBlockData;
import ac.grim.grimac.utils.data.PlayerChangeBlockData;
import ac.grim.grimac.utils.data.PlayerOpenBlockData;
import ac.grim.grimac.utils.nmsImplementations.Materials;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import static ac.grim.grimac.events.bukkit.MagicPlayerBlockBreakPlace.getPlayerTransactionForPosition;

public class FlatPlayerBlockBreakPlace implements Listener {

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBlockPlaceEvent(BlockPlaceEvent event) {
        GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getPlayer());
        if (player == null) return;
        Block block = event.getBlock();

        PlayerChangeBlockData data = new PlayerChangeBlockData(getPlayerTransactionForPosition(player, event.getBlockAgainst().getLocation(), player.compensatedWorld.packetBlockPlaces), block.getX(), block.getY(), block.getZ(), block.getBlockData());
        player.compensatedWorld.worldChangedBlockQueue.add(data);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBlockBreakEvent(BlockBreakEvent event) {
        GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getPlayer());
        if (player == null) return;
        Block block = event.getBlock();

        // Even when breaking waterlogged stuff, the client assumes it will turn into air - which is fine with me
        ChangeBlockData data = new ChangeBlockData(getPlayerTransactionForPosition(player, block.getLocation(), player.compensatedWorld.packetBlockBreaks), block.getX(), block.getY(), block.getZ(), 0);
        player.compensatedWorld.worldChangedBlockQueue.add(data);
    }

    // This works perfectly and supports the client changing blocks from interacting with blocks
    // This event is broken again.
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBlockInteractEvent(PlayerInteractEvent event) {
        GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getPlayer());
        if (player == null) return;

        Block block = event.getClickedBlock();
        if (block == null) return;

        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        // Client side interactable -> Door, trapdoor, gate
        if (Materials.checkFlag(block.getType(), Materials.CLIENT_SIDE_INTERACTABLE)) {
            PlayerOpenBlockData data = new PlayerOpenBlockData(getPlayerTransactionForPosition(player, event.getClickedBlock().getLocation(), player.compensatedWorld.packetBlockPlaces), block.getX(), block.getY(), block.getZ());
            player.compensatedWorld.worldChangedBlockQueue.add(data);
        }
    }
}
