package ac.grim.grimac.utils.nmsImplementations;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.blockdata.WrappedBlockData;
import ac.grim.grimac.utils.blockdata.WrappedBlockDataValue;
import ac.grim.grimac.utils.blockdata.WrappedSnow;
import ac.grim.grimac.utils.blockstate.BaseBlockState;
import ac.grim.grimac.utils.collisions.Materials;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.util.Vector;

public class FluidTypeFlowing {
    private static final Material SOUL_SAND = XMaterial.SOUL_SAND.parseMaterial();
    private static final Material ICE = XMaterial.ICE.parseMaterial();
    private static final Material SNOW = XMaterial.SNOW.parseMaterial();

    public static Vector getFlow(GrimPlayer player, int originalX, int originalY, int originalZ) {
        float fluidLevel = (float) player.compensatedWorld.getFluidLevelAt(originalX, originalY, originalZ);

        if (fluidLevel == 0) return new Vector();

        double d0 = 0.0D;
        double d1 = 0.0D;
        for (BlockFace enumdirection : new BlockFace[]{BlockFace.WEST, BlockFace.EAST, BlockFace.NORTH, BlockFace.SOUTH}) {
            int modifiedX = originalX;
            int modifiedZ = originalZ;

            switch (enumdirection) {
                case EAST:
                    modifiedX += 1;
                    break;
                case WEST:
                    modifiedX -= 1;
                    break;
                case NORTH:
                    modifiedZ -= 1;
                    break;
                default:
                case SOUTH:
                    modifiedZ += 1;
                    break;
            }

            if (affectsFlow(player, originalX, originalY, originalZ, modifiedX, originalY, modifiedZ)) {
                float f = (float) player.compensatedWorld.getFluidLevelAt(modifiedX, originalY, modifiedZ);
                float f1 = 0.0F;
                if (f == 0.0F) {
                    if (!Materials.checkFlag(player.compensatedWorld.getBukkitMaterialAt(modifiedX, originalY, modifiedZ), Materials.SOLID)) {
                        if (affectsFlow(player, originalX, originalY, originalZ, modifiedX, originalY - 1, modifiedZ)) {
                            f = (float) player.compensatedWorld.getFluidLevelAt(modifiedX, originalY - 1, modifiedZ);
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
                int modifiedX = originalX;
                int modifiedZ = originalZ;

                switch (enumdirection) {
                    case EAST:
                        modifiedX += 1;
                        break;
                    case WEST:
                        modifiedX -= 1;
                        break;
                    case NORTH:
                        modifiedZ -= 1;
                        break;
                    default:
                    case SOUTH:
                        modifiedZ += 1;
                        break;
                }

                if (isSolidFace(player, originalX, originalY, originalZ, modifiedX, originalY, modifiedZ) || isSolidFace(player, originalX, originalY, originalZ, modifiedX, originalY + 1, modifiedZ)) {
                    vec3d = normalizeVectorWithoutNaN(vec3d).add(new Vector(0.0D, -6.0D, 0.0D));
                    break;
                }
            }
        }

        return normalizeVectorWithoutNaN(vec3d);
    }

    private static boolean affectsFlow(GrimPlayer player, int originalX, int originalY, int originalZ, int x2, int y2, int z2) {
        return isEmpty(player, originalX, originalY, originalZ) || isSame(player, originalX, originalY, originalZ, x2, y2, z2);
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

    // TODO: Stairs might be broken, can't be sure until I finish the dynamic bounding boxes
    protected static boolean isSolidFace(GrimPlayer player, int originalX, int originalY, int originalZ, int x, int y, int z) {
        BaseBlockState blockState = player.compensatedWorld.getWrappedBlockStateAt(x, y, z);
        Material blockMaterial = blockState.getMaterial();

        // Removed a check for enumdirection of up, as that is impossible for the code we use
        if (isSame(player, x, y, z, originalX, originalY, originalZ)) {
            return false;
        } else {
            // Short circuit out getting block collision for shulker boxes, as they read the world sync
            // Soul sand is always true
            // Leaves are always false despite a full bounding box
            // Snow uses different bounding box getters than collisions
            if (blockMaterial == SNOW) {
                WrappedBlockDataValue dataValue = WrappedBlockData.getMaterialData(SNOW);
                dataValue.getData(blockState);

                WrappedSnow snow = (WrappedSnow) dataValue;

                return snow.getLayers() == 8;
            }

            return !Materials.checkFlag(blockMaterial, Materials.LEAVES) && (blockMaterial == SOUL_SAND || blockMaterial != ICE && CollisionData.getData(blockMaterial).getMovementCollisionBox(blockState, 0, 0, 0, player.getClientVersion()).isFullBlock());
        }
    }

    private static Vector normalizeVectorWithoutNaN(Vector vector) {
        double var0 = vector.length();
        return var0 < 1.0E-4 ? new Vector() : vector.multiply(1 / var0);
    }
}
