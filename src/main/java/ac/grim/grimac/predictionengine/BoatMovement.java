package ac.grim.grimac.predictionengine;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.predictionengine.movementTick.MovementTicker;
import ac.grim.grimac.utils.blockstate.BaseBlockState;
import ac.grim.grimac.utils.collisions.CollisionData;
import ac.grim.grimac.utils.collisions.datatypes.SimpleCollisionBox;
import ac.grim.grimac.utils.enums.BoatEntityStatus;
import ac.grim.grimac.utils.enums.MoverType;
import ac.grim.grimac.utils.math.GrimMathHelper;
import ac.grim.grimac.utils.math.VanillaMath;
import ac.grim.grimac.utils.nmsImplementations.BlockProperties;
import ac.grim.grimac.utils.nmsImplementations.Collisions;
import ac.grim.grimac.utils.nmsImplementations.XMaterial;
import org.bukkit.Material;
import org.bukkit.util.Vector;

public class BoatMovement {
    private static final Material LILY_PAD = XMaterial.LILY_PAD.parseMaterial();

    public static void doBoatMovement(GrimPlayer player) {
        // This does stuff like getting the boat's movement on the water
        new PlayerBaseTick(player).doBaseTick();

        player.boatData.oldStatus = player.boatData.status;
        player.boatData.status = getStatus(player);

        floatBoat(player);

        controlBoat(player);


        new MovementTicker(player).move(MoverType.SELF, player.clientVelocity);
        player.predictedVelocity.vector.add(new Vector(0, player.boatData.midTickY, 0));
        Collisions.handleInsideBlocks(player);

        player.boatData.midTickY = 0;
    }

    private static void floatBoat(GrimPlayer player) {
        double d0 = -0.04F;
        double d1 = player.playerVehicle.hasGravity() ? (double) -0.04F : 0.0D;
        double d2 = 0.0D;
        float invFriction = 0.05F;
        if (player.boatData.oldStatus == BoatEntityStatus.IN_AIR && player.boatData.status != BoatEntityStatus.IN_AIR && player.boatData.status != BoatEntityStatus.ON_LAND) {
            player.boatData.waterLevel = player.lastY + player.boundingBox.maxY - player.boundingBox.minY;

            player.boatData.midTickY = getWaterLevelAbove(player) - player.boundingBox.maxY - player.boundingBox.minY + 0.101D + player.boundingBox.minY;
            player.boundingBox.offset(0, player.boatData.midTickY, 0);


            player.clientVelocity.setY(0);

            player.boatData.lastYd = 0.0D;
            player.boatData.status = BoatEntityStatus.IN_WATER;
        } else {
            if (player.boatData.status == BoatEntityStatus.IN_WATER) {
                d2 = (player.boatData.waterLevel - player.lastY) / (player.boundingBox.maxY - player.boundingBox.minY);
                invFriction = 0.9F;
            } else if (player.boatData.status == BoatEntityStatus.UNDER_FLOWING_WATER) {
                d1 = -7.0E-4D;
                invFriction = 0.9F;
            } else if (player.boatData.status == BoatEntityStatus.UNDER_WATER) {
                d2 = 0.01F;
                invFriction = 0.45F;
            } else if (player.boatData.status == BoatEntityStatus.IN_AIR) {
                invFriction = 0.9F;
            } else if (player.boatData.status == BoatEntityStatus.ON_LAND) {
                invFriction = player.boatData.landFriction;
                player.boatData.landFriction /= 2.0F;
            }

            Vector vector3d = player.clientVelocity;
            player.clientVelocity.setX(vector3d.getX() * invFriction);
            player.clientVelocity.setY(vector3d.getY() + d1);
            player.clientVelocity.setZ(vector3d.getZ() * invFriction);

            player.boatData.deltaRotation *= invFriction;
            if (d2 > 0.0D) {
                double yVel = player.clientVelocity.getY();
                player.clientVelocity.setY((yVel + d2 * 0.06153846016296973D) * 0.75D);
            }
        }
    }

    private static void controlBoat(GrimPlayer player) {
        float f = 0.0F;
        if (player.vehicleHorizontal < -0.01) {
            --player.boatData.deltaRotation;
        }

        if (player.vehicleHorizontal > 0.01) {
            ++player.boatData.deltaRotation;
        }

        if (player.vehicleHorizontal != 0 && player.vehicleForward == 0) {
            f += 0.005F;
        }

        //player.boatData.yRot += player.boatData.deltaRotation;
        if (player.vehicleForward > 0.1) {
            f += 0.04F;
        }

        if (player.vehicleForward < -0.01) {
            f -= 0.005F;
        }

        player.clientVelocity.add(new Vector(VanillaMath.sin(-player.xRot * ((float) Math.PI / 180F)) * f, 0, (double) (VanillaMath.cos(player.xRot * ((float) Math.PI / 180F)) * f)));
    }

    private static BoatEntityStatus getStatus(GrimPlayer player) {
        BoatEntityStatus boatentity$status = isUnderwater(player);
        if (boatentity$status != null) {
            player.boatData.waterLevel = player.boundingBox.maxY;
            return boatentity$status;
        } else if (checkInWater(player)) {
            return BoatEntityStatus.IN_WATER;
        } else {
            float f = getGroundFriction(player);
            if (f > 0.0F) {
                player.boatData.landFriction = f;
                return BoatEntityStatus.ON_LAND;
            } else {
                return BoatEntityStatus.IN_AIR;
            }
        }
    }

