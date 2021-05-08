package ac.grim.grimac.events.bukkit;

import ac.grim.grimac.GrimAC;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.data.PlayerChangeBlockData;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

public class PlayerBlockBreakPlace implements Listener {
    static final BlockData airBlockData = Material.AIR.createBlockData();

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBlockPlaceEvent(BlockPlaceEvent event) {
        GrimPlayer player = GrimAC.playerGrimHashMap.get(event.getPlayer());
        Block block = event.getBlock();
        PlayerChangeBlockData data = new PlayerChangeBlockData(GrimAC.currentTick.get(), block.getX(), block.getY(), block.getZ(), block.getBlockData());
        player.changeBlockQueue.add(data);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBlockBreakEvent(BlockBreakEvent event) {
        GrimPlayer player = GrimAC.playerGrimHashMap.get(event.getPlayer());
        Block block = event.getBlock();
        PlayerChangeBlockData data = new PlayerChangeBlockData(GrimAC.currentTick.get(), block.getX(), block.getY(), block.getZ(), airBlockData);
        player.changeBlockQueue.add(data);
    }
}
