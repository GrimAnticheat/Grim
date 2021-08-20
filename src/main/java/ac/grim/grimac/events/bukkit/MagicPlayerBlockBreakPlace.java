package ac.grim.grimac.events.bukkit;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.blockstate.BaseBlockState;
import ac.grim.grimac.utils.blockstate.MagicBlockState;
import ac.grim.grimac.utils.collisions.CollisionData;
import ac.grim.grimac.utils.collisions.datatypes.SimpleCollisionBox;
import ac.grim.grimac.utils.data.ChangeBlockData;
import ac.grim.grimac.utils.data.PlayerOpenBlockData;
import ac.grim.grimac.utils.data.packetentity.latency.BlockPlayerUpdate;
import ac.grim.grimac.utils.nmsImplementations.GetBoundingBox;
import ac.grim.grimac.utils.nmsImplementations.Materials;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.concurrent.ConcurrentLinkedQueue;

public class MagicPlayerBlockBreakPlace implements Listener {

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBlockPlaceEvent(BlockPlaceEvent event) {
        GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getPlayer());
        if (player == null) return;

        // This code fixes an issue where a 1.9 player places a block inside themselves
        // Required due to the following packets:
        // Client -> Server: I right-clicked a block!
        // Client: Interaction failed, not placing block (fails silently)
        // Server: You right-clicked a block?  Placing block! Block place successful because you can place blocks
        // inside yourself because of a bad paper patch.
        // GrimAC: Player placed block, add it to the world queue.
        //
        // Desync occurs because the block is added before it actually was added to the world
        // As we believe this block was placed client sided before server sided, while it is the other way around
        //
        // Also it's nice to have this patch and fix that bug :)
        Block placed = event.getBlockPlaced();
        Material type = placed.getType();
        Location location = event.getPlayer().getLocation();

        BaseBlockState magicData = new MagicBlockState(type.getId(), placed.getData());
        SimpleCollisionBox playerBox = GetBoundingBox.getBoundingBoxFromPosAndSize(location.getX(), location.getY(), location.getZ(), 0.6, 1.8);

        // isIntersected != isCollided.  Intersection means check overlap, collided also checks if equal
        if (CollisionData.getData(type).getMovementCollisionBox(player, player.getClientVersion(), magicData, placed.getX(), placed.getY(), placed.getZ()).isIntersected(playerBox)) {
            event.setCancelled(true);
            return;
        }

        Block block = event.getBlock();
        int materialID = block.getType().getId();
        int blockData = block.getData();

        int combinedID = materialID + (blockData << 12);

        ChangeBlockData data = new ChangeBlockData(getPlayerTransactionForPosition(player, event.getBlockAgainst().getLocation(), player.compensatedWorld.packetBlockPlaces), block.getX(), block.getY(), block.getZ(), combinedID);
        player.compensatedWorld.worldChangedBlockQueue.add(data);
    }

    public static int getPlayerTransactionForPosition(GrimPlayer player, Location location, ConcurrentLinkedQueue<BlockPlayerUpdate> updates) {
        int transaction = player.lastTransactionAtStartOfTick;

        for (BlockPlayerUpdate update : updates) {
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
        GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getPlayer());
        if (player == null) return;
        Block block = event.getBlock();

        // Even when breaking waterlogged stuff, the client assumes it will turn into air (?)
        // So in 1.12 everything probably turns into air when broken
        ChangeBlockData data = new ChangeBlockData(getPlayerTransactionForPosition(player, block.getLocation(), player.compensatedWorld.packetBlockBreaks), block.getX(), block.getY(), block.getZ(), 0);
        player.compensatedWorld.worldChangedBlockQueue.add(data);
    }

    // This works perfectly and supports the client changing blocks from interacting with blocks
    // This event is broken again.
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBlockInteractEvent(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getPlayer());
        if (player == null) return;

        Block block = event.getClickedBlock();
        if (block == null) return;

        // Client side interactable -> Door, trapdoor, gate
        if (Materials.checkFlag(block.getType(), Materials.CLIENT_SIDE_INTERACTABLE)) {
            PlayerOpenBlockData data = new PlayerOpenBlockData(getPlayerTransactionForPosition(player, event.getClickedBlock().getLocation(), player.compensatedWorld.packetBlockPlaces), block.getX(), block.getY(), block.getZ());
            player.compensatedWorld.worldChangedBlockQueue.add(data);
        }
    }
}
