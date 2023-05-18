package ac.grim.grimac.events.packets;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.checks.impl.prediction.OffsetHandler;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.BlockPlace;
import ac.grim.grimac.utils.collisions.datatypes.SimpleCollisionBox;
import ac.grim.grimac.utils.data.Pair;
import ac.grim.grimac.utils.data.ShulkerData;
import ac.grim.grimac.utils.math.VectorUtils;
import ac.grim.grimac.utils.nmsutil.Materials;
import ac.grim.grimac.utils.nmsutil.Ray;
import ac.grim.grimac.utils.nmsutil.ReachUtils;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.world.BlockFace;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.protocol.world.states.type.StateType;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.util.Vector3f;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerBlockAction;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerCloseWindow;
import com.google.common.collect.Sets;
import io.github.retrooper.packetevents.util.FoliaCompatUtil;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.BlockIterator;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.*;

// If a player doesn't get this packet, then they don't know the shulker box is currently opened
// Meaning if a player enters a chunk with an opened shulker box, they see the shulker box as closed.
//
// Exempting the player on shulker boxes is an option... but then you have people creating PvP arenas
// on shulker boxes to get high lenience.
//
public class PacketBlockAction extends PacketListenerAbstract {
    public PacketBlockAction() {
        super(PacketListenerPriority.HIGH);
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (event.getPacketType() == PacketType.Play.Server.BLOCK_ACTION) {
            GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getUser());
            if (player == null) return;
            WrapperPlayServerBlockAction blockAction = new WrapperPlayServerBlockAction(event);
            Vector3i blockPos = blockAction.getBlockPosition();
            player.latencyUtils.addRealTimeTask(player.lastTransactionSent.get(), () -> {
                // The client ignores the state sent to the client.
                WrappedBlockState existing = player.compensatedWorld.getWrappedBlockStateAt(blockPos);
                detectThroughWalls(player, existing, blockPos, blockAction);
                if (Materials.isShulker(existing.getType())) {
                    // Param is the number of viewers of the shulker box.
                    // Hashset with .equals() set to be position
                    if (blockAction.getActionData() >= 1) {
                        ShulkerData data = new ShulkerData(blockPos, player.lastTransactionSent.get(), false);
                        player.compensatedWorld.openShulkerBoxes.remove(data);
                        player.compensatedWorld.openShulkerBoxes.add(data);
                    } else {
                        // The shulker box is closing
                        ShulkerData data = new ShulkerData(blockPos, player.lastTransactionSent.get(), true);
                        player.compensatedWorld.openShulkerBoxes.remove(data);
                        player.compensatedWorld.openShulkerBoxes.add(data);
                    }
                }
            });
        }
    }

    private boolean detectThroughWalls(GrimPlayer player, WrappedBlockState existing,
                                       Vector3i blockPos, WrapperPlayServerBlockAction blockAction) {
        if (!Materials.isWoodenChest(existing.getType())) return false;
        if (blockAction.getActionData() != 1) return false; //open inv
        float additionalY = (float) player.bukkitPlayer.getEyeHeight();
        int chestsFound = 0;
        boolean chestInLine = didRayTraceHit(player, new SimpleCollisionBox(blockPos));

        for (int mode = 0; mode < 3; mode++) {
            Pair<Boolean, Boolean> result = isInvalidChests(getChestLine(player,
                    6,2, additionalY, mode));
            if (!result.getSecond() && chestInLine) return false;
            if (result.getFirst()) chestsFound += 1;
        }
        for (Double possibleEyeHeight : player.getPossibleEyeHeights()) {
            Pair<Boolean, Boolean> result = isInvalidChests(getChestLine(player,
                    6,2, possibleEyeHeight.floatValue(), 0));
            if (!result.getSecond() && chestInLine) return false;
            if (result.getFirst()) chestsFound += 1;
        }
        boolean lagSpike = player.getTransactionPing() > 500;
        if (chestsFound > 0 && !chestInLine) { //opening another chest (legit loot through walls)
            if (!lagSpike) addViolation(player, "HandNoClip Legit (" + chestsFound + ")");
            closeInventory(player); //just cancel open
        } else if (chestsFound > 0) { //looking at chest. 100% loot through walls
            if (!lagSpike) addViolation(player, "HandNoClip (" + chestsFound + ")");
            closeInventory(player);
        } else {
            //not looking at chest at all (ping, TPS, legit NoClip)
            Vector vec = player.bukkitPlayer.getLocation().toVector().subtract(
                    new Vector(blockPos.x, blockPos.y, blockPos.z)).multiply(-1);
            boolean possibleReach = !isInvalidChests(getChestLine(player.bukkitPlayer.getWorld(),
                    player.bukkitPlayer.getLocation().toVector(), vec,
                    6,2, 0)).getSecond();

            if (possibleReach) return false; //ping or TPS compensation
            if (!lagSpike) addViolation(player, "HandNoClip Legit");
            closeInventory(player);
        }
        return true;
    }

    private void closeInventory(GrimPlayer player) {
        FoliaCompatUtil.runTaskForEntity(player.bukkitPlayer, GrimAPI.INSTANCE.getPlugin(), () -> {
            player.bukkitPlayer.closeInventory();
        }, null, 0);
    }

    private void addViolation(GrimPlayer player, String reason) {
        OffsetHandler offsetHandler = player.checkManager.getOffsetHandler();
        offsetHandler.flagAndAlert(reason);
    }
    private boolean didRayTraceHit(GrimPlayer player, SimpleCollisionBox box) {
        List<Vector3f> possibleLookDirs = new ArrayList<>(Arrays.asList(
                new Vector3f(player.lastXRot, player.yRot, 0),
                new Vector3f(player.xRot, player.yRot, 0)
        ));

        // Start checking if player is in the block
        double minEyeHeight = Collections.min(player.getPossibleEyeHeights());
        double maxEyeHeight = Collections.max(player.getPossibleEyeHeights());

        SimpleCollisionBox eyePositions = new SimpleCollisionBox(player.x, player.y + minEyeHeight, player.z, player.x, player.y + maxEyeHeight, player.z);
        eyePositions.expand(player.getMovementThreshold());

        // If the player is inside a block, then they can ray trace through the block and hit the other side of the block
        if (eyePositions.isIntersected(box)) {
            return true;
        }
        // End checking if the player is in the block

        // 1.9+ players could be a tick behind because we don't get skipped ticks
        if (player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_9)) {
            possibleLookDirs.add(new Vector3f(player.lastXRot, player.lastYRot, 0));
        }

        // 1.7 players do not have any of these issues! They are always on the latest look vector
        if (player.getClientVersion().isOlderThan(ClientVersion.V_1_8)) {
            possibleLookDirs = Collections.singletonList(new Vector3f(player.xRot, player.yRot, 0));
        }

        for (double d : player.getPossibleEyeHeights()) {
            for (Vector3f lookDir : possibleLookDirs) {
                // x, y, z are correct for the block placement even after post tick because of code elsewhere
                Vector3d starting = new Vector3d(player.x, player.y + d, player.z);
                // xRot and yRot are a tick behind
                Ray trace = new Ray(player, starting.getX(), starting.getY(), starting.getZ(), lookDir.getX(), lookDir.getY());
                Pair<Vector, BlockFace> intercept = ReachUtils.calculateIntercept(box, trace.getOrigin(), trace.getPointAtDistance(6));

                if (intercept.getFirst() != null) return true;
            }
        }

        return false;
    }
    private Pair<Boolean, Boolean> isInvalidChests(List<Material> lastTwoBlocks) {
        boolean hasChest = lastTwoBlocks.contains(Material.CHEST) ||
                lastTwoBlocks.contains(Material.TRAPPED_CHEST) ||
                lastTwoBlocks.contains(Material.ENDER_CHEST) ||
                lastTwoBlocks.contains(Material.SHULKER_BOX) ||
                lastTwoBlocks.contains(Material.BARREL); //TODO add version compatibility
        return new Pair<>(hasChest, !hasChest || lastTwoBlocks.size() == 2);
    }
    private static final Set<Material> transparent = Sets.newHashSet(Material.AIR,
            Material.CAVE_AIR,
            Material.VOID_AIR); //TODO add version compatibility

    private List<Material> getChestLine(GrimPlayer player, int maxDistance, int maxLength,
                                        float addY, int mode) {
        float xRot = mode == 0 ? player.lastXRot :
                mode == 1 ? player.xRot :
                        averageYaw(player.lastXRot, player.xRot); //TODO
        float yRot = mode == 0 ? player.lastYRot : mode == 1 ? player.yRot : (player.lastYRot + player.yRot) / 2;

        double X = mode == 0 ? player.lastX :
                mode == 1 ? player.x :
                        player.lastX - (player.x - player.lastX);
        double Y = mode == 0 ? player.lastY :
                mode == 1 ? player.y :
                        player.lastY - (player.y - player.lastY);
        double Z = mode == 0 ? player.lastZ :
                mode == 1 ? player.z :
                        player.lastZ - (player.z - player.lastZ);
        return getChestLine(player.bukkitPlayer.getWorld(), new Vector(X, Y, Z),
                getLastDirection(xRot, yRot), maxDistance, maxLength, addY);
    }

    private List<Material> getChestLine(World world, Vector start, Vector direction, int maxDistance, int maxLength,
                                        float addY) {
        List<Material> blocks = new LinkedList<>();
        Iterator<Block> itr = new BlockIterator(world, start, direction, addY, maxDistance);
        int preLast = 0;
        while (itr.hasNext()) {
            Block block = itr.next();
            block.getFace(block);
            Material material = block.getType();
            if (material.toString().contains("SHULKER_BOX")) material = Material.SHULKER_BOX;
            boolean isChest = material == Material.CHEST ||
                    material == Material.TRAPPED_CHEST ||
                    material == Material.ENDER_CHEST ||
                    material == Material.SHULKER_BOX ||
                    material == Material.BARREL; //TODO add version compatibility
            if (isChest) preLast++;
            if (!transparent.contains(material) &&
                    material.isOccluding() || isChest) {
                blocks.add(material);
                if (maxLength != 0 && blocks.size() > maxLength) {
                    blocks.remove(0);
                }
                if (preLast >= 1) break;
            }
        }
        return blocks;
    }

    public float averageYaw(float firstYaw, float secondYaw) {
        boolean equals = firstYaw < 0 && secondYaw < 0 || firstYaw > 0 && secondYaw > 0;
        if (equals) return (firstYaw + secondYaw) / 2;
        if (firstYaw < 0) firstYaw += 360;
        else secondYaw += 360;
        return (firstYaw + secondYaw) / 2;
    }
    @NotNull
    public Vector getLastDirection(float yaw, float pitch) {
        Vector vector = new Vector();

        double rotX = yaw;
        double rotY = pitch;

        vector.setY(-Math.sin(Math.toRadians(rotY)));

        double xz = Math.cos(Math.toRadians(rotY));

        vector.setX(-xz * Math.sin(Math.toRadians(rotX)));
        vector.setZ(xz * Math.cos(Math.toRadians(rotX)));

        return vector;
    }
}
