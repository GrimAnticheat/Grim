package ac.grim.grimac.checks.movement;

import ac.grim.grimac.GrimPlayer;
import ac.grim.grimac.utils.chunks.ChunkCache;
import ac.grim.grimac.utils.enums.BoatEntityStatus;
import ac.grim.grimac.utils.enums.MoverType;
import ac.grim.grimac.utils.math.Mth;
import net.minecraft.server.v1_16_R3.*;
import org.bukkit.craftbukkit.v1_16_R3.CraftWorld;
import org.bukkit.util.Vector;

public class BoatMovement {
    public static void doBoatMovement(GrimPlayer grimPlayer) {
        // This does stuff like getting the boat's movement on the water
        new PlayerBaseTick(grimPlayer).doBaseTick();

        grimPlayer.boatData.status = getStatus(grimPlayer);
        floatBoat(grimPlayer);
        controlBoat(grimPlayer);

        MovementVelocityCheck.move(grimPlayer, MoverType.SELF, grimPlayer.clientVelocity.clone().multiply(grimPlayer.stuckSpeedMultiplier));
    }

    private static void floatBoat(GrimPlayer grimPlayer) {
        double d0 = -0.04F;
        double d1 = grimPlayer.playerVehicle.hasGravity() ? (double) -0.04F : 0.0D;
        double d2 = 0.0D;
        float invFriction = 0.05F;
        if (grimPlayer.boatData.oldStatus == BoatEntityStatus.IN_AIR && grimPlayer.boatData.status != BoatEntityStatus.IN_AIR && grimPlayer.boatData.status != BoatEntityStatus.ON_LAND) {
            grimPlayer.boatData.waterLevel = grimPlayer.lastY + grimPlayer.boundingBox.c();
            grimPlayer.boatData.midTickY = getWaterLevelAbove(grimPlayer) - grimPlayer.boundingBox.c() + 0.101D;
            grimPlayer.clientVelocity.setY(0);
            grimPlayer.boatData.lastYd = 0.0D;
            grimPlayer.boatData.status = BoatEntityStatus.IN_WATER;
        } else {
            if (grimPlayer.boatData.status == BoatEntityStatus.IN_WATER) {
                d2 = (grimPlayer.boatData.waterLevel - grimPlayer.lastY) / grimPlayer.boundingBox.c();
                invFriction = 0.9F;
            } else if (grimPlayer.boatData.status == BoatEntityStatus.UNDER_FLOWING_WATER) {
                d1 = -7.0E-4D;
                invFriction = 0.9F;
            } else if (grimPlayer.boatData.status == BoatEntityStatus.UNDER_WATER) {
                d2 = 0.01F;
                invFriction = 0.45F;
            } else if (grimPlayer.boatData.status == BoatEntityStatus.IN_AIR) {
                invFriction = 0.9F;
            } else if (grimPlayer.boatData.status == BoatEntityStatus.ON_LAND) {
                invFriction = grimPlayer.boatData.landFriction;
                grimPlayer.boatData.landFriction /= 2.0F;
            }

            Vector vector3d = grimPlayer.clientVelocity;
            grimPlayer.clientVelocity.setX(vector3d.getX() * invFriction);
            grimPlayer.clientVelocity.setY(vector3d.getY() + d1);
            grimPlayer.clientVelocity.setZ(vector3d.getZ() * invFriction);

            grimPlayer.boatData.deltaRotation *= invFriction;
            if (d2 > 0.0D) {
                double yVel = grimPlayer.clientVelocity.getY();
                grimPlayer.clientVelocity.setY((yVel + d2 * 0.06153846016296973D) * 0.75D);
            }
        }
    }

