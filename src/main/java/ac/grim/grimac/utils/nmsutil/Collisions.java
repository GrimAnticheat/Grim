package ac.grim.grimac.utils.nmsutil;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.chunks.Column;
import ac.grim.grimac.utils.collisions.CollisionData;
import ac.grim.grimac.utils.collisions.datatypes.CollisionBox;
import ac.grim.grimac.utils.collisions.datatypes.SimpleCollisionBox;
import ac.grim.grimac.utils.data.VectorData;
import ac.grim.grimac.utils.data.packetentity.PacketEntity;
import ac.grim.grimac.utils.math.GrimMath;
import ac.grim.grimac.utils.math.VectorUtils;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.world.chunk.BaseChunk;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.protocol.world.states.defaulttags.BlockTags;
import com.github.retrooper.packetevents.protocol.world.states.type.StateType;
import com.github.retrooper.packetevents.protocol.world.states.type.StateTypes;
import org.bukkit.Location;
import org.bukkit.WorldBorder;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

public class Collisions {
    private static final double COLLISION_EPSILON = 1.0E-7;
    private static final int ABSOLUTE_MAX_SIZE = 29999984;

    private static final boolean IS_FOURTEEN; // Optimization for chunks with empty block count

    static {
        IS_FOURTEEN = PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_14);
    }

    private static final List<List<Axis>> allAxisCombinations = Arrays.asList(
            Arrays.asList(Axis.Y, Axis.X, Axis.Z),
            Arrays.asList(Axis.Y, Axis.Z, Axis.X),

            Arrays.asList(Axis.X, Axis.Y, Axis.Z),
            Arrays.asList(Axis.X, Axis.Z, Axis.Y),

            Arrays.asList(Axis.Z, Axis.X, Axis.Y),
            Arrays.asList(Axis.Z, Axis.Y, Axis.X));

    // Call this when there isn't uncertainty on the Y axis
    public static Vector collide(GrimPlayer player, double desiredX, double desiredY, double desiredZ) {
        return collide(player, desiredX, desiredY, desiredZ, desiredY, null);
    }

    public static Vector collide(GrimPlayer player, double desiredX, double desiredY, double desiredZ, double clientVelY, VectorData data) {
        if (desiredX == 0 && desiredY == 0 && desiredZ == 0) return new Vector();

        List<SimpleCollisionBox> desiredMovementCollisionBoxes = new ArrayList<>();
        getCollisionBoxes(player, player.boundingBox.copy().expandToCoordinate(desiredX, desiredY, desiredZ), desiredMovementCollisionBoxes, false);

        double bestInput = Double.MAX_VALUE;
        Vector bestOrderResult = null;

        Vector bestTheoreticalCollisionResult = VectorUtils.cutBoxToVector(player.actualMovement, new SimpleCollisionBox(0, Math.min(0, desiredY), 0, desiredX, Math.max(0.6, desiredY), desiredZ).sort());
        int zeroCount = (desiredX == 0 ? 1 : 0) + (desiredY == 0 ? 1 : 0) + (desiredZ == 0 ? 1 : 0);

        for (List<Axis> order : allAxisCombinations) {
            Vector collisionResult = collideBoundingBoxLegacy(new Vector(desiredX, desiredY, desiredZ), player.boundingBox, desiredMovementCollisionBoxes, order);

            // While running up stairs and holding space, the player activates the "lastOnGround" part without otherwise being able to step
            // 0.03 movement must compensate for stepping elsewhere.  Too much of a hack to include in this method.
            boolean movingIntoGround = (player.lastOnGround || (collisionResult.getY() != desiredY && (desiredY < 0 || clientVelY < 0))) || player.pointThreeEstimator.closeEnoughToGroundToStepWithPointThree(data, clientVelY);
            double stepUpHeight = player.getMaxUpStep();

            // If the player has x or z collision, is going in the downwards direction in the last or this tick, and can step up
            // If not, just return the collisions without stepping up that we calculated earlier
            if (stepUpHeight > 0.0F && movingIntoGround && (collisionResult.getX() != desiredX || collisionResult.getZ() != desiredZ)) {
                player.uncertaintyHandler.isStepMovement = true;

                // Get a list of bounding boxes from the player's current bounding box to the wanted coordinates
                List<SimpleCollisionBox> stepUpCollisionBoxes = new ArrayList<>();
                getCollisionBoxes(player, player.boundingBox.copy().expandToCoordinate(desiredX, stepUpHeight, desiredZ), stepUpCollisionBoxes, false);

                Vector regularStepUp = collideBoundingBoxLegacy(new Vector(desiredX, stepUpHeight, desiredZ), player.boundingBox, stepUpCollisionBoxes, order);

                // 1.7 clients do not have this stepping bug fix
                if (player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_8)) {
                    Vector stepUpBugFix = collideBoundingBoxLegacy(new Vector(0, stepUpHeight, 0), player.boundingBox.copy().expandToCoordinate(desiredX, 0, desiredZ), stepUpCollisionBoxes, order);
                    if (stepUpBugFix.getY() < stepUpHeight) {
                        Vector stepUpBugFixResult = collideBoundingBoxLegacy(new Vector(desiredX, 0, desiredZ), player.boundingBox.copy().offset(0, stepUpBugFix.getY(), 0), stepUpCollisionBoxes, order).add(stepUpBugFix);
                        if (getHorizontalDistanceSqr(stepUpBugFixResult) > getHorizontalDistanceSqr(regularStepUp)) {
                            regularStepUp = stepUpBugFixResult;
                        }
                    }
                }

                if (getHorizontalDistanceSqr(regularStepUp) > getHorizontalDistanceSqr(collisionResult)) {
                    collisionResult = regularStepUp.add(collideBoundingBoxLegacy(new Vector(0, -regularStepUp.getY() + (player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_14) ? desiredY : 0), 0), player.boundingBox.copy().offset(regularStepUp.getX(), regularStepUp.getY(), regularStepUp.getZ()), stepUpCollisionBoxes, order));
                }
            }

            double resultAccuracy = collisionResult.distanceSquared(bestTheoreticalCollisionResult);

            // Step movement doesn't care about ground (due to 0.03 fucking it up)
            if (player.wouldCollisionResultFlagGroundSpoof(desiredY, collisionResult.getY())) {
                resultAccuracy += 1;
            }

            if (resultAccuracy < bestInput) {
                bestOrderResult = collisionResult;
                bestInput = resultAccuracy;
                if (resultAccuracy < 0.00001 * 0.00001) break;
                if (zeroCount >= 2) break;
            }

        }
        return bestOrderResult;
    }

    // This is mostly taken from Tuinity collisions
    public static boolean getCollisionBoxes(GrimPlayer player, SimpleCollisionBox wantedBB, List<SimpleCollisionBox> listOfBlocks, boolean onlyCheckCollide) {
        SimpleCollisionBox expandedBB = wantedBB.copy();

        // Worldborders were added in 1.8
        // Don't add to border unless the player is colliding with it and is near it
        if (player.clientControlledHorizontalCollision && PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_8) && player.playerWorld != null) {
            WorldBorder border = player.playerWorld.getWorldBorder();
            double centerX = border.getCenter().getX();
            double centerZ = border.getCenter().getZ();

            // For some reason, the game limits the border to 29999984 blocks wide
            // TODO: Support dynamic worldborder with latency compensation
            double size = border.getSize() / 2;

            // If the player's is within 16 blocks of the worldborder, add the worldborder to the collisions (optimization)
            if (Math.abs(player.x + centerX) + 16 > size || Math.abs(player.z + centerZ) + 16 > size) {
                double minX = Math.floor(GrimMath.clamp(centerX - size, -ABSOLUTE_MAX_SIZE, ABSOLUTE_MAX_SIZE));
                double minZ = Math.floor(GrimMath.clamp(centerZ - size, -ABSOLUTE_MAX_SIZE, ABSOLUTE_MAX_SIZE));
                double maxX = Math.ceil(GrimMath.clamp(centerX + size, -ABSOLUTE_MAX_SIZE, ABSOLUTE_MAX_SIZE));
                double maxZ = Math.ceil(GrimMath.clamp(centerZ + size, -ABSOLUTE_MAX_SIZE, ABSOLUTE_MAX_SIZE));

                // If the player is fully within the worldborder
                double maxMax = Math.max(Math.max(maxX - minX, maxZ - minZ), 1.0D);

                double d0 = player.lastZ - minZ;
                double d1 = maxZ - player.lastZ;
                double d2 = player.lastX - minX;
                double d3 = maxX - player.lastX;
                double d4 = Math.min(d2, d3);
                d4 = Math.min(d4, d0);
                double distanceToBorder = Math.min(d4, d1);

                if (distanceToBorder < maxMax * 2.0D && player.lastX > minX - maxMax && player.lastX < maxX + maxMax && player.lastZ > minZ - maxMax && player.lastZ < maxZ + maxMax) {
                    if (listOfBlocks == null) listOfBlocks = new ArrayList<>();

                    // South border
                    listOfBlocks.add(new SimpleCollisionBox(minX - 10, Double.NEGATIVE_INFINITY, maxZ, maxX + 10, Double.POSITIVE_INFINITY, maxZ, false));
                    // North border
                    listOfBlocks.add(new SimpleCollisionBox(minX - 10, Double.NEGATIVE_INFINITY, minZ, maxX + 10, Double.POSITIVE_INFINITY, minZ, false));
                    // East border
                    listOfBlocks.add(new SimpleCollisionBox(maxX, Double.NEGATIVE_INFINITY, minZ - 10, maxX, Double.POSITIVE_INFINITY, maxZ + 10, false));
                    // West border
                    listOfBlocks.add(new SimpleCollisionBox(minX, Double.NEGATIVE_INFINITY, minZ - 10, minX, Double.POSITIVE_INFINITY, maxZ + 10, false));

                    if (onlyCheckCollide) {
                        for (SimpleCollisionBox box : listOfBlocks) {
                            if (box.isIntersected(wantedBB)) return true;
                        }
                    }
                }
            }
        }

        int minBlockX = (int) Math.floor(expandedBB.minX - COLLISION_EPSILON) - 1;
        int maxBlockX = (int) Math.floor(expandedBB.maxX + COLLISION_EPSILON) + 1;
        int minBlockY = (int) Math.floor(expandedBB.minY - COLLISION_EPSILON) - 1;
        int maxBlockY = (int) Math.floor(expandedBB.maxY + COLLISION_EPSILON) + 1;
        int minBlockZ = (int) Math.floor(expandedBB.minZ - COLLISION_EPSILON) - 1;
        int maxBlockZ = (int) Math.floor(expandedBB.maxZ + COLLISION_EPSILON) + 1;

        final int minSection = player.compensatedWorld.getMinHeight() >> 4;
        final int minBlock = minSection << 4;
        final int maxBlock = player.compensatedWorld.getMaxHeight() - 1;

        int minChunkX = minBlockX >> 4;
        int maxChunkX = maxBlockX >> 4;

        int minChunkZ = minBlockZ >> 4;
        int maxChunkZ = maxBlockZ >> 4;

        int minYIterate = Math.max(minBlock, minBlockY);
        int maxYIterate = Math.min(maxBlock, maxBlockY);

        for (int currChunkZ = minChunkZ; currChunkZ <= maxChunkZ; ++currChunkZ) {
            int minZ = currChunkZ == minChunkZ ? minBlockZ & 15 : 0; // coordinate in chunk
            int maxZ = currChunkZ == maxChunkZ ? maxBlockZ & 15 : 15; // coordinate in chunk

            for (int currChunkX = minChunkX; currChunkX <= maxChunkX; ++currChunkX) {
                int minX = currChunkX == minChunkX ? minBlockX & 15 : 0; // coordinate in chunk
                int maxX = currChunkX == maxChunkX ? maxBlockX & 15 : 15; // coordinate in chunk

                int chunkXGlobalPos = currChunkX << 4;
                int chunkZGlobalPos = currChunkZ << 4;

                Column chunk = player.compensatedWorld.getChunk(currChunkX, currChunkZ);
                if (chunk == null) continue;

                BaseChunk[] sections = chunk.getChunks();

                for (int y = minYIterate; y <= maxYIterate; ++y) {
                    int sectionIndex = (y >> 4) - minSection;

                    BaseChunk section = sections[sectionIndex];

                    if (section == null || (IS_FOURTEEN && section.isEmpty())) { // Check for empty on 1.13+ servers
                        // empty
                        // skip to next section
                        y = (y & ~(15)) + 15; // increment by 15: iterator loop increments by the extra one
                        continue;
                    }

                    for (int currZ = minZ; currZ <= maxZ; ++currZ) {
                        for (int currX = minX; currX <= maxX; ++currX) {
                            int x = currX | chunkXGlobalPos;
                            int z = currZ | chunkZGlobalPos;

                            WrappedBlockState data = section.get(x & 0xF, y & 0xF, z & 0xF);

                            // Works on both legacy and modern!  Faster than checking for material types, most common case
                            if (data.getGlobalId() == 0) continue;

                            int edgeCount = ((x == minBlockX || x == maxBlockX) ? 1 : 0) +
                                    ((y == minBlockY || y == maxBlockY) ? 1 : 0) +
                                    ((z == minBlockZ || z == maxBlockZ) ? 1 : 0);

                            if (edgeCount != 3 && (edgeCount != 1 || Materials.isShapeExceedsCube(data.getType()))
                                    && (edgeCount != 2 || data.getType() == StateTypes.PISTON_HEAD)) {
                                // Don't add to a list if we only care if the player intersects with the block
                                if (!onlyCheckCollide) {
                                    CollisionData.getData(data.getType()).getMovementCollisionBox(player, player.getClientVersion(), data, x, y, z).downCast(listOfBlocks);
                                } else if (CollisionData.getData(data.getType()).getMovementCollisionBox(player, player.getClientVersion(), data, x, y, z).isCollided(wantedBB)) {
                                    return true;
                                }
                            }
                        }
                    }
                }
            }
        }

        for (PacketEntity entity : player.compensatedEntities.entityMap.values()) {
            if (entity.type == EntityTypes.BOAT && player.playerVehicle != entity) {
                SimpleCollisionBox box = entity.getPossibleCollisionBoxes();
                if (box.isIntersected(expandedBB)) {
                    if (listOfBlocks == null) listOfBlocks = new ArrayList<>();
                    listOfBlocks.add(box);
                }
            }

            if (entity.type == EntityTypes.SHULKER) {
                SimpleCollisionBox box = entity.getPossibleCollisionBoxes();
                if (box.isIntersected(expandedBB)) {
                    if (listOfBlocks == null) listOfBlocks = new ArrayList<>();
                    listOfBlocks.add(box);
                }
            }
        }

        return false;
    }

    public static Vector collideBoundingBoxLegacy(Vector toCollide, SimpleCollisionBox
            box, List<SimpleCollisionBox> desiredMovementCollisionBoxes, List<Axis> order) {
        double x = toCollide.getX();
        double y = toCollide.getY();
        double z = toCollide.getZ();

        SimpleCollisionBox setBB = box.copy();

        for (Axis axis : order) {
            if (axis == Axis.X) {
                for (SimpleCollisionBox bb : desiredMovementCollisionBoxes) {
                    x = bb.collideX(setBB, x);
                }
                setBB.offset(x, 0.0D, 0.0D);
            } else if (axis == Axis.Y) {
                for (SimpleCollisionBox bb : desiredMovementCollisionBoxes) {
                    y = bb.collideY(setBB, y);
                }
                setBB.offset(0.0D, y, 0.0D);
            } else if (axis == Axis.Z) {
                for (SimpleCollisionBox bb : desiredMovementCollisionBoxes) {
                    z = bb.collideZ(setBB, z);
                }
                setBB.offset(0.0D, 0.0D, z);
            }
        }

        return new Vector(x, y, z);
    }

    public static boolean isEmpty(GrimPlayer player, SimpleCollisionBox playerBB) {
        return !getCollisionBoxes(player, playerBB, null, true);
    }

    private static double getHorizontalDistanceSqr(Vector vector) {
        return vector.getX() * vector.getX() + vector.getZ() * vector.getZ();
    }

    public static Vector maybeBackOffFromEdge(Vector vec3, GrimPlayer player, boolean overrideVersion) {
        if (!player.specialFlying && player.isSneaking && isAboveGround(player)) {
            double x = vec3.getX();
            double z = vec3.getZ();

            double maxStepDown = overrideVersion || player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_11) ? -player.getMaxUpStep() : -1 + COLLISION_EPSILON;

            while (x != 0.0 && isEmpty(player, player.boundingBox.copy().offset(x, maxStepDown, 0.0))) {
                if (x < 0.05D && x >= -0.05D) {
                    x = 0.0D;
                } else if (x > 0.0D) {
                    x -= 0.05D;
                } else {
                    x += 0.05D;
                }
            }
            while (z != 0.0 && isEmpty(player, player.boundingBox.copy().offset(0.0, maxStepDown, z))) {
                if (z < 0.05D && z >= -0.05D) {
                    z = 0.0D;
                } else if (z > 0.0D) {
                    z -= 0.05D;
                } else {
                    z += 0.05D;
                }
            }
            while (x != 0.0 && z != 0.0 && isEmpty(player, player.boundingBox.copy().offset(x, maxStepDown, z))) {
                if (x < 0.05D && x >= -0.05D) {
                    x = 0.0D;
                } else if (x > 0.0D) {
                    x -= 0.05D;
                } else {
                    x += 0.05D;
                }

                if (z < 0.05D && z >= -0.05D) {
                    z = 0.0D;
                } else if (z > 0.0D) {
                    z -= 0.05D;
                } else {
                    z += 0.05D;
                }
            }
            vec3 = new Vector(x, vec3.getY(), z);
        }

        return vec3;
    }

    public static boolean isAboveGround(GrimPlayer player) {
        // https://bugs.mojang.com/browse/MC-2404
        return player.lastOnGround || (player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_16_2) && (player.fallDistance < player.getMaxUpStep() &&
                !isEmpty(player, player.boundingBox.copy().offset(0.0, player.fallDistance - player.getMaxUpStep(), 0.0))));
    }

    public static void handleInsideBlocks(GrimPlayer player) {
        // Use the bounding box for after the player's movement is applied
        SimpleCollisionBox aABB = player.inVehicle ? GetBoundingBox.getCollisionBoxForPlayer(player, player.x, player.y, player.z).expand(-0.001) : player.boundingBox.copy().expand(-0.001);

        Location blockPos = new Location(player.playerWorld, aABB.minX, aABB.minY, aABB.minZ);
        Location blockPos2 = new Location(player.playerWorld, aABB.maxX, aABB.maxY, aABB.maxZ);

        if (CheckIfChunksLoaded.isChunksUnloadedAt(player, blockPos.getBlockX(), blockPos.getBlockY(), blockPos.getBlockZ(), blockPos2.getBlockX(), blockPos2.getBlockY(), blockPos2.getBlockZ()))
            return;

        for (int i = blockPos.getBlockX(); i <= blockPos2.getBlockX(); ++i) {
            for (int j = blockPos.getBlockY(); j <= blockPos2.getBlockY(); ++j) {
                for (int k = blockPos.getBlockZ(); k <= blockPos2.getBlockZ(); ++k) {
                    WrappedBlockState block = player.compensatedWorld.getWrappedBlockStateAt(i, j, k);
                    StateType blockType = block.getType();

                    if (blockType == StateTypes.COBWEB) {
                        player.stuckSpeedMultiplier = new Vector(0.25, 0.05000000074505806, 0.25);
                    }

                    if (blockType == StateTypes.SWEET_BERRY_BUSH
                            && player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_14)) {
                        player.stuckSpeedMultiplier = new Vector(0.800000011920929, 0.75, 0.800000011920929);
                    }

                    if (blockType == StateTypes.POWDER_SNOW && i == Math.floor(player.x) && j == Math.floor(player.y) && k == Math.floor(player.z)
                            && player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_17)) {
                        player.stuckSpeedMultiplier = new Vector(0.8999999761581421, 1.5, 0.8999999761581421);
                    }

                    if (blockType == StateTypes.SOUL_SAND && player.getClientVersion().isOlderThan(ClientVersion.V_1_15)) {
                        player.clientVelocity.setX(player.clientVelocity.getX() * 0.4D);
                        player.clientVelocity.setZ(player.clientVelocity.getZ() * 0.4D);
                    }

                    if (blockType == StateTypes.LAVA && player.getClientVersion().isOlderThan(ClientVersion.V_1_16) && player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_14)) {
                        player.wasTouchingLava = true;
                    }

                    if (blockType == StateTypes.BUBBLE_COLUMN && player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_13)) {
                        WrappedBlockState blockAbove = player.compensatedWorld.getWrappedBlockStateAt(i, j + 1, k);

                        if (player.playerVehicle != null && player.playerVehicle.type == EntityTypes.BOAT) {
                            if (!blockAbove.getType().isAir()) {
                                if (block.isDrag()) {
                                    player.clientVelocity.setY(Math.max(-0.3D, player.clientVelocity.getY() - 0.03D));
                                } else {
                                    player.clientVelocity.setY(Math.min(0.7D, player.clientVelocity.getY() + 0.06D));
                                }
                            }
                        } else {
                            if (blockAbove.getType().isAir()) {
                                for (VectorData vector : player.getPossibleVelocitiesMinusKnockback()) {
                                    if (block.isDrag()) {
                                        vector.vector.setY(Math.max(-0.9D, vector.vector.getY() - 0.03D));
                                    } else {
                                        vector.vector.setY(Math.min(1.8D, vector.vector.getY() + 0.1D));
                                    }
                                }
                            } else {
                                for (VectorData vector : player.getPossibleVelocitiesMinusKnockback()) {
                                    if (block.isDrag()) {
                                        vector.vector.setY(Math.max(-0.3D, vector.vector.getY() - 0.03D));
                                    } else {
                                        vector.vector.setY(Math.min(0.7D, vector.vector.getY() + 0.06D));
                                    }
                                }
                            }
                        }

                        // Reset fall distance inside bubble column
                        player.fallDistance = 0;
                    }

                    if (blockType == StateTypes.HONEY_BLOCK && player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_15)) {
                        if (isSlidingDown(player.clientVelocity, player, i, j, j)) {
                            if (player.clientVelocity.getY() < -0.13D) {
                                double d0 = -0.05 / player.clientVelocity.getY();
                                player.clientVelocity.setX(player.clientVelocity.getX() * d0);
                                player.clientVelocity.setY(-0.05D);
                                player.clientVelocity.setZ(player.clientVelocity.getZ() * d0);
                            } else {
                                player.clientVelocity.setY(-0.05D);
                            }
                        }

                        // If honey sliding, fall distance is 0
                        player.fallDistance = 0;
                    }
                }
            }
        }
    }

    private static boolean isSlidingDown(Vector vector, GrimPlayer player, int locationX, int locationY,
                                         int locationZ) {
        if (player.onGround) {
            return false;
        } else if (player.y > locationY + 0.9375D - 1.0E-7D) {
            return false;
        } else if (vector.getY() >= -0.08D) {
            return false;
        } else {
            double d0 = Math.abs(locationX + 0.5D - player.lastX);
            double d1 = Math.abs(locationZ + 0.5D - player.lastZ);
            // Calculate player width using bounding box, which will change while swimming or gliding
            double d2 = 0.4375D + ((player.pose.width) / 2.0F);
            return d0 + 1.0E-7D > d2 || d1 + 1.0E-7D > d2;
        }
    }

    // 0.03 hack
    public static boolean checkStuckSpeed(GrimPlayer player, double expand) {
        // Use the bounding box for after the player's movement is applied
        SimpleCollisionBox aABB = GetBoundingBox.getCollisionBoxForPlayer(player, player.x, player.y, player.z).expand(expand);

        Location blockPos = new Location(player.playerWorld, aABB.minX, aABB.minY, aABB.minZ);
        Location blockPos2 = new Location(player.playerWorld, aABB.maxX, aABB.maxY, aABB.maxZ);

        if (CheckIfChunksLoaded.isChunksUnloadedAt(player, blockPos.getBlockX(), blockPos.getBlockY(), blockPos.getBlockZ(), blockPos2.getBlockX(), blockPos2.getBlockY(), blockPos2.getBlockZ()))
            return false;

        for (int i = blockPos.getBlockX(); i <= blockPos2.getBlockX(); ++i) {
            for (int j = blockPos.getBlockY(); j <= blockPos2.getBlockY(); ++j) {
                for (int k = blockPos.getBlockZ(); k <= blockPos2.getBlockZ(); ++k) {
                    WrappedBlockState block = player.compensatedWorld.getWrappedBlockStateAt(i, j, k);
                    StateType blockType = block.getType();

                    if (blockType == StateTypes.COBWEB) {
                        return true;
                    }

                    if (blockType == StateTypes.SWEET_BERRY_BUSH && player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_14)) {
                        return true;
                    }

                    if (blockType == StateTypes.POWDER_SNOW && i == Math.floor(player.x) && j == Math.floor(player.y) && k == Math.floor(player.z) && player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_17)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    public static boolean suffocatesAt(GrimPlayer player, SimpleCollisionBox playerBB) {
        // Blocks are stored in YZX order
        for (int y = (int) Math.floor(playerBB.minY); y < Math.ceil(playerBB.maxY); y++) {
            for (int z = (int) Math.floor(playerBB.minZ); z < Math.ceil(playerBB.maxZ); z++) {
                for (int x = (int) Math.floor(playerBB.minX); x < Math.ceil(playerBB.maxX); x++) {
                    if (doesBlockSuffocate(player, x, y, z)) {
                        // Mojang re-added soul sand pushing by checking if the player is actually in the block
                        // (This is why from 1.14-1.15 soul sand didn't push)
                        if (player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_16)) {
                            WrappedBlockState data = player.compensatedWorld.getWrappedBlockStateAt(x, y, z);
                            CollisionBox box = CollisionData.getData(data.getType()).getMovementCollisionBox(player, player.getClientVersion(), data, x, y, z);

                            if (!box.isIntersected(playerBB)) continue;
                        }

                        return true;
                    }
                }
            }
        }

        return false;
    }

    public static boolean doesBlockSuffocate(GrimPlayer player, int x, int y, int z) {
        WrappedBlockState data = player.compensatedWorld.getWrappedBlockStateAt(x, y, z);
        StateType mat = data.getType();

        // Optimization - all blocks that can suffocate must have a hitbox
        if (!mat.isSolid()) return false;

        // 1.13- players can not be pushed by blocks that can emit power, for some reason, while 1.14+ players can
        if (mat == StateTypes.OBSERVER || mat == StateTypes.REDSTONE_BLOCK)
            return player.getClientVersion().isNewerThan(ClientVersion.V_1_13_2);
        // Tnt only pushes on 1.14+ clients
        if (mat == StateTypes.TNT) return player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_14);
        // Farmland only pushes on 1.16+ clients
        if (mat == StateTypes.FARMLAND) return player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_16);
        // 1.14-1.15 doesn't push with soul sand, the rest of the versions do
        if (mat == StateTypes.SOUL_SAND)
            return player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_16) || player.getClientVersion().isOlderThan(ClientVersion.V_1_14);
        // 1.13 and below exempt piston bases, while 1.14+ look to see if they are a full block or not
        if ((mat == StateTypes.PISTON || mat == StateTypes.STICKY_PISTON) && player.getClientVersion().isOlderThan(ClientVersion.V_1_14))
            return false;
        // 1.13 and below exempt ICE and FROSTED_ICE, 1.14 have them push
        if (mat == StateTypes.ICE || mat == StateTypes.FROSTED_ICE)
            return player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_14);
        // I believe leaves and glass are consistently exempted across all versions
        if (BlockTags.LEAVES.contains(mat) || BlockTags.GLASS_BLOCKS.contains(mat)) return false;
        // 1.16 players are pushed by dirt paths, 1.8 players don't have this block, so it gets converted to a full block
        if (mat == StateTypes.DIRT_PATH)
            return player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_16) || player.getClientVersion().isOlderThan(ClientVersion.V_1_9);
        // Only 1.14+ players are pushed by beacons
        if (mat == StateTypes.BEACON) return player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_14);

        // Thank god I already have the solid blocking blacklist written, but all these are exempt
        if (Materials.isSolidBlockingBlacklist(mat, player.getClientVersion())) return false;

        CollisionBox box = CollisionData.getData(mat).getMovementCollisionBox(player, player.getClientVersion(), data, x, y, z);
        return box.isFullBlock();
    }

    public static boolean hasBouncyBlock(GrimPlayer player) {
        SimpleCollisionBox playerBB = GetBoundingBox.getCollisionBoxForPlayer(player, player.x, player.y, player.z).expand(0.03).offset(0, -1, 0);
        return hasSlimeBlock(player) || hasMaterial(player, playerBB, type -> BlockTags.BEDS.contains(type.getType()));
    }

    // Has slime block, or honey with the ViaVersion replacement block
    // This is terrible code lmao.  I need to refactor to add a new player bounding box, or somehow play with block mappings,
    // so I can automatically map honey -> slime and other important ViaVersion replacement blocks
    public static boolean hasSlimeBlock(GrimPlayer player) {
        return player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_8) // Only 1.8 players have slime blocks
                && (hasMaterial(player, StateTypes.SLIME_BLOCK, -1) // Directly a slime block
                ||
                // ViaVersion mapped slime block from 1.8 to 1.14.4
                (player.getClientVersion().isOlderThanOrEquals(ClientVersion.V_1_14_4)
                        && player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_8)
                        && hasMaterial(player, StateTypes.HONEY_BLOCK, -1)));
    }

    public static boolean hasMaterial(GrimPlayer player, StateType searchMat, double offset) {
        SimpleCollisionBox playerBB = GetBoundingBox.getCollisionBoxForPlayer(player, player.x, player.y, player.z).expand(0.03).offset(0, offset, 0);
        return hasMaterial(player, playerBB, material -> material.getType() == searchMat);
    }

    // Thanks Tuinity
    public static boolean hasMaterial(GrimPlayer player, SimpleCollisionBox checkBox, Predicate<WrappedBlockState> searchingFor) {
        int minBlockX = (int) Math.floor(checkBox.minX);
        int maxBlockX = (int) Math.floor(checkBox.maxX);
        int minBlockY = (int) Math.floor(checkBox.minY);
        int maxBlockY = (int) Math.floor(checkBox.maxY);
        int minBlockZ = (int) Math.floor(checkBox.minZ);
        int maxBlockZ = (int) Math.floor(checkBox.maxZ);

        final int minSection = player.compensatedWorld.getMinHeight() >> 4;
        final int minBlock = minSection << 4;
        final int maxBlock = player.compensatedWorld.getMaxHeight() - 1;

        int minChunkX = minBlockX >> 4;
        int maxChunkX = maxBlockX >> 4;

        int minChunkZ = minBlockZ >> 4;
        int maxChunkZ = maxBlockZ >> 4;

        int minYIterate = Math.max(minBlock, minBlockY);
        int maxYIterate = Math.min(maxBlock, maxBlockY);

        for (int currChunkZ = minChunkZ; currChunkZ <= maxChunkZ; ++currChunkZ) {
            int minZ = currChunkZ == minChunkZ ? minBlockZ & 15 : 0; // coordinate in chunk
            int maxZ = currChunkZ == maxChunkZ ? maxBlockZ & 15 : 15; // coordinate in chunk

            for (int currChunkX = minChunkX; currChunkX <= maxChunkX; ++currChunkX) {
                int minX = currChunkX == minChunkX ? minBlockX & 15 : 0; // coordinate in chunk
                int maxX = currChunkX == maxChunkX ? maxBlockX & 15 : 15; // coordinate in chunk

                int chunkXGlobalPos = currChunkX << 4;
                int chunkZGlobalPos = currChunkZ << 4;

                Column chunk = player.compensatedWorld.getChunk(currChunkX, currChunkZ);

                if (chunk == null) continue;
                BaseChunk[] sections = chunk.getChunks();

                for (int y = minYIterate; y <= maxYIterate; ++y) {
                    BaseChunk section = sections[(y >> 4) - minSection];

                    if (section == null || (IS_FOURTEEN && section.isEmpty())) { // Check for empty on 1.13+ servers
                        // empty
                        // skip to next section
                        y = (y & ~(15)) + 15; // increment by 15: iterator loop increments by the extra one
                        continue;
                    }

                    for (int currZ = minZ; currZ <= maxZ; ++currZ) {
                        for (int currX = minX; currX <= maxX; ++currX) {
                            int x = currX | chunkXGlobalPos;
                            int z = currZ | chunkZGlobalPos;

                            WrappedBlockState data = section.get(x & 0xF, y & 0xF, z & 0xF);

                            if (searchingFor.test(data)) return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    public static boolean onClimbable(GrimPlayer player, double x, double y, double z) {
        WrappedBlockState blockState = player.compensatedWorld.getWrappedBlockStateAt(x, y, z);
        StateType blockMaterial = blockState.getType();

        if (BlockTags.CLIMBABLE.contains(blockMaterial)) {
            return true;
        }

        // ViaVersion replacement block -> sweet berry bush to vines
        if (blockMaterial == StateTypes.SWEET_BERRY_BUSH && player.getClientVersion().isOlderThan(ClientVersion.V_1_14)) {
            return true;
        }

        return trapdoorUsableAsLadder(player, x, y, z, blockState);
    }

    public static boolean trapdoorUsableAsLadder(GrimPlayer player, double x, double y, double z, WrappedBlockState blockData) {
        if (!BlockTags.TRAPDOORS.contains(blockData.getType())) return false;

        if (blockData.isOpen()) {
            WrappedBlockState blockBelow = player.compensatedWorld.getWrappedBlockStateAt(x, y - 1, z);

            if (blockBelow.getType() == StateTypes.LADDER) {
                return blockData.getFacing() == blockBelow.getFacing();
            }
        }

        return false;
    }

    public enum Axis {
        X,
        Y,
        Z
    }
}
