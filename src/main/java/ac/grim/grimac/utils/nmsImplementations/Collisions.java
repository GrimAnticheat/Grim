package ac.grim.grimac.utils.nmsImplementations;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.blockdata.WrappedBlockData;
import ac.grim.grimac.utils.blockdata.types.WrappedBlockDataValue;
import ac.grim.grimac.utils.blockdata.types.WrappedDirectional;
import ac.grim.grimac.utils.blockdata.types.WrappedTrapdoor;
import ac.grim.grimac.utils.blockstate.BaseBlockState;
import ac.grim.grimac.utils.blockstate.FlatBlockState;
import ac.grim.grimac.utils.chunkdata.BaseChunk;
import ac.grim.grimac.utils.chunks.Column;
import ac.grim.grimac.utils.collisions.CollisionData;
import ac.grim.grimac.utils.collisions.datatypes.CollisionBox;
import ac.grim.grimac.utils.collisions.datatypes.SimpleCollisionBox;
import ac.grim.grimac.utils.data.VectorData;
import ac.grim.grimac.utils.data.packetentity.PacketEntity;
import ac.grim.grimac.utils.enums.EntityType;
import ac.grim.grimac.utils.math.GrimMath;
import ac.grim.grimac.utils.math.VectorUtils;
import io.github.retrooper.packetevents.utils.player.ClientVersion;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.WorldBorder;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.BubbleColumn;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

public class Collisions {
    private static final Material HONEY_BLOCK = XMaterial.HONEY_BLOCK.parseMaterial();
    private static final Material COBWEB = XMaterial.COBWEB.parseMaterial();
    private static final Material BUBBLE_COLUMN = XMaterial.BUBBLE_COLUMN.parseMaterial();
    private static final Material SWEET_BERRY_BUSH = XMaterial.SWEET_BERRY_BUSH.parseMaterial();
    private static final Material SLIME_BLOCK = XMaterial.SLIME_BLOCK.parseMaterial();
    private static final Material POWDER_SNOW = XMaterial.POWDER_SNOW.parseMaterial();

    private static final Material LADDER = XMaterial.LADDER.parseMaterial();

    private static final Material PISTON_HEAD = XMaterial.PISTON_HEAD.parseMaterial();

    private static final Material OBSERVER = XMaterial.OBSERVER.parseMaterial();
    private static final Material REDSTONE_BLOCK = XMaterial.REDSTONE_BLOCK.parseMaterial();

    private static final Material ICE = XMaterial.ICE.parseMaterial();
    private static final Material FROSTED_ICE = XMaterial.FROSTED_ICE.parseMaterial();

    private static final Material TNT = XMaterial.TNT.parseMaterial();
    private static final Material FARMLAND = XMaterial.FARMLAND.parseMaterial();
    private static final Material DIRT_PATH = XMaterial.DIRT_PATH.parseMaterial();
    private static final Material SOUL_SAND = XMaterial.SOUL_SAND.parseMaterial();
    private static final Material PISTON_BASE = XMaterial.PISTON.parseMaterial();
    private static final Material STICKY_PISTON_BASE = XMaterial.STICKY_PISTON.parseMaterial();
    private static final Material BEACON = XMaterial.BEACON.parseMaterial();

    private static final double COLLISION_EPSILON = 1.0E-7;
    private static final int absoluteMaxSize = 29999984;

    private static final List<List<Axis>> allAxisCombinations = Arrays.asList(
            Arrays.asList(Axis.Y, Axis.X, Axis.Z),
            Arrays.asList(Axis.Y, Axis.Z, Axis.X),

            Arrays.asList(Axis.X, Axis.Y, Axis.Z),
            Arrays.asList(Axis.X, Axis.Z, Axis.Y),

            Arrays.asList(Axis.Z, Axis.X, Axis.Y),
            Arrays.asList(Axis.Z, Axis.Y, Axis.X));

