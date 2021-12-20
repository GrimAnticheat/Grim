package ac.grim.grimac.utils.nmsutil;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.blockdata.WrappedBlockData;
import ac.grim.grimac.utils.blockdata.types.*;
import ac.grim.grimac.utils.blockstate.BaseBlockState;
import ac.grim.grimac.utils.collisions.CollisionData;
import ac.grim.grimac.utils.collisions.blocks.DoorHandler;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.util.Vector;

public class FluidTypeFlowing {
    private static final Material SOUL_SAND = XMaterial.SOUL_SAND.parseMaterial();
    private static final Material ICE = XMaterial.ICE.parseMaterial();
    private static final Material SNOW = XMaterial.SNOW.parseMaterial();
    private static final Material COMPOSTER = XMaterial.COMPOSTER.parseMaterial();
    private static final Material STICKY_PISTON = XMaterial.STICKY_PISTON.parseMaterial();
    private static final Material PISTON = XMaterial.PISTON.parseMaterial();
    private static final Material PISTON_HEAD = XMaterial.PISTON_HEAD.parseMaterial();
    private static final Material LADDER = XMaterial.LADDER.parseMaterial();

    private static final Material BEACON = XMaterial.BEACON.parseMaterial();
    private static final Material GLOWSTONE = XMaterial.GLOWSTONE.parseMaterial();
    private static final Material SEA_LANTERN = XMaterial.SEA_LANTERN.parseMaterial();
    private static final Material CONDUIT = XMaterial.CONDUIT.parseMaterial();

    public static Vector getFlow(GrimPlayer player, int originalX, int originalY, int originalZ) {
        float fluidLevel = (float) Math.min(player.compensatedWorld.getFluidLevelAt(originalX, originalY, originalZ), 8 / 9D);
        ClientVersion version = player.getClientVersion();

        if (fluidLevel == 0) return new Vector();

        double d0 = 0.0D;
        double d1 = 0.0D;
        for (BlockFace enumdirection : new BlockFace[]{BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST}) {
            int modifiedX = originalX + enumdirection.getModX();
            int modifiedZ = originalZ + enumdirection.getModZ();

            if (affectsFlow(player, originalX, originalY, originalZ, modifiedX, originalY, modifiedZ)) {
                float f = (float) Math.min(player.compensatedWorld.getFluidLevelAt(modifiedX, originalY, modifiedZ), 8 / 9D);
                float f1 = 0.0F;
                if (f == 0.0F) {
                    Material mat = player.compensatedWorld.getBukkitMaterialAt(modifiedX, originalY, modifiedZ);

                    // Grim's definition of solid is whether the block has a hitbox
                    // Minecraft is... it's whatever Mojang was feeling like, but it's very consistent
                    // Use method call to support 1.13-1.15 clients and banner oddity
                    if (Materials.isSolidBlockingBlacklist(mat, version)) {
                        if (affectsFlow(player, originalX, originalY, originalZ, modifiedX, originalY - 1, modifiedZ)) {
                            f = (float) Math.min(player.compensatedWorld.getFluidLevelAt(modifiedX, originalY - 1, modifiedZ), 8 / 9D);
                            if (f > 0.0F) {
                                f1 = fluidLevel - (f - 0.8888889F);
                            }
                        }
                    }

                } else if (f > 0.0F) {
                    f1 = fluidLevel - f;
                }

                if (f1 != 0.0F) {
                    d0 += (float) enumdirection.getModX() * f1;
                    d1 += (float) enumdirection.getModZ() * f1;
                }
            }
        }

        Vector vec3d = new Vector(d0, 0.0D, d1);

        // Fluid level 1-7 is for regular fluid heights
        // Fluid level 8-15 is for falling fluids
        if (player.compensatedWorld.isFluidFalling(originalX, originalY, originalZ)) {
            for (BlockFace enumdirection : new BlockFace[]{BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST}) {
                if (isSolidFace(player, originalX, originalY, originalZ, enumdirection) || isSolidFace(player, originalX, originalY + 1, originalZ, enumdirection)) {
                    vec3d = normalizeVectorWithoutNaN(vec3d).add(new Vector(0.0D, -6.0D, 0.0D));
                    break;
                }
            }
        }
        return normalizeVectorWithoutNaN(vec3d);
    }

    private static boolean affectsFlow(GrimPlayer player, int originalX, int originalY, int originalZ, int x2, int y2, int z2) {
        return isEmpty(player, x2, y2, z2) || isSame(player, originalX, originalY, originalZ, x2, y2, z2);
    }