    public static float getWaterLevelAbove(GrimPlayer player) {
        SimpleCollisionBox axisalignedbb = player.boundingBox;
        int i = (int) Math.floor(axisalignedbb.minX);
        int j = (int) Math.ceil(axisalignedbb.maxX);
        int k = (int) Math.floor(axisalignedbb.maxY);
        int l = (int) Math.ceil(axisalignedbb.maxY - player.boatData.lastYd);
        int i1 = (int) Math.floor(axisalignedbb.minZ);
        int j1 = (int) Math.ceil(axisalignedbb.maxZ);

        label39:
        for (int k1 = k; k1 < l; ++k1) {
            float f = 0.0F;

            for (int l1 = i; l1 < j; ++l1) {
                for (int i2 = i1; i2 < j1; ++i2) {
                    double level = player.compensatedWorld.getWaterFluidLevelAt(l1, k1, i2);

                    f = (float) Math.max(f, level);

                    if (f >= 1.0F) {
                        continue label39;
                    }
                }
            }

            if (f < 1.0F) {
                return (float) k1 + f;
            }
        }

        return (float) (l + 1);
    }

    private static BoatEntityStatus isUnderwater(GrimPlayer player) {
        SimpleCollisionBox axisalignedbb = player.boundingBox;
        double d0 = axisalignedbb.maxY + 0.001D;
        int i = GrimMathHelper.floor(axisalignedbb.minX);
        int j = GrimMathHelper.ceil(axisalignedbb.maxX);
        int k = GrimMathHelper.floor(axisalignedbb.maxY);
        int l = GrimMathHelper.ceil(d0);
        int i1 = GrimMathHelper.floor(axisalignedbb.minZ);
        int j1 = GrimMathHelper.ceil(axisalignedbb.maxZ);
        boolean flag = false;

        for (int k1 = i; k1 < j; ++k1) {
            for (int l1 = k; l1 < l; ++l1) {
                for (int i2 = i1; i2 < j1; ++i2) {
                    double level = player.compensatedWorld.getWaterFluidLevelAt(k1, l1, i2);
                    if (d0 < l1 + level) {
                        if (!player.compensatedWorld.isWaterSourceBlock(k1, l1, i2)) {
                            return BoatEntityStatus.UNDER_FLOWING_WATER;
                        }

                        flag = true;
                    }
                }
            }
        }

        return flag ? BoatEntityStatus.UNDER_WATER : null;
    }

    private static boolean checkInWater(GrimPlayer grimPlayer) {
        SimpleCollisionBox axisalignedbb = grimPlayer.boundingBox;
        int i = GrimMathHelper.floor(axisalignedbb.minX);
        int j = GrimMathHelper.ceil(axisalignedbb.maxX);
        int k = GrimMathHelper.floor(axisalignedbb.minY);
        int l = GrimMathHelper.ceil(axisalignedbb.minY + 0.001D);
        int i1 = GrimMathHelper.floor(axisalignedbb.minZ);
        int j1 = GrimMathHelper.ceil(axisalignedbb.maxZ);
        boolean flag = false;
        grimPlayer.boatData.waterLevel = Double.MIN_VALUE;

        for (int k1 = i; k1 < j; ++k1) {
            for (int l1 = k; l1 < l; ++l1) {
                for (int i2 = i1; i2 < j1; ++i2) {
                    double level = grimPlayer.compensatedWorld.getWaterFluidLevelAt(k1, l1, i2);
                    if (level > 0) {
                        float f = (float) ((float) l1 + level);
                        grimPlayer.boatData.waterLevel = Math.max(f, grimPlayer.boatData.waterLevel);
                        flag |= axisalignedbb.minY < (double) f;
                    }
                }
            }
        }

        return flag;
    }

    public static float getGroundFriction(GrimPlayer player) {
        SimpleCollisionBox axisalignedbb = player.boundingBox;
        SimpleCollisionBox axisalignedbb1 = new SimpleCollisionBox(axisalignedbb.minX, axisalignedbb.minY - 0.001D, axisalignedbb.minZ, axisalignedbb.maxX, axisalignedbb.minY, axisalignedbb.maxZ);
        int i = (int) (Math.floor(axisalignedbb1.minX) - 1);
        int j = (int) (Math.ceil(axisalignedbb1.maxX) + 1);
        int k = (int) (Math.floor(axisalignedbb1.minY) - 1);
        int l = (int) (Math.ceil(axisalignedbb1.maxY) + 1);
        int i1 = (int) (Math.floor(axisalignedbb1.minZ) - 1);
        int j1 = (int) (Math.ceil(axisalignedbb1.maxZ) + 1);

        float f = 0.0F;
        int k1 = 0;

        for (int l1 = i; l1 < j; ++l1) {
            for (int i2 = i1; i2 < j1; ++i2) {
                int j2 = (l1 != i && l1 != j - 1 ? 0 : 1) + (i2 != i1 && i2 != j1 - 1 ? 0 : 1);
                if (j2 != 2) {
                    for (int k2 = k; k2 < l; ++k2) {
                        if (j2 <= 0 || k2 != k && k2 != l - 1) {
                            BaseBlockState blockData = player.compensatedWorld.getWrappedBlockStateAt(l1, k2, i2);
                            Material blockMaterial = blockData.getMaterial();

                            if (!(blockMaterial == LILY_PAD) && CollisionData.getData(blockMaterial).getMovementCollisionBox(player, player.getClientVersion(), blockData, l1, k2, i2).isIntersected(axisalignedbb1)) {
                                f += BlockProperties.getMaterialFriction(player, blockMaterial);
                                ++k1;
                            }
                        }
                    }
                }
            }
        }

        return f / (float) k1;
    }
}