    private static void controlBoat(GrimPlayer grimPlayer) {
        float f = 0.0F;
        if (grimPlayer.vehicleHorizontal < -0.01) {
            --grimPlayer.boatData.deltaRotation;
        }

        if (grimPlayer.vehicleHorizontal > 0.01) {
            ++grimPlayer.boatData.deltaRotation;
        }

        if (grimPlayer.vehicleHorizontal != 0 && grimPlayer.vehicleForward == 0) {
            f += 0.005F;
        }

        //grimPlayer.boatData.yRot += grimPlayer.boatData.deltaRotation;
        if (grimPlayer.vehicleForward > 0.1) {
            f += 0.04F;
        }

        if (grimPlayer.vehicleForward < -0.01) {
            f -= 0.005F;
        }

        grimPlayer.clientVelocity.add(new Vector(Mth.sin(-grimPlayer.boatData.yRot * ((float) Math.PI / 180F)) * f, 0, (double) (Mth.cos(grimPlayer.boatData.yRot * ((float) Math.PI / 180F)) * f)));
    }

    private static BoatEntityStatus getStatus(GrimPlayer grimPlayer) {
        BoatEntityStatus boatentity$status = isUnderwater(grimPlayer);
        if (boatentity$status != null) {
            grimPlayer.boatData.waterLevel = grimPlayer.boundingBox.maxY;
            return boatentity$status;
        } else if (checkInWater(grimPlayer)) {
            return BoatEntityStatus.IN_WATER;
        } else {
            float f = getGroundFriction(grimPlayer);
            if (f > 0.0F) {
                grimPlayer.boatData.landFriction = f;
                return BoatEntityStatus.ON_LAND;
            } else {
                return BoatEntityStatus.IN_AIR;
            }
        }
    }

    public static float getWaterLevelAbove(GrimPlayer grimPlayer) {
        ac.grim.grimac.utils.nmsImplementations.tuinityVoxelShapes.AxisAlignedBB axisalignedbb = grimPlayer.boundingBox;
        int i = (int) Math.floor(axisalignedbb.minX);
        int j = (int) Math.ceil(axisalignedbb.maxX);
        int k = (int) Math.floor(axisalignedbb.maxY);
        int l = (int) Math.ceil(axisalignedbb.maxY - grimPlayer.boatData.lastYd);
        int i1 = (int) Math.floor(axisalignedbb.minZ);
        int j1 = (int) Math.ceil(axisalignedbb.maxZ);
        BlockPosition.MutableBlockPosition mutableBlockPosition = new BlockPosition.MutableBlockPosition();

        label39:
        for (int k1 = k; k1 < l; ++k1) {
            float f = 0.0F;

            for (int l1 = i; l1 < j; ++l1) {
                for (int i2 = i1; i2 < j1; ++i2) {
                    mutableBlockPosition.d(l1, k1, i2);
                    Fluid fluidState = ChunkCache.getBlockDataAt(l1, k1, i2).getFluid();
                    if (fluidState.a(TagsFluid.WATER)) {
                        // TODO: This is not async safe!
                        f = Math.max(f, fluidState.getHeight(((CraftWorld) grimPlayer.playerWorld).getHandle(), mutableBlockPosition));
                    }

                    if (f >= 1.0F) {
                        continue label39;
                    }
                }
            }

            if (f < 1.0F) {
                return (float) mutableBlockPosition.getY() + f;
            }
        }

        return (float) (l + 1);
    }

