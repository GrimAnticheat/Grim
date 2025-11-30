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

@CheckData(name = "WallPlace", description = "Placed a block through a wall")
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
                
                Pair<Vector3dm, BlockFace> intercept = ReachUtils.calculateIntercept(targetBox, 
                    new Vector3dm(eyePos.x, eyePos.y, eyePos.z), 
                    new Vector3dm(endPoint.x, endPoint.y, endPoint.z));
                
                if (intercept.getFirst() != null) {
                    Vector3dm interceptPoint = intercept.getFirst();
                    if (hasSolidWallBetween(eyePos, interceptPoint, distance)) {
                        return true;
                    }
                }
            }
        }
        
        return false;
    }

    private boolean hasSolidWallBetween(Vector3d start, Vector3dm end, double maxDistance) {
        double endX = end.x;
        double endY = end.y;
        double endZ = end.z;
        
        Vector3d direction = new Vector3d(endX - start.x, endY - start.y, endZ - start.z);
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
            blockType = player.compensatedWorld.getStateTypeAt(x, y, z);
        } catch (Exception e) {
            return false;
        }
        
        if (blockType == StateTypes.AIR) return false;
        
        if (isLiquid(blockType)) return false;
        if (isNonSolidException(blockType)) return false;
        
        return isFullSolidBlock(blockType);
    }

    private boolean isLiquid(StateTypes blockType) {
        return blockType == StateTypes.WATER || blockType == StateTypes.LAVA;
    }

    private boolean isNonSolidException(StateTypes blockType) {
        return blockType == StateTypes.LADDER ||
               blockType == StateTypes.VINE ||
               blockType == StateTypes.SNOW ||
               isCarpet(blockType) ||
               isTorch(blockType) ||
               isRedstoneComponent(blockType) ||
               isRail(blockType) ||
               isButton(blockType) ||
               isPressurePlate(blockType) ||
               isSign(blockType) ||
               isBanner(blockType) ||
               isHead(blockType) ||
               isGate(blockType) ||
               isTrapdoor(blockType) ||
               isDoor(blockType) ||
               isBed(blockType) ||
               isCake(blockType) ||
               isCandle(blockType) ||
               isPlant(blockType);
    }

    private boolean isFullSolidBlock(StateTypes blockType) {
        return blockType == StateTypes.STONE ||
               blockType == StateTypes.DIRT ||
               blockType == StateTypes.COBBLESTONE ||
               blockType == StateTypes.OAK_PLANKS ||
               blockType == StateTypes.SAND ||
               blockType == StateTypes.GRAVEL ||
               blockType == StateTypes.GOLD_ORE ||
               blockType == StateTypes.IRON_ORE ||
               blockType == StateTypes.COAL_ORE ||
               blockType == StateTypes.OAK_LOG ||
               blockType == StateTypes.GLASS ||
               !isNonSolidException(blockType);
    }

    private boolean isCarpet(StateTypes blockType) {
        return blockType.name().toLowerCase().contains("carpet");
    }

    private boolean isTorch(StateTypes blockType) {
        return blockType.name().toLowerCase().contains("torch");
    }

    private boolean isRedstoneComponent(StateTypes blockType) {
        return blockType.name().toLowerCase().contains("redstone") ||
               blockType == StateTypes.REPEATER ||
               blockType == StateTypes.COMPARATOR ||
               blockType == StateTypes.LEVER;
    }

    private boolean isRail(StateTypes blockType) {
        return blockType.name().toLowerCase().contains("rail");
    }

    private boolean isButton(StateTypes blockType) {
        return blockType.name().toLowerCase().contains("button");
    }

    private boolean isPressurePlate(StateTypes blockType) {
        return blockType.name().toLowerCase().contains("pressure_plate");
    }

    private boolean isSign(StateTypes blockType) {
        return blockType.name().toLowerCase().contains("sign");
    }

    private boolean isBanner(StateTypes blockType) {
        return blockType.name().toLowerCase().contains("banner");
    }

    private boolean isHead(StateTypes blockType) {
        return blockType.name().toLowerCase().contains("head") ||
               blockType.name().toLowerCase().contains("skull");
    }

    private boolean isGate(StateTypes blockType) {
        return blockType.name().toLowerCase().contains("fence_gate");
    }

    private boolean isTrapdoor(StateTypes blockType) {
        return blockType.name().toLowerCase().contains("trapdoor");
    }

    private boolean isDoor(StateTypes blockType) {
        return blockType.name().toLowerCase().contains("door");
    }

    private boolean isBed(StateTypes blockType) {
        return blockType.name().toLowerCase().contains("bed");
    }

    private boolean isCake(StateTypes blockType) {
        return blockType.name().toLowerCase().contains("cake");
    }

    private boolean isCandle(StateTypes blockType) {
        return blockType.name().toLowerCase().contains("candle");
    }

    private boolean isPlant(StateTypes blockType) {
        return blockType.name().toLowerCase().contains("plant") ||
               blockType.name().toLowerCase().contains("sapling") ||
               blockType.name().toLowerCase().contains("mushroom") ||
               blockType.name().toLowerCase().contains("flower");
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