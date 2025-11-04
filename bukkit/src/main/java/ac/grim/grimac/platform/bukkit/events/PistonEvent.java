package ac.grim.grimac.platform.bukkit.events;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.platform.bukkit.utils.convert.BukkitConversionUtils;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.collisions.datatypes.SimpleCollisionBox;
import ac.grim.grimac.utils.data.PistonData;
import ac.grim.grimac.utils.data.TrackedPosition;
import com.github.retrooper.packetevents.protocol.world.BlockFace;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;

import java.util.ArrayList;
import java.util.List;

public class PistonEvent implements Listener {

    private final Material SLIME_BLOCK = Material.getMaterial("SLIME_BLOCK");
    private final Material HONEY_BLOCK = Material.getMaterial("HONEY_BLOCK");

    private static final double MAX_HORIZONTAL_DISTANCE = 24.0;
    private static final double MAX_VERTICAL_DISTANCE = 64.0;

    // accuracy isn't that important, it's close enough and performant
    private static boolean isCloseEnough(final int aX, final int aY, final int aZ,
                                         final double bX, final double bY, final double bZ) {
        return Math.abs(aX - bX) <= MAX_HORIZONTAL_DISTANCE
                && Math.abs(aY - bY) <= MAX_VERTICAL_DISTANCE
                && Math.abs(aZ - bZ) <= MAX_HORIZONTAL_DISTANCE;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPistonPushEvent(BlockPistonExtendEvent event) {
        boolean hasSlimeBlock = false;
        boolean hasHoneyBlock = false;

        List<SimpleCollisionBox> boxes = new ArrayList<>();
        for (Block block : event.getBlocks()) {
            boxes.add(new SimpleCollisionBox(0, 0, 0, 1, 1, 1, true)
                    .offset(block.getX(),
                            block.getY(),
                            block.getZ()));
            boxes.add(new SimpleCollisionBox(0, 0, 0, 1, 1, 1, true)
                    .offset(block.getX() + event.getDirection().getModX(),
                            block.getY() + event.getDirection().getModY(),
                            block.getZ() + event.getDirection().getModZ()));

            // Support honey block like this because ViaVersion replacement
            if (block.getType() == SLIME_BLOCK) {
                hasSlimeBlock = true;
            }

            if (block.getType() == HONEY_BLOCK) {
                hasHoneyBlock = true;
            }
        }

        Block piston = event.getBlock();

        // Add bounding box of the actual piston head pushing
        boxes.add(new SimpleCollisionBox(0, 0, 0, 1, 1, 1, true)
                .offset(piston.getX() + event.getDirection().getModX(),
                        piston.getY() + event.getDirection().getModY(),
                        piston.getZ() + event.getDirection().getModZ()));

        final int chunkX = event.getBlock().getX() >> 4;
        final int chunkZ = event.getBlock().getZ() >> 4;
        final BlockFace blockFace = BukkitConversionUtils.fromBukkitFace(event.getDirection());
        final int sourceX = piston.getX();
        final int sourceY = piston.getY();
        final int sourceZ = piston.getZ();

        for (GrimPlayer player : GrimAPI.INSTANCE.getPlayerDataManager().getEntries()) {
            TrackedPosition position = player.compensatedEntities.self.trackedServerPosition;
            if (player.compensatedWorld.isChunkLoaded(chunkX, chunkZ) && isCloseEnough(sourceX, sourceY, sourceZ, position.getX(), position.getY(), position.getZ())) {
                final int lastTrans = player.lastTransactionSent.get();
                PistonData data = new PistonData(blockFace, boxes, lastTrans, true, hasSlimeBlock, hasHoneyBlock);
                player.latencyUtils.addRealTimeTaskAsync(lastTrans, () -> player.compensatedWorld.activePistons.add(data));
            }
        }
    }

    // For some unknown reason, bukkit handles this stupidly
    // Calls the event once without blocks
    // Calls it again with blocks -
    // This wouldn't be an issue if it didn't flip the direction of the event
    // What a stupid system, again I can stand mojang doing stupid stuff but not other mod makers
    //
    // This gives too much of a lenience when retracting
    // But as this is insanely gitchy due to bukkit I don't care.
    // The lenience is never actually given because of collisions hitting the piston base
    // Blocks outside the piston head give only as much lenience as needed
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPistonRetractEvent(BlockPistonRetractEvent event) {
        boolean hasSlimeBlock = false;
        boolean hasHoneyBlock = false;

        List<SimpleCollisionBox> boxes = new ArrayList<>();
        BlockFace face = BukkitConversionUtils.fromBukkitFace(event.getDirection());

        // The event was called without blocks and is therefore in the right direction
        if (event.getBlocks().isEmpty()) {
            Block piston = event.getBlock();

            // Add bounding box of the actual piston head pushing
            boxes.add(new SimpleCollisionBox(0, 0, 0, 1, 1, 1, true)
                    .offset(piston.getX() + face.getModX(),
                            piston.getY() + face.getModY(),
                            piston.getZ() + face.getModZ()));
        }

        for (Block block : event.getBlocks()) {
            boxes.add(new SimpleCollisionBox(0, 0, 0, 1, 1, 1, true)
                    .offset(block.getX(), block.getY(), block.getZ()));
            boxes.add(new SimpleCollisionBox(0, 0, 0, 1, 1, 1, true)
                    .offset(block.getX() + face.getModX(), block.getY() + face.getModY(), block.getZ() + face.getModZ()));

            // Support honey block like this because ViaVersion replacement
            if (block.getType() == SLIME_BLOCK) {
                hasSlimeBlock = true;
            }

            if (block.getType() == HONEY_BLOCK) {
                hasHoneyBlock = true;
            }
        }

        final Block block = event.getBlock();
        final int chunkX = block.getX() >> 4;
        final int chunkZ = block.getZ() >> 4;
        final int sourceX = block.getX();
        final int sourceY = block.getY();
        final int sourceZ = block.getZ();

        for (GrimPlayer player : GrimAPI.INSTANCE.getPlayerDataManager().getEntries()) {
            TrackedPosition position = player.compensatedEntities.self.trackedServerPosition;
            if (player.compensatedWorld.isChunkLoaded(chunkX, chunkZ) && isCloseEnough(sourceX, sourceY, sourceZ, position.getX(), position.getY(), position.getZ())) {
                int lastTrans = player.lastTransactionSent.get();
                PistonData data = new PistonData(face, boxes, lastTrans, false, hasSlimeBlock, hasHoneyBlock);
                player.latencyUtils.addRealTimeTaskAsync(lastTrans, () -> player.compensatedWorld.activePistons.add(data));
            }
        }
    }
}
