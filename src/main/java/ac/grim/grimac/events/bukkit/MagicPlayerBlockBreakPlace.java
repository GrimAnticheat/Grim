package ac.grim.grimac.events.bukkit;

import ac.grim.grimac.GrimAC;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.data.ChangeBlockData;
import io.github.retrooper.packetevents.utils.nms.NMSUtils;
import io.github.retrooper.packetevents.utils.reflection.Reflection;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class MagicPlayerBlockBreakPlace implements Listener {
    private static final Method getTypeId;

    static {
        getTypeId = Reflection.getMethod(NMSUtils.blockClass, "getTypeId", int.class);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBlockPlaceEvent(BlockPlaceEvent event) {
        try {
            GrimPlayer player = GrimAC.playerGrimHashMap.get(event.getPlayer());
            Block block = event.getBlock();
            int materialID = (int) getTypeId.invoke(block);
            int blockData = block.getData();

            int combinedID = materialID + (blockData << 12);

            ChangeBlockData data = new ChangeBlockData(GrimAC.currentTick.get(), block.getX(), block.getY(), block.getZ(), combinedID);
            player.compensatedWorld.changeBlockQueue.add(data);
        } catch (InvocationTargetException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBlockBreakEvent(BlockBreakEvent event) {
        try {
            GrimPlayer player = GrimAC.playerGrimHashMap.get(event.getPlayer());
            Block block = event.getBlock();
            int materialID = (int) getTypeId.invoke(block);
            int blockData = block.getData();

            int combinedID = materialID + (blockData << 12);

            ChangeBlockData data = new ChangeBlockData(GrimAC.currentTick.get(), block.getX(), block.getY(), block.getZ(), combinedID);
            player.compensatedWorld.changeBlockQueue.add(data);
        } catch (InvocationTargetException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }
}
