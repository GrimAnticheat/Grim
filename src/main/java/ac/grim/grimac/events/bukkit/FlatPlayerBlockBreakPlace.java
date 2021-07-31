package ac.grim.grimac.events.bukkit;

import ac.grim.grimac.GrimAC;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.data.ChangeBlockData;
import ac.grim.grimac.utils.data.PlayerChangeBlockData;
import ac.grim.grimac.utils.latency.CompensatedWorldFlat;
import ac.grim.grimac.utils.nmsImplementations.Materials;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Openable;
import org.bukkit.block.data.type.Door;
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
        GrimPlayer player = GrimAC.playerGrimHashMap.get(event.getPlayer());
        if (player == null) return;
        Block block = event.getBlock();

        PlayerChangeBlockData data = new PlayerChangeBlockData(getPlayerTransactionForPosition(player, block.getLocation()), block.getX(), block.getY(), block.getZ(), block.getBlockData());
        player.compensatedWorld.changeBlockQueue.add(data);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBlockBreakEvent(BlockBreakEvent event) {
        GrimPlayer player = GrimAC.playerGrimHashMap.get(event.getPlayer());
        if (player == null) return;
        Block block = event.getBlock();

        // Even when breaking waterlogged stuff, the client assumes it will turn into air - which is fine with me
        ChangeBlockData data = new ChangeBlockData(getPlayerTransactionForPosition(player, block.getLocation()), block.getX(), block.getY(), block.getZ(), 0);
        player.compensatedWorld.changeBlockQueue.add(data);
    }

    // This works perfectly and supports the client changing blocks from interacting with blocks
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBlockInteractEvent(PlayerInteractEvent event) {
        if (event.getClickedBlock() == null) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Block block = event.getClickedBlock();
        if (block != null && Materials.checkFlag(block.getType(), Materials.CLIENT_SIDE_INTERACTABLE)) {
            GrimPlayer player = GrimAC.playerGrimHashMap.get(event.getPlayer());
            if (player == null) return;

            BlockState state = block.getState();

            if (state.getBlockData() instanceof Door) {
                Door door = (Door) state.getBlockData();
                BlockState otherDoorState = block.getRelative(door.getHalf() == Bisected.Half.BOTTOM ? BlockFace.UP : BlockFace.DOWN).getState();

                if (otherDoorState.getBlockData() instanceof Door) {
                    Door doorAbove = (Door) otherDoorState.getBlock().getState().getBlockData();

                    // The doors are probably connected
                    if (doorAbove.getFacing() == door.getFacing() && doorAbove.isOpen() == door.isOpen()) {
                        doorAbove.setOpen(!doorAbove.isOpen());

                        ChangeBlockData data = new ChangeBlockData(getPlayerTransactionForPosition(player, event.getClickedBlock().getLocation()), block.getX(), block.getY() + (door.getHalf() == Bisected.Half.BOTTOM ? 1 : -1), block.getZ(), CompensatedWorldFlat.getFlattenedGlobalID(doorAbove));
                        player.compensatedWorld.changeBlockQueue.add(data);
                    }
                }
            }

            BlockData stateData = state.getBlockData();
            if (stateData instanceof Openable) {
                Openable openable = (Openable) stateData;
                openable.setOpen(!openable.isOpen());
            }

            ChangeBlockData data = new ChangeBlockData(getPlayerTransactionForPosition(player, event.getClickedBlock().getLocation()), block.getX(), block.getY(), block.getZ(), CompensatedWorldFlat.getFlattenedGlobalID(stateData));
            player.compensatedWorld.changeBlockQueue.add(data);
        }
    }
}