    protected static boolean isSolidFace(GrimPlayer player, int originalX, int y, int originalZ, BlockFace direction) {
        int x = originalX + direction.getModX();
        int z = originalZ + direction.getModZ();

        BaseBlockState blockState = player.compensatedWorld.getWrappedBlockStateAt(x, y, z);
        WrappedBlockDataValue dataValue = WrappedBlockData.getMaterialData(blockState);
        Material blockMaterial = blockState.getMaterial();

        if (isSame(player, x, y, z, originalX, y, originalZ)) return false;
        if (blockMaterial == ICE) return false;

        // 1.11 and below clients use a different method to determine solid faces
        if (player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_12)) {
            if (blockMaterial == PISTON || blockMaterial == STICKY_PISTON) {
                WrappedPistonBase pistonBase = (WrappedPistonBase) dataValue;
                return pistonBase.getDirection().getOppositeFace() == direction ||
                        CollisionData.getData(blockMaterial).getMovementCollisionBox(player, player.getClientVersion(), blockState, 0, 0, 0).isFullBlock();
            } else if (blockMaterial == PISTON_HEAD) {
                WrappedPiston pistonHead = (WrappedPiston) dataValue;
                return pistonHead.getDirection() == direction;
            }
        }

        if (player.getClientVersion().isOlderThan(ClientVersion.V_1_12)) {
            // No bush, cocoa, wart, reed
            // No double grass, tall grass, or vine
            // No button, flower pot, ladder, lever, rail, redstone, redstone wire, skull, torch, trip wire, or trip wire hook
            // No carpet
            // No snow
            // Otherwise, solid
            return !Materials.checkFlag(blockMaterial, Materials.SOLID_BLACKLIST);
        } else if (player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_12) && player.getClientVersion().isOlderThanOrEquals(ClientVersion.V_1_13_2)) {
            // 1.12/1.13 exempts stairs, pistons, sticky pistons, and piston heads.
            // It also exempts shulker boxes, leaves, trapdoors, stained glass, beacons, cauldrons, glass, glowstone, ice, sea lanterns, and conduits.
            //
            // Everything is hardcoded, and I have attempted by best at figuring out things, although it's not perfect
            // Report bugs on GitHub, as always.  1.13 is an odd version and issues could be lurking here.
            if (Materials.checkFlag(blockMaterial, Materials.STAIRS) || Materials.checkFlag(blockMaterial, Materials.LEAVES)
                    || Materials.checkFlag(blockMaterial, Materials.SHULKER) || Materials.checkFlag(blockMaterial, Materials.GLASS_BLOCK)
                    || Materials.checkFlag(blockMaterial, Materials.TRAPDOOR))
                return false;

            if (blockMaterial == BEACON || Materials.checkFlag(blockMaterial, Materials.CAULDRON)
                    || blockMaterial == GLOWSTONE || blockMaterial == SEA_LANTERN || blockMaterial == CONDUIT)
                return false;

            if (blockMaterial == PISTON || blockMaterial == STICKY_PISTON || blockMaterial == PISTON_HEAD)
                return false;

            return blockMaterial == SOUL_SAND || (CollisionData.getData(blockMaterial).getMovementCollisionBox(player, player.getClientVersion(), blockState, x, y, z).isFullBlock());
        } else {
            if (Materials.checkFlag(blockMaterial, Materials.LEAVES)) {
                // Leaves don't have solid faces in 1.13, they do in 1.14 and 1.15, and they don't in 1.16 and beyond
                return player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_14) && player.getClientVersion().isOlderThanOrEquals(ClientVersion.V_1_15_2);
            } else if (blockMaterial == SNOW) {
                WrappedSnow snow = (WrappedSnow) dataValue;
                return snow.getLayers() == 8;
            } else if (Materials.checkFlag(blockMaterial, Materials.STAIRS)) {
                WrappedStairs stairs = (WrappedStairs) dataValue;
                return stairs.getDirection() == direction;
            } else if (blockMaterial == COMPOSTER) {
                return true;
            } else if (blockMaterial == SOUL_SAND) {
                return player.getClientVersion().isOlderThanOrEquals(ClientVersion.V_1_12_2) || player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_16);
            } else if (blockMaterial == LADDER) {
                WrappedDirectional ladder = (WrappedDirectional) dataValue;
                return ladder.getDirection().getOppositeFace() == direction;
            } else if (Materials.checkFlag(blockMaterial, Materials.TRAPDOOR)) {
                WrappedTrapdoor trapdoor = (WrappedTrapdoor) dataValue;
                return trapdoor.getDirection().getOppositeFace() == direction && trapdoor.isOpen();
            } else if (Materials.checkFlag(blockMaterial, Materials.DOOR)) {
                CollisionData data = CollisionData.getData(blockMaterial);

                if (data.dynamic instanceof DoorHandler) {
                    BlockFace dir = ((DoorHandler) data.dynamic).fetchDirection(player, player.getClientVersion(), dataValue, x, y, z);
                    return dir.getOppositeFace() == direction;
                }
            }

            // Explicitly a full block, therefore it has a full face
            return (CollisionData.getData(blockMaterial).getMovementCollisionBox(player, player.getClientVersion(), blockState, x, y, z).isFullBlock());
        }
    }

    private static Vector normalizeVectorWithoutNaN(Vector vector) {
        double var0 = vector.length();
        return var0 < 1.0E-4 ? new Vector() : vector.multiply(1 / var0);
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