    // Call this when there isn't uncertainty on the Y axis
    public static Vector collide(GrimPlayer player, double desiredX, double desiredY, double desiredZ) {
        return collide(player, desiredX, desiredY, desiredZ, desiredY);
    }

    public static Vector collide(GrimPlayer player, double desiredX, double desiredY, double desiredZ, double clientVelY) {
        if (desiredX == 0 && desiredY == 0 && desiredZ == 0) return new Vector();

        List<SimpleCollisionBox> desiredMovementCollisionBoxes = new ArrayList<>();
        getCollisionBoxes(player, player.boundingBox.copy().expandToCoordinate(desiredX, desiredY, desiredZ), desiredMovementCollisionBoxes, false);

        double bestInput = Double.MAX_VALUE;
        Vector bestOrderResult = null;

        Vector bestTheoreticalCollisionResult = VectorUtils.cutBoxToVector(player.actualMovement, new SimpleCollisionBox(0, Math.min(0, desiredY), 0, desiredX, Math.max(0.6, desiredY), desiredZ).sort());
        int zeroCount = (desiredX == 0 ? 1 : 0) + (desiredY == 0 ? 1 : 0) + (desiredZ == 0 ? 1 : 0);

        for (List<Axis> order : allAxisCombinations) {
            Vector collisionResult = collideBoundingBoxLegacy(player, new Vector(desiredX, desiredY, desiredZ), player.boundingBox, desiredMovementCollisionBoxes, order);

            // While running up stairs and holding space, the player activates the "lastOnGround" part without otherwise being able to step
            // Also allow the non uncertain vector to be below 0 to attempt to fix false positives
            boolean movingIntoGround = player.lastOnGround || (collisionResult.getY() != desiredY && (desiredY < 0 || clientVelY < 0)) ||
                    // If the player is claiming that they were stepping
                    // And the player's Y velocity is "close enough" to being downwards
                    // And the last movement was 0.03 messing up stepping
                    //
                    // Additionally, the player must be stepping onto a block for this to work
                    // not a "perfect" method to detect stepping, but it should cover this 0.03 edge case with small movement
                    //
                    // 9/14/2021
                    // TODO: This might allow some sort of stepping bypass, although not a major one
                    // I don't know how to fix this 0.03 issue
                    // This is the setup in case you want to tweak this 0.03-related uncertainty:
                    // TRAPDOOR SLAB
                    // BLOCK
                    //
                    // DesiredY is reported as 0.003 when this situation occurs, give a bit more lenience though
                    // Could allow step cheats that step onto 1.25 levels, although it's not much of a cheat
                    // Additionally, I haven't been able to find this cheat yet, and will patch it if I find it.
                    // But for now I'd rather keep this simpler rather than trying to blindly patch a
                    // nonexistent cheat.
                    (player.actualMovement.getY() > 0 && desiredY < 0.005 && !Collisions.isEmpty(player, GetBoundingBox.getCollisionBoxForPlayer(player, player.x, player.y, player.z).offset(0, -COLLISION_EPSILON, 0)))
                    // Fix a false with cobwebs on top of soul sand (0.03) - We don't detect that the player actually would touch the ground this tick
                    || (player.onGround && (player.uncertaintyHandler.wasAffectedByStuckSpeed() || player.uncertaintyHandler.influencedByBouncyBlock()) && player.uncertaintyHandler.lastTickWasNearGroundZeroPointZeroThree)
                    // Fix a false when stepping underwater with high uncertainty (require fluid on eyes to stop players from exiting water with stepping movement)
                    || (player.onGround && player.uncertaintyHandler.controlsVerticalMovement() && !Collisions.isEmpty(player, GetBoundingBox.getCollisionBoxForPlayer(player, player.x, player.y, player.z).offset(0, -COLLISION_EPSILON, 0)));
            double stepUpHeight = player.getMaxUpStep();

            // If the player has x or z collision, is going in the downwards direction in the last or this tick, and can step up
            // If not, just return the collisions without stepping up that we calculated earlier
            if (stepUpHeight > 0.0F && movingIntoGround && (collisionResult.getX() != desiredX || collisionResult.getZ() != desiredZ)) {
                player.uncertaintyHandler.isStepMovement = true;

                // Get a list of bounding boxes from the player's current bounding box to the wanted coordinates
                List<SimpleCollisionBox> stepUpCollisionBoxes = new ArrayList<>();
                getCollisionBoxes(player, player.boundingBox.copy().expandToCoordinate(desiredX, stepUpHeight, desiredZ), stepUpCollisionBoxes, false);

                Vector regularStepUp = collideBoundingBoxLegacy(player, new Vector(desiredX, stepUpHeight, desiredZ), player.boundingBox, stepUpCollisionBoxes, order);

                // 1.7 clients do not have this stepping bug fix
                if (player.getClientVersion().isNewerThanOrEquals(ClientVersion.v_1_8)) {
                    Vector stepUpBugFix = collideBoundingBoxLegacy(player, new Vector(0, stepUpHeight, 0), player.boundingBox.copy().expandToCoordinate(desiredX, 0, desiredZ), stepUpCollisionBoxes, order);
                    if (stepUpBugFix.getY() < stepUpHeight) {
                        Vector stepUpBugFixResult = collideBoundingBoxLegacy(player, new Vector(desiredX, 0, desiredZ), player.boundingBox.copy().offset(0, stepUpBugFix.getY(), 0), stepUpCollisionBoxes, order).add(stepUpBugFix);
                        if (getHorizontalDistanceSqr(stepUpBugFixResult) > getHorizontalDistanceSqr(regularStepUp)) {
                            regularStepUp = stepUpBugFixResult;
                        }
                    }
                }

                if (getHorizontalDistanceSqr(regularStepUp) > getHorizontalDistanceSqr(collisionResult)) {
                    collisionResult = regularStepUp.add(collideBoundingBoxLegacy(player, new Vector(0, -regularStepUp.getY() + (player.getClientVersion().isNewerThanOrEquals(ClientVersion.v_1_14) ? desiredY : 0), 0), player.boundingBox.copy().offset(regularStepUp.getX(), regularStepUp.getY(), regularStepUp.getZ()), stepUpCollisionBoxes, order));
                }
            }

            double resultAccuracy = collisionResult.distanceSquared(bestTheoreticalCollisionResult);

            // Step movement doesn't care about ground (due to uncertainty fucking it up)
            if (player.onGround != (desiredY < 0 && desiredY != collisionResult.getY()) && !player.uncertaintyHandler.isStepMovement)
                resultAccuracy += 1;

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
        if (player.clientControlledHorizontalCollision && XMaterial.supports(8) && player.playerWorld != null) {
            WorldBorder border = player.playerWorld.getWorldBorder();
            double centerX = border.getCenter().getX();
            double centerZ = border.getCenter().getZ();

            // For some reason, the game limits the border to 29999984 blocks wide
            // TODO: Support dynamic worldborder with latency compensation
            double size = border.getSize() / 2;

            // If the player's is within 16 blocks of the worldborder, add the worldborder to the collisions (optimization)
            if (Math.abs(player.x + centerX) + 16 > size || Math.abs(player.z + centerZ) + 16 > size) {
                double minX = Math.floor(GrimMath.clamp(centerX - size, -absoluteMaxSize, absoluteMaxSize));
                double minZ = Math.floor(GrimMath.clamp(centerZ - size, -absoluteMaxSize, absoluteMaxSize));
                double maxX = Math.ceil(GrimMath.clamp(centerX + size, -absoluteMaxSize, absoluteMaxSize));
                double maxZ = Math.ceil(GrimMath.clamp(centerZ + size, -absoluteMaxSize, absoluteMaxSize));

                // If the player is fully within the worldborder
                if (player.boundingBox.minX > minX - 1.0E-7D && player.boundingBox.maxX < maxX + 1.0E-7D
                        && player.boundingBox.minZ > minZ - 1.0E-7D && player.boundingBox.maxZ < maxZ + 1.0E-7D) {
                    if (listOfBlocks == null) listOfBlocks = new ArrayList<>();

                    // South border
                    listOfBlocks.add(new SimpleCollisionBox(minX, Double.NEGATIVE_INFINITY, maxZ, maxX, Double.POSITIVE_INFINITY, maxZ, false));
                    // North border
                    listOfBlocks.add(new SimpleCollisionBox(minX, Double.NEGATIVE_INFINITY, minZ, maxX, Double.POSITIVE_INFINITY, minZ, false));
                    // East border
                    listOfBlocks.add(new SimpleCollisionBox(maxX, Double.NEGATIVE_INFINITY, minZ, maxX, Double.POSITIVE_INFINITY, maxZ, false));
                    // West border
                    listOfBlocks.add(new SimpleCollisionBox(minX, Double.NEGATIVE_INFINITY, minZ, minX, Double.POSITIVE_INFINITY, maxZ, false));

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
        final int maxSection = player.compensatedWorld.getMaxHeight() >> 4;
        final int minBlock = minSection << 4;
        final int maxBlock = (maxSection << 4) | 15;

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

                    if (section == null || section.isKnownEmpty()) { // Check for empty on 1.13+ servers
                        // empty
                        // skip to next section
                        y = (y & ~(15)) + 15; // increment by 15: iterator loop increments by the extra one
                        continue;
                    }

                    for (int currZ = minZ; currZ <= maxZ; ++currZ) {
                        for (int currX = minX; currX <= maxX; ++currX) {
                            int x = currX | chunkXGlobalPos;
                            int z = currZ | chunkZGlobalPos;

                            BaseBlockState data = section.get(x & 0xF, y & 0xF, z & 0xF);

                            // Works on both legacy and modern!  Faster than checking for material types, most common case
                            if (data.getCombinedId() == 0) continue;

                            int edgeCount = ((x == minBlockX || x == maxBlockX) ? 1 : 0) +
                                    ((y == minBlockY || y == maxBlockY) ? 1 : 0) +
                                    ((z == minBlockZ || z == maxBlockZ) ? 1 : 0);

                            if (edgeCount != 3 && (edgeCount != 1 || Materials.checkFlag(data.getMaterial(), Materials.SHAPE_EXCEEDS_CUBE))
                                    && (edgeCount != 2 || data.getMaterial() == PISTON_HEAD)) {
                                // Don't add to a list if we only care if the player intersects with the block
                                if (!onlyCheckCollide) {
                                    CollisionData.getData(data.getMaterial()).getMovementCollisionBox(player, player.getClientVersion(), data, x, y, z).downCast(listOfBlocks);
                                } else if (CollisionData.getData(data.getMaterial()).getMovementCollisionBox(player, player.getClientVersion(), data, x, y, z).isCollided(wantedBB)) {
                                    return true;
                                }
                            }
                        }
                    }
                }
            }
        }

        synchronized (player.compensatedEntities.entityMap) {
            for (PacketEntity entity : player.compensatedEntities.entityMap.values()) {
                if (entity.type == EntityType.BOAT) {
                    SimpleCollisionBox box = GetBoundingBox.getBoatBoundingBox(entity.position.getX(), entity.position.getY(), entity.position.getZ());
                    if (box.isIntersected(expandedBB)) {
                        if (listOfBlocks == null) listOfBlocks = new ArrayList<>();
                        listOfBlocks.add(box);
                    }
                }

                if (entity.type == EntityType.SHULKER) {
                    SimpleCollisionBox box = GetBoundingBox.getBoundingBoxFromPosAndSize(entity.position.getX(), entity.position.getY(), entity.position.getZ(), 1, 1);
                    if (box.isIntersected(expandedBB)) {
                        if (listOfBlocks == null) listOfBlocks = new ArrayList<>();
                        listOfBlocks.add(box);
                    }
                }
            }
        }

        return false;
    }

    private static Vector collideBoundingBoxLegacy(GrimPlayer player, Vector toCollide, SimpleCollisionBox
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

            double maxStepDown = overrideVersion || player.getClientVersion().isNewerThanOrEquals(ClientVersion.v_1_11) ? -player.getMaxUpStep() : -1 + COLLISION_EPSILON;

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

    private static boolean isAboveGround(GrimPlayer player) {
        // https://bugs.mojang.com/browse/MC-2404
        return player.lastOnGround || (player.getClientVersion().isNewerThanOrEquals(ClientVersion.v_1_16_2) && (player.fallDistance < player.getMaxUpStep() &&
                !isEmpty(player, player.boundingBox.copy().offset(0.0, player.fallDistance - player.getMaxUpStep(), 0.0))));
    }

    public static void handleInsideBlocks(GrimPlayer player) {
        // Use the bounding box for after the player's movement is applied
        SimpleCollisionBox aABB = GetBoundingBox.getCollisionBoxForPlayer(player, player.x, player.y, player.z).expand(-0.001);

        Location blockPos = new Location(player.playerWorld, aABB.minX, aABB.minY, aABB.minZ);
        Location blockPos2 = new Location(player.playerWorld, aABB.maxX, aABB.maxY, aABB.maxZ);

        if (CheckIfChunksLoaded.isChunksUnloadedAt(player, blockPos.getBlockX(), blockPos.getBlockY(), blockPos.getBlockZ(), blockPos2.getBlockX(), blockPos2.getBlockY(), blockPos2.getBlockZ()))
            return;

        for (int i = blockPos.getBlockX(); i <= blockPos2.getBlockX(); ++i) {
            for (int j = blockPos.getBlockY(); j <= blockPos2.getBlockY(); ++j) {
                for (int k = blockPos.getBlockZ(); k <= blockPos2.getBlockZ(); ++k) {
                    BaseBlockState block = player.compensatedWorld.getWrappedBlockStateAt(i, j, k);
                    Material blockType = block.getMaterial();

                    if (blockType == COBWEB) {
                        player.stuckSpeedMultiplier = new Vector(0.25, 0.05000000074505806, 0.25);
                    }

                    if (blockType == SWEET_BERRY_BUSH
                            && player.getClientVersion().isNewerThanOrEquals(ClientVersion.v_1_14)) {
                        player.stuckSpeedMultiplier = new Vector(0.800000011920929, 0.75, 0.800000011920929);
                    }

                    if (blockType == POWDER_SNOW && i == Math.floor(player.x) && j == Math.floor(player.y) && k == Math.floor(player.z)
                            && player.getClientVersion().isNewerThanOrEquals(ClientVersion.v_1_17)) {
                        player.stuckSpeedMultiplier = new Vector(0.8999999761581421, 1.5, 0.8999999761581421);
                    }

                    if (blockType == Material.SOUL_SAND && player.getClientVersion().isOlderThan(ClientVersion.v_1_15)) {
                        player.clientVelocity.setX(player.clientVelocity.getX() * 0.4D);
                        player.clientVelocity.setZ(player.clientVelocity.getZ() * 0.4D);
                    }

                    if (Materials.checkFlag(blockType, Materials.LAVA) && player.getClientVersion().isOlderThan(ClientVersion.v_1_16) && player.getClientVersion().isNewerThanOrEquals(ClientVersion.v_1_14)) {
                        player.wasTouchingLava = true;
                    }

                    if (blockType == BUBBLE_COLUMN && player.getClientVersion().isNewerThanOrEquals(ClientVersion.v_1_13)) {
                        BaseBlockState blockAbove = player.compensatedWorld.getWrappedBlockStateAt(i, j + 1, k);
                        BlockData bubbleData = ((FlatBlockState) block).getBlockData();
                        BubbleColumn bubbleColumn = (BubbleColumn) bubbleData;

                        if (player.playerVehicle != null && player.playerVehicle.type == EntityType.BOAT) {
                            if (!Materials.checkFlag(blockAbove.getMaterial(), Materials.AIR)) {
                                if (bubbleColumn.isDrag()) {
                                    player.clientVelocity.setY(Math.max(-0.3D, player.clientVelocity.getY() - 0.03D));
                                } else {
                                    player.clientVelocity.setY(Math.min(0.7D, player.clientVelocity.getY() + 0.06D));
                                }
                            }
                        } else {
                            if (Materials.checkFlag(blockAbove.getMaterial(), Materials.AIR)) {
                                for (VectorData vector : player.getPossibleVelocitiesMinusKnockback()) {
                                    if (bubbleColumn.isDrag()) {
                                        vector.vector.setY(Math.max(-0.9D, vector.vector.getY() - 0.03D));
                                    } else {
                                        vector.vector.setY(Math.min(1.8D, vector.vector.getY() + 0.1D));
                                    }
                                }
                            } else {
                                for (VectorData vector : player.getPossibleVelocitiesMinusKnockback()) {
                                    if (bubbleColumn.isDrag()) {
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

                    if (blockType == HONEY_BLOCK && player.getClientVersion().isNewerThanOrEquals(ClientVersion.v_1_15)) {
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
            double d0 = Math.abs((double) locationX + 0.5D - player.lastX);
            double d1 = Math.abs((double) locationZ + 0.5D - player.lastZ);
            // Calculate player width using bounding box, which will change while swimming or gliding
            double d2 = 0.4375D + ((player.pose.width) / 2.0F);
            return d0 + 1.0E-7D > d2 || d1 + 1.0E-7D > d2;
        }
    }

    // 0.03 hack
    public static boolean checkStuckSpeed(GrimPlayer player) {
        // Use the bounding box for after the player's movement is applied
        SimpleCollisionBox aABB = GetBoundingBox.getCollisionBoxForPlayer(player, player.x, player.y, player.z).expand(0.03);

        Location blockPos = new Location(player.playerWorld, aABB.minX, aABB.minY, aABB.minZ);
        Location blockPos2 = new Location(player.playerWorld, aABB.maxX, aABB.maxY, aABB.maxZ);

        if (CheckIfChunksLoaded.isChunksUnloadedAt(player, blockPos.getBlockX(), blockPos.getBlockY(), blockPos.getBlockZ(), blockPos2.getBlockX(), blockPos2.getBlockY(), blockPos2.getBlockZ()))
            return false;

        for (int i = blockPos.getBlockX(); i <= blockPos2.getBlockX(); ++i) {
            for (int j = blockPos.getBlockY(); j <= blockPos2.getBlockY(); ++j) {
                for (int k = blockPos.getBlockZ(); k <= blockPos2.getBlockZ(); ++k) {
                    BaseBlockState block = player.compensatedWorld.getWrappedBlockStateAt(i, j, k);
                    Material blockType = block.getMaterial();

                    if (blockType == COBWEB) {
                        return true;
                    }

                    if (blockType == SWEET_BERRY_BUSH && player.getClientVersion().isNewerThanOrEquals(ClientVersion.v_1_14)) {
                        return true;
                    }

                    if (blockType == POWDER_SNOW && i == Math.floor(player.x) && j == Math.floor(player.y) && k == Math.floor(player.z) && player.getClientVersion().isNewerThanOrEquals(ClientVersion.v_1_17)) {
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
                        if (player.getClientVersion().isNewerThanOrEquals(ClientVersion.v_1_16)) {
                            BaseBlockState data = player.compensatedWorld.getWrappedBlockStateAt(x, y, z);
                            Material mat = data.getMaterial();
                            CollisionBox box = CollisionData.getData(mat).getMovementCollisionBox(player, player.getClientVersion(), data, x, y, z);

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
        BaseBlockState data = player.compensatedWorld.getWrappedBlockStateAt(x, y, z);
        Material mat = data.getMaterial();

        // Optimization - all blocks that can suffocate must have a hitbox
        if (!Materials.checkFlag(mat, Materials.SOLID)) return false;

        // 1.13- players can not be pushed by blocks that can emit power, for some reason, while 1.14+ players can
        if (mat == OBSERVER || mat == REDSTONE_BLOCK)
            return player.getClientVersion().isNewerThan(ClientVersion.v_1_13_2);
        // Tnt only pushes on 1.14+ clients
        if (mat == TNT) return player.getClientVersion().isNewerThanOrEquals(ClientVersion.v_1_14);
        // Farmland only pushes on 1.16+ clients
        if (mat == FARMLAND) return player.getClientVersion().isNewerThanOrEquals(ClientVersion.v_1_16);
        // 1.14-1.15 doesn't push with soul sand, the rest of the versions do
        if (mat == SOUL_SAND)
            return player.getClientVersion().isNewerThanOrEquals(ClientVersion.v_1_16) || player.getClientVersion().isOlderThan(ClientVersion.v_1_14);
        // 1.13 and below exempt piston bases, while 1.14+ look to see if they are a full block or not
        if ((mat == PISTON_BASE || mat == STICKY_PISTON_BASE) && player.getClientVersion().isOlderThan(ClientVersion.v_1_14))
            return false;
        // 1.13 and below exempt ICE and FROSTED_ICE, 1.14 have them push
        if (mat == ICE || mat == FROSTED_ICE)
            return player.getClientVersion().isNewerThanOrEquals(ClientVersion.v_1_14);
        // I believe leaves and glass are consistently exempted across all versions
        if (Materials.checkFlag(mat, Materials.LEAVES) || Materials.checkFlag(mat, Materials.GLASS_BLOCK)) return false;
        // 1.16 players are pushed by dirt paths, 1.8 players don't have this block, so it gets converted to a full block
        if (mat == DIRT_PATH)
            return player.getClientVersion().isNewerThanOrEquals(ClientVersion.v_1_16) || player.getClientVersion().isOlderThan(ClientVersion.v_1_9);
        // Only 1.14+ players are pushed by beacons
        if (mat == BEACON) return player.getClientVersion().isNewerThanOrEquals(ClientVersion.v_1_14);

        // Thank god I already have the solid blocking blacklist written, but all these are exempt
        if (Materials.isSolidBlockingBlacklist(mat, player.getClientVersion())) return false;

        CollisionBox box = CollisionData.getData(mat).getMovementCollisionBox(player, player.getClientVersion(), data, x, y, z);
        return box.isFullBlock();
    }

    public static boolean hasBouncyBlock(GrimPlayer player) {
        return hasSlimeBlock(player) || hasMaterial(player, Materials.BED);
    }

    // Has slime block, or honey with the ViaVersion replacement block
    // This is terrible code lmao.  I need to refactor to add a new player bounding box, or somehow play with block mappings,
    // so I can automatically map honey -> slime and other important ViaVersion replacement blocks
    public static boolean hasSlimeBlock(GrimPlayer player) {
        return player.getClientVersion().isNewerThanOrEquals(ClientVersion.v_1_8)
                && (hasMaterial(player, SLIME_BLOCK, -1) ||
                (player.getClientVersion().isOlderThanOrEquals(ClientVersion.v_1_14_4)
                        && player.getClientVersion().isNewerThanOrEquals(ClientVersion.v_1_8)
                        && hasMaterial(player, HONEY_BLOCK, -1)));
    }

    public static boolean hasMaterial(GrimPlayer player, int materialType) {
        SimpleCollisionBox playerBB = player.boundingBox.copy().expand(0.03).offset(0, -0.04, 0);
        return hasMaterial(player, playerBB, material -> Materials.checkFlag(material, materialType));
    }

    public static boolean hasMaterial(GrimPlayer player, Material searchMat, double offset) {
        SimpleCollisionBox playerBB = GetBoundingBox.getCollisionBoxForPlayer(player, player.x, player.y, player.z).expand(0.03).offset(0, offset, 0);
        return hasMaterial(player, playerBB, material -> material == searchMat);
    }

    // Thanks Tuinity
    public static boolean hasMaterial(GrimPlayer player, SimpleCollisionBox checkBox, Predicate<Material> searchingFor) {
        int minBlockX = (int) Math.floor(checkBox.minX - COLLISION_EPSILON) - 1;
        int maxBlockX = (int) Math.floor(checkBox.maxX + COLLISION_EPSILON) + 1;
        int minBlockY = (int) Math.floor(checkBox.minY - COLLISION_EPSILON) - 1;
        int maxBlockY = (int) Math.floor(checkBox.maxY + COLLISION_EPSILON) + 1;
        int minBlockZ = (int) Math.floor(checkBox.minZ - COLLISION_EPSILON) - 1;
        int maxBlockZ = (int) Math.floor(checkBox.maxZ + COLLISION_EPSILON) + 1;

        final int minSection = player.compensatedWorld.getMinHeight() >> 4;
        final int maxSection = player.compensatedWorld.getMaxHeight() >> 4;
        final int minBlock = minSection << 4;
        final int maxBlock = (maxSection << 4) | 15;

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

                    if (section == null || section.isKnownEmpty()) { // Check for empty on 1.13+ servers
                        // empty
                        // skip to next section
                        y = (y & ~(15)) + 15; // increment by 15: iterator loop increments by the extra one
                        continue;
                    }

                    for (int currZ = minZ; currZ <= maxZ; ++currZ) {
                        for (int currX = minX; currX <= maxX; ++currX) {
                            int x = currX | chunkXGlobalPos;
                            int z = currZ | chunkZGlobalPos;

                            BaseBlockState data = section.get(x & 0xF, y & 0xF, z & 0xF);

                            if (searchingFor.test(data.getMaterial())) return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    public static boolean onClimbable(GrimPlayer player, double x, double y, double z) {
        BaseBlockState blockState = player.compensatedWorld.getWrappedBlockStateAt(x, y, z);
        Material blockMaterial = blockState.getMaterial();

        if (Materials.checkFlag(blockMaterial, Materials.CLIMBABLE)) {
            return true;
        }

        // ViaVersion replacement block -> sweet berry bush to vines
        if (blockMaterial == SWEET_BERRY_BUSH && player.getClientVersion().isOlderThan(ClientVersion.v_1_14)) {
            return true;
        }

        return trapdoorUsableAsLadder(player, x, y, z, blockState);
    }

    private static boolean trapdoorUsableAsLadder(GrimPlayer player, double x, double y, double z, BaseBlockState
            blockData) {
        if (!Materials.checkFlag(blockData.getMaterial(), Materials.TRAPDOOR)) return false;

        WrappedBlockDataValue blockDataValue = WrappedBlockData.getMaterialData(blockData);
        WrappedTrapdoor trapdoor = (WrappedTrapdoor) blockDataValue;

        if (trapdoor.isOpen()) {
            BaseBlockState blockBelow = player.compensatedWorld.getWrappedBlockStateAt(x, y - 1, z);

            if (blockBelow.getMaterial() == LADDER) {
                WrappedBlockDataValue belowData = WrappedBlockData.getMaterialData(blockBelow);

                WrappedDirectional ladder = (WrappedDirectional) belowData;
                return ladder.getDirection() == trapdoor.getDirection();
            }
        }

        return false;
    }

    private enum Axis {
        X,
        Y,
        Z
    }
}
