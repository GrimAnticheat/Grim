package ac.grim.grimac.events.bukkit;

import ac.grim.grimac.GrimAC;
import ac.grim.grimac.utils.data.PistonData;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;

public class PistonEvent implements Listener {
    @EventHandler
    public void onPistonPushEvent(BlockPistonExtendEvent event) {
        GrimAC.playerGrimHashMap.values().forEach(player -> {
            if (player.compensatedWorld.isChunkLoaded(event.getBlock().getX() >> 4, event.getBlock().getZ() >> 4)) {
                player.compensatedWorld.pistonData.add(new PistonData(player, event.getDirection(), event.getBlock(), event.getBlocks(), true, player.lastTransactionAtStartOfTick));
            }
        });
    }

    @EventHandler
    public void onPistonRetractEvent(BlockPistonRetractEvent event) {
        GrimAC.playerGrimHashMap.values().forEach(player -> {
            if (player.compensatedWorld.isChunkLoaded(event.getBlock().getX() >> 4, event.getBlock().getZ() >> 4)) {
                player.compensatedWorld.pistonData.add(new PistonData(player, event.getDirection(), event.getBlock(), event.getBlocks(), false, player.lastTransactionAtStartOfTick));
            }
        });
    }
}
