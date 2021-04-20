package ac.grim.grimac.checks.movement;

import ac.grim.grimac.GrimPlayer;

public class BoatMovement {
    public static void doBoatMovement(GrimPlayer grimPlayer) {
        // This does stuff like getting the boat's movement on the water
        new PlayerBaseTick(grimPlayer).doBaseTick();
    }

    /*private void floatBoat(GrimPlayer grimPlayer) {
        double d0 = -0.04F;
        double d1 = grimPlayer.playerVehicle.hasGravity() ? (double) -0.04F : 0.0D;
        double d2 = 0.0D;
        float invFriction = 0.05F;
        if (grimPlayer.boatData.oldStatus == BoatEntityStatus.IN_AIR && grimPlayer.boatData.status != BoatEntityStatus.IN_AIR && grimPlayer.boatData.status != BoatEntityStatus.ON_LAND) {
            grimPlayer.boatData.waterLevel = grimPlayer.lastY + grimPlayer.boundingBox.c();
            grimPlayer.boatData.midTickY = getWaterLevelAbove() - grimPlayer.boundingBox.c() + 0.101D;
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
                invFriction = this.landFriction /= 2.0F;
            }

            Vector vector3d = this.getDeltaMovement();
            this.setDeltaMovement(vector3d.x * (double) this.invFriction, vector3d.y + d1, vector3d.z * (double) this.invFriction);
            this.deltaRotation *= this.invFriction;
            if (d2 > 0.0D) {
                Vector vector3d1 = this.getDeltaMovement();
                this.setDeltaMovement(vector3d1.x, (vector3d1.y + d2 * 0.06153846016296973D) * 0.75D, vector3d1.z);
            }
        }
    }

    private void controlBoat() {
        float f = 0.0F;
        if (this.inputLeft) {
            --this.deltaRotation;
        }

        if (this.inputRight) {
            ++this.deltaRotation;
        }

        if (this.inputRight != this.inputLeft && !this.inputUp && !this.inputDown) {
            f += 0.005F;
        }

        this.yRot += this.deltaRotation;
        if (this.inputUp) {
            f += 0.04F;
        }

        if (this.inputDown) {
            f -= 0.005F;
        }

        this.setDeltaMovement(this.getDeltaMovement().add((double) (MathHelper.sin(-this.yRot * ((float) Math.PI / 180F)) * f), 0.0D, (double) (MathHelper.cos(this.yRot * ((float) Math.PI / 180F)) * f)));
        //this.setPaddleState(this.inputRight && !this.inputLeft || this.inputUp, this.inputLeft && !this.inputRight || this.inputUp);
    }

    private BoatEntityStatus getStatus(GrimPlayer grimPlayer) {
        BoatEntityStatus boatentity$status = this.isUnderwater();
        if (boatentity$status != null) {
            grimPlayer.boatData.waterLevel = grimPlayer.boundingBox.maxY;
            return boatentity$status;
        } else if (checkInWater(grimPlayer)) {
            return BoatEntityStatus.IN_WATER;
        } else {
            float f = this.getGroundFriction();
            if (f > 0.0F) {
                this.landFriction = f;
                return BoatEntityStatus.ON_LAND;
            } else {
                return BoatEntityStatus.IN_AIR;
            }
        }
    }

    public float getWaterLevelAbove(GrimPlayer grimPlayer) {
        AxisAlignedBB axisalignedbb = grimPlayer.boundingBox;
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

    public float getGroundFriction(GrimPlayer grimPlayer) {
        AxisAlignedBB axisalignedbb = grimPlayer.boundingBox;
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
                            if (!(blockstate.getBlock() instanceof BlockWaterLily) && VoxelShapes.joinIsNotEmpty(blockstate.getCollisionShape(this.level, mutableBlockPos).move((double) l1, (double) k2, (double) i2), voxelshape, IBooleanFunction.AND)) {
                                f += blockstate.getBlock().getFriction();
                                ++k1;
                            }
                        }
                    }
                }
            }
        }

        return f / (float) k1;
    }*/
}
