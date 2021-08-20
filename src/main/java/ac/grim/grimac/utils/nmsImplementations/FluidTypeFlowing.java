package ac.grim.grimac.utils.nmsImplementations;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.blockdata.WrappedBlockData;
import ac.grim.grimac.utils.blockdata.types.*;
import ac.grim.grimac.utils.blockstate.BaseBlockState;
import ac.grim.grimac.utils.collisions.CollisionData;
import io.github.retrooper.packetevents.utils.player.ClientVersion;
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

    private static final Material BEACON = XMaterial.BEACON.parseMaterial();
    private static final Material GLOWSTONE = XMaterial.GLOWSTONE.parseMaterial();
    private static final Material SEA_LANTERN = XMaterial.SEA_LANTERN.parseMaterial();
    private static final Material CONDUIT = XMaterial.CONDUIT.parseMaterial();

    public static Vector getFlow(GrimPlayer player, int originalX, int originalY, int originalZ) {
        float fluidLevel = (float) Math.min(player.compensatedWorld.getFluidLevelAt(originalX, originalY, originalZ), 8 / 9D);

        if (fluidLevel == 0) return new Vector();

        double d0 = 0.0D;
        double d1 = 0.0D;
        for (BlockFace enumdirection : new BlockFace[]{BlockFace.WEST, BlockFace.EAST, BlockFace.NORTH, BlockFace.SOUTH}) {
            int modifiedX = originalX + enumdirection.getModX();
            int modifiedZ = originalZ + enumdirection.getModZ();

            if (affectsFlow(player, originalX, originalY, originalZ, modifiedX, originalY, modifiedZ)) {
                float f = (float) Math.min(player.compensatedWorld.getFluidLevelAt(modifiedX, originalY, modifiedZ), 8 / 9D);
                float f1 = 0.0F;
                if (f == 0.0F) {
                    if (!Materials.checkFlag(player.compensatedWorld.getBukkitMaterialAt(modifiedX, originalY, modifiedZ), Materials.SOLID)) {
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
            for (BlockFace enumdirection : new BlockFace[]{BlockFace.WEST, BlockFace.EAST, BlockFace.NORTH, BlockFace.SOUTH}) {
                if (isSolidFace(player, originalX, originalY, originalZ, enumdirection)) {
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

    // TODO: Stairs might be broken, can't be sure until I finish the dynamic bounding boxes
    protected static boolean isSolidFace(GrimPlayer player, int originalX, int originalY, int originalZ, BlockFace direction) {
        int x = originalX + direction.getModX();
        int z = originalZ + direction.getModZ();

        boolean isSolid = false;

        for (int modifyY = 0; modifyY <= 1; modifyY++) {
            int y = originalY + modifyY;
            BaseBlockState blockState = player.compensatedWorld.getWrappedBlockStateAt(x, y, z);
            Material blockMaterial = blockState.getMaterial();

            if (!isSame(player, x, y, z, originalX, originalY, originalZ)) {
                if (player.getClientVersion().isNewerThanOrEquals(ClientVersion.v_1_13) && player.getClientVersion().isOlderThanOrEquals(ClientVersion.v_1_13_2)) {
                    // 1.13 exempts stairs, pistons, sticky pistons, and piston heads.
                    // It also exempts shulker boxes, leaves, trapdoors, stained glass, beacons, cauldrons, glass, glowstone, ice, sea lanterns, and conduits.
                    if (Materials.checkFlag(blockMaterial, Materials.STAIRS) || Materials.checkFlag(blockMaterial, Materials.LEAVES)
                            || Materials.checkFlag(blockMaterial, Materials.SHULKER) || Materials.checkFlag(blockMaterial, Materials.GLASS_BLOCK)
                            || Materials.checkFlag(blockMaterial, Materials.TRAPDOOR))
                        continue;

                    if (blockMaterial == BEACON || Materials.checkFlag(blockMaterial, Materials.CAULDRON) || blockMaterial == GLOWSTONE
                            || blockMaterial == SEA_LANTERN || blockMaterial == CONDUIT || blockMaterial == ICE)
                        continue;

                    if (blockMaterial == PISTON || blockMaterial == STICKY_PISTON || blockMaterial == PISTON_HEAD)
                        continue;

                    isSolid = CollisionData.getData(blockMaterial).getMovementCollisionBox(player, player.getClientVersion(), blockState, 0, 0, 0).isFullBlock();

                } else if (blockMaterial == SNOW) {
                    WrappedBlockDataValue dataValue = WrappedBlockData.getMaterialData(blockState);

                    WrappedSnow snow = (WrappedSnow) dataValue;

                    isSolid = snow.getLayers() == 8;
                } else if (Materials.checkFlag(blockMaterial, Materials.LEAVES)) {
                    // Leaves don't have solid faces in 1.13, they do in 1.14 and 1.15, and they don't in 1.16 and beyond
                    isSolid = player.getClientVersion().isNewerThanOrEquals(ClientVersion.v_1_14) && player.getClientVersion().isOlderThanOrEquals(ClientVersion.v_1_15_2);
                } else if (Materials.checkFlag(blockMaterial, Materials.STAIRS)) {
                    WrappedBlockDataValue dataValue = WrappedBlockData.getMaterialData(blockState);
                    WrappedStairs stairs = (WrappedStairs) dataValue;
                    isSolid = stairs.getDirection() == direction;
                } else if (Materials.checkFlag(blockMaterial, Materials.DOOR)) {
                    WrappedBlockDataValue dataValue = WrappedBlockData.getMaterialData(blockState);
                    WrappedDoor door = (WrappedDoor) dataValue;
                    BlockFace realBBDirection;

                    // Thankfully we only have to do this for 1.13+ clients
                    // Meaning we don't have to grab the data below the door for 1.12- players
                    // as 1.12- players do not run this code
                    boolean flag = !door.getOpen();
                    boolean flag1 = door.isRightHinge();
                    switch (door.getDirection()) {
                        case EAST:
                        default:
                            realBBDirection = flag ? BlockFace.EAST : (flag1 ? BlockFace.NORTH : BlockFace.SOUTH);
                            break;
                        case SOUTH:
                            realBBDirection = flag ? BlockFace.SOUTH : (flag1 ? BlockFace.EAST : BlockFace.WEST);
                            break;
                        case WEST:
                            realBBDirection = flag ? BlockFace.WEST : (flag1 ? BlockFace.SOUTH : BlockFace.NORTH);
                            break;
                        case NORTH:
                            realBBDirection = flag ? BlockFace.NORTH : (flag1 ? BlockFace.WEST : BlockFace.EAST);
                            break;
                    }

                    isSolid = realBBDirection.getOppositeFace() == direction;
                } else if (blockMaterial == PISTON || blockMaterial == STICKY_PISTON) {
                    WrappedBlockDataValue dataValue = WrappedBlockData.getMaterialData(blockState);
                    WrappedPistonBase pistonBase = (WrappedPistonBase) dataValue;
                    isSolid = pistonBase.getDirection().getOppositeFace() == direction ||
                            CollisionData.getData(blockMaterial).getMovementCollisionBox(player, player.getClientVersion(), blockState, 0, 0, 0).isFullBlock();
                } else if (blockMaterial == PISTON_HEAD) {
                    WrappedBlockDataValue dataValue = WrappedBlockData.getMaterialData(blockState);
                    WrappedPiston pistonHead = (WrappedPiston) dataValue;
                    isSolid = pistonHead.getDirection() == direction;
                } else if (blockMaterial == COMPOSTER) {
                    isSolid = true;
                } else if (blockMaterial == SOUL_SAND) {
                    isSolid = player.getClientVersion().isOlderThanOrEquals(ClientVersion.v_1_12_2) || player.getClientVersion().isNewerThanOrEquals(ClientVersion.v_1_16);
                } else if (blockMaterial == ICE) {
                    isSolid = false;
                } else {
                    isSolid = CollisionData.getData(blockMaterial).getMovementCollisionBox(player, player.getClientVersion(), blockState, 0, 0, 0).isFullBlock();
                }
            }

            if (isSolid)
                return true;
        }

        return false;
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