    private static BoatEntityStatus isUnderwater(GrimPlayer grimPlayer) {
        ac.grim.grimac.utils.nmsImplementations.tuinityVoxelShapes.AxisAlignedBB axisalignedbb = grimPlayer.boundingBox;
        double d0 = axisalignedbb.maxY + 0.001D;
        int i = Mth.floor(axisalignedbb.minX);
        int j = Mth.ceil(axisalignedbb.maxX);
        int k = Mth.floor(axisalignedbb.maxY);
        int l = Mth.ceil(d0);
        int i1 = Mth.floor(axisalignedbb.minZ);
        int j1 = Mth.ceil(axisalignedbb.maxZ);
        boolean flag = false;
        BlockPosition.MutableBlockPosition blockpos$mutable = new BlockPosition.MutableBlockPosition();

        for (int k1 = i; k1 < j; ++k1) {
            for (int l1 = k; l1 < l; ++l1) {
                for (int i2 = i1; i2 < j1; ++i2) {
                    blockpos$mutable.d(k1, l1, i2);
                    Fluid fluidstate = ChunkCache.getBlockDataAt(k1, l1, i1).getFluid();
                    // TODO: This is not thread safe!
                    if (fluidstate.a(TagsFluid.WATER) && d0 < (double) ((float) blockpos$mutable.getY() + fluidstate.getHeight(grimPlayer.entityPlayer.getWorld(), blockpos$mutable))) {
                        if (!fluidstate.isSource()) {
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
        ac.grim.grimac.utils.nmsImplementations.tuinityVoxelShapes.AxisAlignedBB axisalignedbb = grimPlayer.boundingBox;
        int i = Mth.floor(axisalignedbb.minX);
        int j = Mth.ceil(axisalignedbb.maxX);
        int k = Mth.floor(axisalignedbb.minY);
        int l = Mth.ceil(axisalignedbb.minY + 0.001D);
        int i1 = Mth.floor(axisalignedbb.minZ);
        int j1 = Mth.ceil(axisalignedbb.maxZ);
        boolean flag = false;
        grimPlayer.boatData.waterLevel = Double.MIN_VALUE;
        BlockPosition.MutableBlockPosition blockpos$mutable = new BlockPosition.MutableBlockPosition();

        for (int k1 = i; k1 < j; ++k1) {
            for (int l1 = k; l1 < l; ++l1) {
                for (int i2 = i1; i2 < j1; ++i2) {
                    blockpos$mutable.d(k1, l1, i2);
                    // TODO: This is once again not thread safe!
                    Fluid fluidstate = grimPlayer.entityPlayer.getWorld().getFluid(blockpos$mutable);
                    if (fluidstate.a(TagsFluid.WATER)) {
                        float f = (float) l1 + fluidstate.getHeight(grimPlayer.entityPlayer.getWorld(), blockpos$mutable);
                        grimPlayer.boatData.waterLevel = Math.max(f, grimPlayer.boatData.waterLevel);
                        flag |= axisalignedbb.minY < (double) f;
                    }
                }
            }
        }

        return flag;
    }

    public static float getGroundFriction(GrimPlayer grimPlayer) {
        ac.grim.grimac.utils.nmsImplementations.tuinityVoxelShapes.AxisAlignedBB axisalignedbb = grimPlayer.boundingBox;
        AxisAlignedBB axisalignedbb1 = new AxisAlignedBB(axisalignedbb.minX, axisalignedbb.minY - 0.001D, axisalignedbb.minZ, axisalignedbb.maxX, axisalignedbb.minY, axisalignedbb.maxZ);
        int i = (int) (Math.floor(axisalignedbb1.minX) - 1);
        int j = (int) (Math.ceil(axisalignedbb1.maxX) + 1);
        int k = (int) (Math.floor(axisalignedbb1.minY) - 1);
        int l = (int) (Math.ceil(axisalignedbb1.maxY) + 1);
        int i1 = (int) (Math.floor(axisalignedbb1.minZ) - 1);
        int j1 = (int) (Math.ceil(axisalignedbb1.maxZ) + 1);
        VoxelShape voxelshape = VoxelShapes.a(axisalignedbb1);
        float f = 0.0F;
        int k1 = 0;
        BlockPosition.MutableBlockPosition mutableBlockPos = new BlockPosition.MutableBlockPosition();

        for (int l1 = i; l1 < j; ++l1) {
            for (int i2 = i1; i2 < j1; ++i2) {
                int j2 = (l1 != i && l1 != j - 1 ? 0 : 1) + (i2 != i1 && i2 != j1 - 1 ? 0 : 1);
                if (j2 != 2) {
                    for (int k2 = k; k2 < l; ++k2) {
                        if (j2 <= 0 || k2 != k && k2 != l - 1) {
                            mutableBlockPos.d(l1, k2, i2);
                            IBlockData blockstate = ChunkCache.getBlockDataAt(l1, k2, i2);
                            if (!(blockstate.getBlock() instanceof BlockWaterLily) && VoxelShapes.c(blockstate.getCollisionShape(null, mutableBlockPos).a(l1, k2, i2), voxelshape, OperatorBoolean.AND)) {
                                f += blockstate.getBlock().getFrictionFactor();
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
