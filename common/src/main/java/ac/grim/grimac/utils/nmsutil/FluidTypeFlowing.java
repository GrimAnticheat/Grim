package ac.grim.grimac.utils.nmsutil;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.collisions.CollisionData;
import ac.grim.grimac.utils.collisions.blocks.DoorHandler;
import ac.grim.grimac.utils.math.Vector3dm;
import ac.grim.grimac.utils.math.VectorUtils;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.world.BlockFace;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.protocol.world.states.defaulttags.BlockTags;
import com.github.retrooper.packetevents.protocol.world.states.type.StateType;
import com.github.retrooper.packetevents.protocol.world.states.type.StateTypes;
import lombok.experimental.UtilityClass;

@UtilityClass
public class FluidTypeFlowing {

    public static Vector3dm getFlow(GrimPlayer player, int originalX, int originalY, int originalZ) {
        ClientVersion version = player.getClientVersion();
        return version.isOlderThan(ClientVersion.V_1_13) ? legacy$getFlow(player, version, originalX, originalY, originalZ) : modern$getFlow(player, version, originalX, originalY, originalZ);
    }

    private static Vector3dm modern$getFlow(GrimPlayer player, ClientVersion version, int originalX, int originalY, int originalZ) {
        float fluidLevel = Math.min(player.compensatedWorld.getFluidLevelAt(originalX, originalY, originalZ), 8 / 9f);
        if (fluidLevel == 0) return new Vector3dm();

        double modX = 0.0D;
        double modZ = 0.0D;
        for (BlockFace direction : new BlockFace[]{BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST}) {
            int modifiedX = originalX + direction.getModX();
            int modifiedZ = originalZ + direction.getModZ();

            if (affectsFlow(player, originalX, originalY, originalZ, modifiedX, originalY, modifiedZ)) {
                float adjacentLevel = Math.min(player.compensatedWorld.getFluidLevelAt(modifiedX, originalY, modifiedZ), 8 / 9f);
                float flow = 0.0F;
                if (adjacentLevel == 0.0F) {
                    StateType mat = player.compensatedWorld.getBlockType(modifiedX, originalY, modifiedZ);

                    // Grim's definition of solid is whether the block has a hitbox
                    // Minecraft is... it's whatever Mojang was feeling like, but it's very consistent
                    // Use method call to support 1.13-1.15 clients and banner oddity
                    if (Materials.isSolidBlockingBlacklist(mat, version)) {
                        if (affectsFlow(player, originalX, originalY, originalZ, modifiedX, originalY - 1, modifiedZ)) {
                            adjacentLevel = Math.min(player.compensatedWorld.getFluidLevelAt(modifiedX, originalY - 1, modifiedZ), 8 / 9f);
                            if (adjacentLevel > 0.0F) {
                                flow = fluidLevel - (adjacentLevel - 0.8888889F);
                            }
                        }
                    }
                } else if (adjacentLevel > 0.0F) {
                    flow = fluidLevel - adjacentLevel;
                }

                if (flow != 0.0F) {
                    modX += direction.getModX() * flow;
                    modZ += direction.getModZ() * flow;
                }
            }
        }

        Vector3dm vec3d = new Vector3dm(modX, 0.0D, modZ);

        // Fluid level 1-7 is for regular fluid heights
        // Fluid level 8-15 is for falling fluids
        WrappedBlockState state = player.compensatedWorld.getBlock(originalX, originalY, originalZ);
        if ((state.getType() == StateTypes.WATER || state.getType() == StateTypes.LAVA) && state.getLevel() >= 8) {
            for (BlockFace direction : new BlockFace[]{BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST}) {
                if (isSolidFace(player, originalX, originalY, originalZ, direction) || isSolidFace(player, originalX, originalY + 1, originalZ, direction)) {
                    vec3d = VectorUtils.normalize(player, vec3d).add(0.0D, -6.0D, 0.0D);
                    break;
                }
            }
        }

        return VectorUtils.normalize(player, vec3d);
    }

