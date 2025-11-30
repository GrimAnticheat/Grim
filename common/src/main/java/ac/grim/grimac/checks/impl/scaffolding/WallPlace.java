package ac.grim.grimac.checks.impl.scaffolding;

import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.BlockPlaceCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.BlockPlace;
import ac.grim.grimac.utils.collisions.datatypes.SimpleCollisionBox;
import ac.grim.grimac.utils.data.Pair;
import ac.grim.grimac.utils.math.Vector3dm;
import ac.grim.grimac.utils.nmsutil.Ray;
import ac.grim.grimac.utils.nmsutil.ReachUtils;
import com.github.retrooper.packetevents.protocol.attribute.Attributes;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.protocol.world.BlockFace;
import com.github.retrooper.packetevents.protocol.world.states.type.StateTypes;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.util.Vector3f;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@CheckData(name = "WallPlace", description = "Placed a block through a wall", experimental = true)
public class WallPlace extends BlockPlaceCheck {
    private double flagBuffer = 0;
    private boolean ignorePost = false;
    private static final double STEP_SIZE = 0.1;

    public WallPlace(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onBlockPlace(final BlockPlace place) {
        if (place.material == StateTypes.SCAFFOLDING) return;
        if (player.gamemode == GameMode.SPECTATOR) return;
        if (player.inVehicle()) return;
        
        boolean hasWall = hasWallBetweenPlayerAndBlock(place);
        
        if (flagBuffer > 0 && hasWall) {
            ignorePost = true;
            if (flagAndAlert("pre-flying") && shouldModifyPackets() && shouldCancel()) {
                place.resync();
            }
        }
    }

    @Override
    public void onPostFlyingBlockPlace(BlockPlace place) {
        if (place.material == StateTypes.SCAFFOLDING) return;
        if (player.gamemode == GameMode.SPECTATOR) return;
        if (player.inVehicle()) return;

        if (ignorePost) {
            ignorePost = false;
            return;
        }

        boolean hasWall = hasWallBetweenPlayerAndBlock(place);
        
        if (hasWall) {
            flagBuffer = Math.min(10, flagBuffer + 1);
            if (flagAndAlert("through-wall") && shouldModifyPackets() && shouldCancel()) {
                place.resync();
            }
        } else {
            flagBuffer = Math.max(0, flagBuffer - 0.25);
        }
    }

    private boolean hasWallBetweenPlayerAndBlock(BlockPlace place) {
        final double[] possibleEyeHeights = player.getPossibleEyeHeights();
        
        SimpleCollisionBox targetBox = new SimpleCollisionBox(place.position);
        
        final double distance = player.compensatedEntities.self.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE);

        for (double eyeHeight : possibleEyeHeights) {
            Vector3d eyePos = new Vector3d(player.x, player.y + eyeHeight, player.z);
            
            List<Vector3f> lookDirs = getPossibleLookDirections();
            
            for (Vector3f lookDir : lookDirs) {
                Ray ray = new Ray(player, eyePos.x, eyePos.y, eyePos.z, lookDir.x, lookDir.y);
                Vector3d endPoint = ray.getPointAtDistance(distance);
                
                Pair<Vector3dm, BlockFace> intercept = ReachUtils.calculateIntercept(targetBox, eyePos, endPoint);
                
                if (intercept.first() != null) {
                    if (hasSolidWallBetween(eyePos, intercept.first(), distance)) {
                        return true;
                    }
                }
            }
        }
        
        return false;
    }

    private boolean hasSolidWallBetween(Vector3d start, Vector3dm end, double maxDistance) {
        Vector3d direction = new Vector3d(end.x - start.x, end.y - start.y, end.z - start.z);
        double actualDistance = Math.min(direction.length(), maxDistance);
        
        if (actualDistance <= 0) return false;
        
        direction = direction.normalize();
        
        float yaw = (float) Math.toDegrees(Math.atan2(direction.z, direction.x)) - 90f;
        float pitch = (float) -Math.toDegrees(Math.asin(direction.y));
        Ray traceRay = new Ray(player, start.x, start.y, start.z, yaw, pitch);
        
        double currentDist = STEP_SIZE;
        int steps = (int) (actualDistance / STEP_SIZE);
        
        for (int i = 0; i < steps; i++) {
            Vector3d point = traceRay.getPointAtDistance(currentDist);
            
            if (currentDist < 0.3 || currentDist > actualDistance - 0.3) {
                currentDist += STEP_SIZE;
                continue;
            }
            
            int blockX = (int) Math.floor(point.x);
            int blockY = (int) Math.floor(point.y);
            int blockZ = (int) Math.floor(point.z);
            
            if (isSolidWallBlock(blockX, blockY, blockZ)) {
                return true;
            }
            
            currentDist += STEP_SIZE;
        }
        
        return false;
    }

    private boolean isSolidWallBlock(int x, int y, int z) {
        StateTypes blockType;
        try {
            blockType = player.compensatedWorld.getStateAt(x, y, z).getType();
        } catch (Exception e) {
            return false;
        }
        
        if (blockType == StateTypes.AIR) return false;
        if (!blockType.isSolid()) return false;
        if (blockType.isReplaceable()) return false;
        
        if (isExceptionBlock(blockType)) return false;
        
        return true;
    }

    private boolean isExceptionBlock(StateTypes blockType) {
        String name = blockType.getName().toLowerCase();
        
        if (blockType == StateTypes.WATER || blockType == StateTypes.LAVA) {
            return true;
        }
        
        if (name.contains("plant") || name.contains("sapling") || 
            name.contains("mushroom") || name.contains("flower")) {
            return true;
        }
        
        return blockType == StateTypes.LADDER ||
               blockType == StateTypes.VINE ||
               blockType == StateTypes.SNOW ||
               blockType == StateTypes.CARPET ||
               blockType == StateTypes.TORCH ||
               blockType == StateTypes.WALL_TORCH ||
               blockType == StateTypes.REDSTONE_WIRE ||
               blockType == StateTypes.REDSTONE_TORCH ||
               blockType == StateTypes.REPEATER ||
               blockType == StateTypes.COMPARATOR ||
               blockType == StateTypes.RAIL ||
               blockType == StateTypes.POWERED_RAIL ||
               blockType == StateTypes.DETECTOR_RAIL ||
               blockType == StateTypes.ACTIVATOR_RAIL ||
               blockType == StateTypes.LEVER ||
               blockType == StateTypes.STONE_BUTTON ||
               blockType == StateTypes.OAK_BUTTON ||
               name.contains("sign") ||
               name.contains("pressure_plate") ||
               name.contains("carpet") ||
               name.contains("banner") ||
               name.contains("head") ||
               name.contains("skull") ||
               name.contains("fence_gate") ||
               name.contains("trapdoor") ||
               name.contains("door") ||
               name.contains("bed") ||
               name.contains("cake") ||
               name.contains("candle");
    }

    private List<Vector3f> getPossibleLookDirections() {
        List<Vector3f> directions = new ArrayList<>(Arrays.asList(
            new Vector3f(player.yaw, player.pitch, 0),
            new Vector3f(player.lastYaw, player.pitch, 0)
        ));

        if (player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_9)) {
            directions.add(new Vector3f(player.lastYaw, player.lastPitch, 0));
        }

        if (player.getClientVersion().isOlderThan(ClientVersion.V_1_8)) {
            directions = Collections.singletonList(new Vector3f(player.yaw, player.pitch, 0));
        }

        return directions;
    }

    public double getFlagBuffer() {
        return flagBuffer;
    }
}