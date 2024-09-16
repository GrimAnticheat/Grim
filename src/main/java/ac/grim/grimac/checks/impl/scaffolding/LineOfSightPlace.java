package ac.grim.grimac.checks.impl.scaffolding;

import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.BlockPlaceCheck;
import ac.grim.grimac.events.packets.CheckManagerListener;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.BlockPlace;
import ac.grim.grimac.utils.collisions.datatypes.SimpleCollisionBox;
import ac.grim.grimac.utils.data.HitData;
import ac.grim.grimac.utils.data.Pair;
import ac.grim.grimac.utils.nmsutil.Ray;
import com.github.retrooper.packetevents.protocol.attribute.Attributes;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.protocol.world.BlockFace;
import com.github.retrooper.packetevents.protocol.world.states.type.StateType;
import com.github.retrooper.packetevents.protocol.world.states.type.StateTypes;
import com.github.retrooper.packetevents.util.Vector3f;
import com.github.retrooper.packetevents.util.Vector3i;
import org.bukkit.util.Vector;

import java.util.*;

@CheckData(name = "LineOfSightPlace")
public class LineOfSightPlace extends BlockPlaceCheck {

    private double flagBuffer = 0; // If the player flags once, force them to play legit, or we will cancel the tick before.
    private boolean ignorePost = false;
    private boolean useBlockWhitelist;
    private HashSet<StateType> blockWhitelist;

    public LineOfSightPlace(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onBlockPlace(final BlockPlace place) {
        if (checkIfShouldSkip(place)) return;

        if (flagBuffer > 0 && !didRayTraceHit(place)) {
            ignorePost = true;
            // If the player hit and has flagged this check recently
            if (flagAndAlert("pre-flying: " + player.compensatedWorld.getWrappedBlockStateAt(place.getPlacedAgainstBlockLocation()).getType()) && shouldModifyPackets() && shouldCancel()) {
                place.resync();  // Deny the block placement.
            }
        }
    }

    // Use post flying because it has the correct rotation, and can't false easily.
    @Override
    public void onPostFlyingBlockPlace(BlockPlace place) {
        if (checkIfShouldSkip(place)) return;

        // Don't flag twice
        if (ignorePost) {
            ignorePost = false;
            return;
        }

        // Ray trace to try and hit the target block.
        boolean hit = didRayTraceHit(place);
        // This can false with rapidly moving yaw in 1.8+ clients
        if (!hit) {
            flagBuffer = 1;
            flagAndAlert("post-flying: " + player.compensatedWorld.getWrappedBlockStateAt(place.getPlacedAgainstBlockLocation()).getType());
        } else {
            flagBuffer = Math.max(0, flagBuffer - 0.1);
        }
    }

    private boolean checkIfShouldSkip(BlockPlace place) {
        StateType targetBlockStateType = player.compensatedWorld.getWrappedBlockStateAt(place.getPlacedAgainstBlockLocation()).getType();
        if (place.getMaterial() == StateTypes.SCAFFOLDING) return true; // Scaffolding
        if (player.gamemode == GameMode.SPECTATOR) return true; // A waste to check creative mode players
        if (targetBlockStateType == StateTypes.REDSTONE_WIRE) return true; // Redstone too buggy
        if (player.compensatedWorld.isNearHardEntity(player.boundingBox.copy().expand(4))) return true; // Shulkers and Pistons are too buggy

        if (useBlockWhitelist) {
            if (!isBlockTypeWhitelisted(targetBlockStateType)) {
                return true;
            }
        }
        return false;
    }

    private boolean didRayTraceHit(BlockPlace place) {
        double[] possibleEyeHeights = player.getPossibleEyeHeightsArray();

        // Start checking if player is in the block
        double minEyeHeight = Double.MAX_VALUE;
        double maxEyeHeight = Double.MIN_VALUE;
        for (double height : possibleEyeHeights) {
            minEyeHeight = Math.min(minEyeHeight, height);
            maxEyeHeight = Math.max(maxEyeHeight, height);
        }

        double movementThreshold = player.getMovementThreshold();

        SimpleCollisionBox eyePositions = new SimpleCollisionBox(player.x, player.y + minEyeHeight, player.z, player.x, player.y + maxEyeHeight, player.z);
        eyePositions.expand(movementThreshold);

        Vector3i interactBlockVec = place.getPlacedAgainstBlockLocation();
        BlockFace expectedBlockFace = place.getDirection();

        // If the player is inside a block, then they can ray trace through the block and hit the other side of the block
        // This may potentially be exploitable as a minor bypass
        if (eyePositions.isIntersected(new SimpleCollisionBox(interactBlockVec))) {
            return true;
        }
        // End checking if the player is in the block
        double[][] possibleLookDirs;

        // 1.9+ players could be a tick behind because we don't get skipped ticks
        if (player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_9)) {
            possibleLookDirs = new double[][]{
                    {player.lastXRot, player.yRot, 0},
                    {player.xRot, player.yRot, 0},
                    {player.lastXRot, player.lastYRot, 0}
            };
        } else if (player.getClientVersion().isOlderThan(ClientVersion.V_1_8)) {
            // 1.7 players do not have any of these issues! They are always on the latest look vector
            possibleLookDirs = new double[][]{{player.xRot, player.yRot, 0}};
        } else {
            possibleLookDirs = new double[][]{
                    {player.lastXRot, player.yRot, 0},
                    {player.xRot, player.yRot, 0}
            };
        }

        // We do not need to add 0.03/0.0002 to maxDistance to ensure our raytrace hits blocks
        // Since we expand the hitboxes of the expectedTargetBlock by 0.03/0.002 already later
        double maxDistance = player.compensatedEntities.getSelf().getAttributeValue(Attributes.PLAYER_BLOCK_INTERACTION_RANGE);

        // Define possible offsets
        // TODO, vectorize this with SIMD or AVX for performance
        double[][] offsets = {
                {0, 0, 0},
                {movementThreshold, 0, 0},
                {-movementThreshold, 0, 0},
                {0, movementThreshold, 0},
                {0, -movementThreshold, 0},
                {0, 0, movementThreshold},
                {0, 0, -movementThreshold}
        };

        double[] eyePosition = new double[3];
        double[] eyeLookDir = new double[3];

        for (double eyeHeight : possibleEyeHeights) {
            for (double[] lookDir : possibleLookDirs) {
                for (double[] offset : offsets) {
                    eyePosition[0] = player.x + offset[0];
                    eyePosition[1] = player.y + eyeHeight + offset[1];
                    eyePosition[2] = player.z + offset[2];

                    calculateDirection(eyeLookDir, lookDir[0], lookDir[1]);

                    if (getTargetBlock(eyePosition, eyeLookDir, maxDistance, interactBlockVec, expectedBlockFace)) {
                        return true; // If any possible face matches the client-side placement, assume it's legitimate
                    }
                }
            }
        }

        return false; // No matching face found
    }