    private static Vector3dm legacy$getFlow(GrimPlayer player, ClientVersion version, int originalX, int originalY, int originalZ) {
        WrappedBlockState state = player.compensatedWorld.getBlock(originalX, originalY, originalZ);
        boolean water = Materials.isWater(player.getClientVersion(), state);
        if (!water && state.getType() != StateTypes.LAVA) return new Vector3dm();

        int fluidLevel = legacy$getLiquidDepth(player, originalX, originalY, originalZ, water);
        if (fluidLevel < 0) return new Vector3dm();

        double modX = 0.0D;
        double modZ = 0.0D;
        for (BlockFace direction : new BlockFace[]{BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST}) {
            int modifiedX = originalX + direction.getModX();
            int modifiedZ = originalZ + direction.getModZ();
            int adjacentLevel = legacy$getLiquidDepth(player, modifiedX, originalY, modifiedZ, water);

            if (adjacentLevel < 0) {
                StateType mat = player.compensatedWorld.getBlockType(modifiedX, originalY, modifiedZ);
                if (Materials.isSolidBlockingBlacklist(mat, version)) {
                    adjacentLevel = legacy$getLiquidDepth(player, modifiedX, originalY - 1, modifiedZ, water);
                    if (adjacentLevel >= 0) {
                        int flow = adjacentLevel - (fluidLevel - 8);
                        modX += direction.getModX() * flow;
                        modZ += direction.getModZ() * flow;
                    }
                }
            } else {
                int flow = adjacentLevel - fluidLevel;
                modX += direction.getModX() * flow;
                modZ += direction.getModZ() * flow;
            }
        }

        Vector3dm vec3d = new Vector3dm(modX, 0.0D, modZ);
        if (state.getLevel() >= 8) {
            for (BlockFace direction : new BlockFace[]{BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST}) {
                if (isSolidFace(player, originalX, originalY, originalZ, direction) || isSolidFace(player, originalX, originalY + 1, originalZ, direction)) {
                    vec3d = VectorUtils.normalize(player, vec3d).add(0.0D, -6.0D, 0.0D);
                    break;
                }
            }
        }

        return VectorUtils.normalize(player, vec3d);
    }

    private static int legacy$getLiquidDepth(GrimPlayer player, int x, int y, int z, boolean water) {
        WrappedBlockState state = player.compensatedWorld.getBlock(x, y, z);
        if (water ? !Materials.isWater(player.getClientVersion(), state) : state.getType() != StateTypes.LAVA) {
            return -1;
        }

        int level = state.getLevel();
        return level >= 8 ? 0 : level;
    }

    private static boolean affectsFlow(GrimPlayer player, int originalX, int originalY, int originalZ, int x2, int y2, int z2) {
        return isEmpty(player, x2, y2, z2) || isSame(player, originalX, originalY, originalZ, x2, y2, z2);
    }

    private static boolean isSolidFace(GrimPlayer player, int originalX, int y, int originalZ, BlockFace direction) {
        int x = originalX + direction.getModX();
        int z = originalZ + direction.getModZ();

        WrappedBlockState data = player.compensatedWorld.getBlock(x, y, z);
        StateType type = data.getType();

        if (isSame(player, x, y, z, originalX, y, originalZ)) return false;
        if (type == StateTypes.ICE) return false;

        // 1.11 and below clients use a different method to determine solid faces
        if (player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_12)) {
            if (type == StateTypes.PISTON || type == StateTypes.STICKY_PISTON) {
                return data.getFacing().getOppositeFace() == direction ||
                        CollisionData.getData(type).getMovementCollisionBox(player, player.getClientVersion(), data, 0, 0, 0).isFullBlock();
            } else if (type == StateTypes.PISTON_HEAD) {
                return data.getFacing() == direction;
            }
        }