    // Helper method to calculate direction (replace the Ray class method)
    private void calculateDirection(double[] result, double xRot, double yRot) {
        float rotX = (float) Math.toRadians(xRot);
        float rotY = (float) Math.toRadians(yRot);
        result[1] = -player.trigHandler.sin(rotY);
        double xz = player.trigHandler.cos(rotY);
        result[0] = -xz * player.trigHandler.sin(rotX);
        result[2] = xz * player.trigHandler.cos(rotX);
    }

    private boolean getTargetBlock(double[] eyePosition, double[] eyeDirection, double maxDistance, Vector3i targetBlockVec, BlockFace expectedBlockFace) {
        HitData hitData = CheckManagerListener.getNearestReachHitResult(player, eyePosition, eyeDirection, maxDistance, maxDistance, targetBlockVec, expectedBlockFace);
        return hitData != null  // Player is inside the block, from expanded hitbox, do we still need this?
                && targetBlockVec.equals(hitData.getPosition());
    }

    private boolean isBlockTypeWhitelisted(StateType type) {
        return blockWhitelist.contains(type);
    }

    @Override
    public void reload() {
        super.reload();

        useBlockWhitelist = getConfig().getBooleanElse("LineOfSightPlace.use-block-whitelist", false);
        blockWhitelist = new HashSet<>();
        List<String> blocks = getConfig().getList("LineOfSightPlace.block-whitelist");
        for (String block : blocks) {
            blockWhitelist.add(StateTypes.getByName(block));
        }
    }
}