        if (player.getClientVersion().isOlderThan(ClientVersion.V_1_12)) {
            // No bush, cocoa, wart, reed
            // No double grass, tall grass, or vine
            // No button, flower pot, ladder, lever, rail, redstone, redstone wire, skull, torch, trip wire, or trip wire hook
            // No carpet
            // No snow
            // Otherwise, solid
            return !Materials.isSolidBlockingBlacklist(type, player.getClientVersion());
        } else if (player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_12) && player.getClientVersion().isOlderThanOrEquals(ClientVersion.V_1_13_2)) {
            // 1.12/1.13 exempts stairs, pistons, sticky pistons, and piston heads.
            // It also exempts shulker boxes, leaves, trapdoors, stained glass, beacons, cauldrons, glass, glowstone, ice, sea lanterns, and conduits.
            //
            // Everything is hardcoded, and I have attempted by best at figuring out things, although it's not perfect
            // Report bugs on GitHub, as always.  1.13 is an odd version and issues could be lurking here.
            if (Materials.isStairs(type) || Materials.isLeaves(type)
                    || Materials.isShulker(type) || Materials.isGlassBlock(type)
                    || BlockTags.TRAPDOORS.contains(type))
                return false;

            if (type == StateTypes.BEACON || BlockTags.CAULDRONS.contains(type)
                    || type == StateTypes.GLOWSTONE || type == StateTypes.SEA_LANTERN || type == StateTypes.CONDUIT)
                return false;

            if (type == StateTypes.PISTON || type == StateTypes.STICKY_PISTON || type == StateTypes.PISTON_HEAD)
                return false;

            return type == StateTypes.SOUL_SAND || (CollisionData.getData(type).getMovementCollisionBox(player, player.getClientVersion(), data, x, y, z).isFullBlock());
        } else {
            if (Materials.isLeaves(type)) {
                // Leaves don't have solid faces in 1.13, they do in 1.14 and 1.15, and they don't in 1.16 and beyond
                return player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_14) && player.getClientVersion().isOlderThanOrEquals(ClientVersion.V_1_15_2);
            } else if (type == StateTypes.SNOW) {
                return data.getLayers() == 8;
            } else if (Materials.isStairs(type)) {
                return data.getFacing() == direction;
            } else if (type == StateTypes.COMPOSTER) {
                return true;
            } else if (type == StateTypes.SOUL_SAND) {
                return player.getClientVersion().isOlderThanOrEquals(ClientVersion.V_1_12_2) || player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_16);
            } else if (type == StateTypes.LADDER) {
                return data.getFacing().getOppositeFace() == direction;
            } else if (BlockTags.TRAPDOORS.contains(type)) {
                return data.getFacing().getOppositeFace() == direction && data.isOpen();
            } else if (BlockTags.DOORS.contains(type)) {
                CollisionData collisionData = CollisionData.getData(type);

                if (collisionData.dynamic instanceof DoorHandler) {
                    BlockFace dir = ((DoorHandler) collisionData.dynamic).fetchDirection(player, player.getClientVersion(), data, x, y, z);
                    return dir.getOppositeFace() == direction;
                }
            }

            // Explicitly a full block, therefore it has a full face
            return (CollisionData.getData(type).getMovementCollisionBox(player, player.getClientVersion(), data, x, y, z).isFullBlock());
        }
    }

    public static boolean isEmpty(GrimPlayer player, int x, int y, int z) {
        return player.compensatedWorld.getFluidLevelAt(x, y, z) == 0;
    }

    // Check if both are a type of water or both are a type of lava
    // This is a bit slow... but I don't see a better way to do it with the bukkit api and no nms
    public static boolean isSame(GrimPlayer player, int x1, int y1, int z1, int x2, int y2, int z2) {
        return player.compensatedWorld.getWaterFluidLevelAt(x1, y1, z1) > 0 &&
                player.compensatedWorld.getWaterFluidLevelAt(x2, y2, z2) > 0 ||
                player.compensatedWorld.getLavaFluidLevelAt(x1, y1, z1) > 0 &&
                        player.compensatedWorld.getLavaFluidLevelAt(x2, y2, z2) > 0;
    }
}
