package ac.grim.grimac.predictionengine;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.collisions.datatypes.SimpleCollisionBox;
import ac.grim.grimac.utils.enums.FluidTag;
import ac.grim.grimac.utils.enums.Pose;
import ac.grim.grimac.utils.math.GrimMathHelper;
import ac.grim.grimac.utils.nmsImplementations.*;
import io.github.retrooper.packetevents.utils.player.ClientVersion;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Boat;
import org.bukkit.util.Vector;

public class PlayerBaseTick {
    GrimPlayer player;

    public PlayerBaseTick(GrimPlayer player) {
        this.player = player;
    }

    public static boolean canEnterPose(GrimPlayer player, Pose pose, double x, double y, double z) {
        return Collisions.isEmpty(player, getBoundingBoxForPose(pose, x, y, z).expand(-1.0E-7D));
    }

    protected static SimpleCollisionBox getBoundingBoxForPose(Pose pose, double x, double y, double z) {
        float radius = pose.width / 2.0F;
        return new SimpleCollisionBox(x - radius, y, z - radius, x + radius, y + pose.height, z + radius);
    }

    public void doBaseTick() {
        // Keep track of basetick stuff
        player.baseTickSet = new Vector();
        player.baseTickAddition = new Vector(0, 0, 0);

        // You cannot crouch while flying, only shift - could be specific to 1.14?
        // LocalPlayer:aiStep line 728
        if (player.wasTouchingWater && player.isSneaking && !player.specialFlying && !player.inVehicle) {
            player.baseTickAddVector(new Vector(0, -0.04, 0));
        }

        if (player.specialFlying && player.isSneaking && !player.inVehicle) {
            player.baseTickAddVector(new Vector(0, player.flySpeed * -3, 0));
        }

        updateInWaterStateAndDoFluidPushing();
        updateFluidOnEyes();
        updateSwimming();

        // LocalPlayer:aiStep determining crouching
        // Tick order is entityBaseTick and then the aiStep stuff
        // This code is in the wrong place, I'll fix it later
        player.isCrouching = !player.wasFlying && !player.isSwimming && canEnterPose(player, Pose.CROUCHING, player.lastX, player.lastY, player.lastZ)
                && (player.wasSneaking || player.bukkitPlayer.isSleeping() || !canEnterPose(player, Pose.STANDING, player.lastX, player.lastY, player.lastZ));
        player.isSlowMovement = player.isCrouching || (player.pose == Pose.SWIMMING && !player.wasTouchingWater);


        // LocalPlayer:aiStep line 647
        // Players in boats don't care about being in blocks
        if (!player.inVehicle) {
            this.moveTowardsClosestSpace(player.lastX - (player.boundingBox.maxX - player.boundingBox.minX) * 0.35, player.lastZ + (player.boundingBox.maxZ - player.boundingBox.minZ) * 0.35);
            this.moveTowardsClosestSpace(player.lastX - (player.boundingBox.maxX - player.boundingBox.minX) * 0.35, player.lastZ - (player.boundingBox.maxZ - player.boundingBox.minZ) * 0.35);
            this.moveTowardsClosestSpace(player.lastX + (player.boundingBox.maxX - player.boundingBox.minX) * 0.35, player.lastZ - (player.boundingBox.maxZ - player.boundingBox.minZ) * 0.35);
            this.moveTowardsClosestSpace(player.lastX + (player.boundingBox.maxX - player.boundingBox.minX) * 0.35, player.lastZ + (player.boundingBox.maxZ - player.boundingBox.minZ) * 0.35);
        }

        float f = BlockProperties.getBlockSpeedFactor(player);
        player.blockSpeedMultiplier = new Vector(f, 1.0, f);

        updatePlayerPose();
    }

    protected void updatePlayerPose() {
        if (canEnterPose(player, Pose.SWIMMING, player.x, player.y, player.z)) {
            Pose pose;
            if (player.isGliding) {
                pose = Pose.FALL_FLYING;
            } else if (player.bukkitPlayer.isSleeping()) {
                pose = Pose.SLEEPING;
            } else if (player.isSwimming) {
                pose = Pose.SWIMMING;
            } else if (XMaterial.supports(13) && player.bukkitPlayer.isRiptiding()) {
                pose = Pose.SPIN_ATTACK;
            } else if (player.isSneaking && !player.specialFlying) {
                pose = Pose.CROUCHING;
            } else {
                pose = Pose.STANDING;
            }

            if (!player.inVehicle && !canEnterPose(player, pose, player.x, player.y, player.z)) {
                if (canEnterPose(player, Pose.CROUCHING, player.x, player.y, player.z)) {
                    pose = Pose.CROUCHING;
                } else {
                    pose = Pose.SWIMMING;
                }
            }

            player.pose = pose;
        }
    }

    // Entity line 937
    public void updateInWaterStateAndDoFluidPushing() {
        updateInWaterStateAndDoWaterCurrentPushing();
        double d = player.playerWorld.getEnvironment() == World.Environment.NETHER ? 0.007 : 0.0023333333333333335;
        player.lastTouchingLava = player.wasTouchingLava;
        player.wasTouchingLava = this.updateFluidHeightAndDoFluidPushing(FluidTag.LAVA, d);
    }

    private void updateFluidOnEyes() {
        player.wasEyeInWater = player.isEyeInFluid(FluidTag.WATER);
        player.fluidOnEyes = null;
        double d0 = player.lastY + GetBoundingBox.getEyeHeight(player.isCrouching, player.isGliding, player.isSwimming, player.bukkitPlayer.isSleeping(), player.getClientVersion()) - 0.1111111119389534D;

        if (player.playerVehicle instanceof Boat && !player.boatData.boatUnderwater && player.boundingBox.maxY >= d0 && player.boundingBox.minY <= d0) {
            return;
        }

        double d1 = (float) Math.floor(d0) + player.compensatedWorld.getWaterFluidLevelAt((int) Math.floor(player.lastX), (int) Math.floor(d0), (int) Math.floor(player.lastZ));
        if (d1 > d0) {
            player.fluidOnEyes = FluidTag.WATER;
            return;
        }

        d1 = (float) Math.floor(d0) + player.compensatedWorld.getWaterFluidLevelAt((int) Math.floor(player.lastX), (int) Math.floor(d0), (int) Math.floor(player.lastZ));
        if (d1 > d0) {
            player.fluidOnEyes = FluidTag.LAVA;
        }
    }

    public void updateSwimming() {
        // This doesn't seem like the right place for determining swimming, but it's fine for now
        if (player.getClientVersion().isOlderThanOrEquals(ClientVersion.v_1_12_2)) {
            player.isSwimming = false;
        }
        if (player.isFlying) {
            player.isSwimming = false;
        } else {
            if (player.inVehicle) {
                player.isSwimming = false;
            } else if (player.isSwimming) {
                player.isSwimming = player.lastSprinting && player.wasTouchingWater;
            } else {
                player.isSwimming = player.lastSprinting && player.wasEyeInWater && player.wasTouchingWater;
            }
        }
    }


    private void moveTowardsClosestSpace(double xPosition, double zPosition) {
        int blockX = (int) Math.floor(xPosition);
        int blockZ = (int) Math.floor(zPosition);

        if (!this.suffocatesAt(blockX, blockZ)) {
            return;
        }
        double relativeXMovement = xPosition - blockX;
        double relativeZMovement = zPosition - blockZ;
        BlockFace direction = null;
        double lowestValue = Double.MAX_VALUE;
        for (BlockFace direction2 : new BlockFace[]{BlockFace.WEST, BlockFace.EAST, BlockFace.NORTH, BlockFace.SOUTH}) {
            double d6;
            double d7 = direction2 == BlockFace.WEST || direction2 == BlockFace.EAST ? relativeXMovement : relativeZMovement;
            d6 = direction2 == BlockFace.EAST || direction2 == BlockFace.SOUTH ? 1.0 - d7 : d7;
            // d7 and d6 flip the movement direction based on desired movement direction
            boolean doesSuffocate;
            switch (direction2) {
                case EAST:
                    doesSuffocate = this.suffocatesAt(blockX + 1, blockZ);
                    break;
                case WEST:
                    doesSuffocate = this.suffocatesAt(blockX - 1, blockZ);
                    break;
                case NORTH:
                    doesSuffocate = this.suffocatesAt(blockX, blockZ - 1);
                    break;
                default:
                case SOUTH:
                    doesSuffocate = this.suffocatesAt(blockX, blockZ + 1);
                    break;
            }

            if (d6 >= lowestValue || doesSuffocate) continue;
            lowestValue = d6;
            direction = direction2;
        }
        if (direction != null) {
            if (direction == BlockFace.WEST || direction == BlockFace.EAST) {
                player.baseTickSetX(0.1 * (double) direction.getModX());
            } else {
                player.baseTickSetZ(0.1 * (double) direction.getModZ());
            }
        }
    }

    // Entity line 945
    void updateInWaterStateAndDoWaterCurrentPushing() {
        this.updateFluidHeightAndDoFluidPushing(FluidTag.WATER, 0.014);
        player.lastTouchingWater = player.wasTouchingWater;
        player.wasTouchingWater = this.updateFluidHeightAndDoFluidPushing(FluidTag.WATER, 0.014) && !(player.playerVehicle instanceof Boat);
    }

    public boolean updateFluidHeightAndDoFluidPushing(FluidTag tag, double multiplier) {
        if (player.getClientVersion().isNewerThanOrEquals(ClientVersion.v_1_13)) {
            return updateFluidHeightAndDoFluidPushingModern(tag, multiplier);
        }

        return updateFluidHeightAndDoFluidPushingLegacy(tag, multiplier);
    }

    public boolean updateFluidHeightAndDoFluidPushingLegacy(FluidTag tag, double multiplier) {
        SimpleCollisionBox aABB = player.boundingBox.copy().expand(0, -0.4, 0).expand(-0.001);

        int floorX = GrimMathHelper.floor(aABB.minX);
        int ceilX = GrimMathHelper.ceil(aABB.maxX);
        int floorY = GrimMathHelper.floor(aABB.minY);
        int ceilY = GrimMathHelper.ceil(aABB.maxY);
        int floorZ = GrimMathHelper.floor(aABB.minZ);
        int ceilZ = GrimMathHelper.ceil(aABB.maxZ);
        if (CheckIfChunksLoaded.isChunksUnloadedAt(player, floorX, floorY, floorZ, ceilX, ceilY, ceilZ)) {
            return false;
        }

        boolean hasPushed = false;
        Vector vec3 = new Vector();

        for (int x = floorX; x < ceilX; ++x) {
            for (int y = floorY; y < ceilY; ++y) {
                for (int z = floorZ; z < ceilZ; ++z) {
                    double fluidHeight;
                    if (tag == FluidTag.WATER) {
                        fluidHeight = player.compensatedWorld.getWaterFluidLevelAt(x, y, z);
                    } else {
                        fluidHeight = player.compensatedWorld.getLavaFluidLevelAt(x, y, z);
                    }

                    if (fluidHeight == 0)
                        continue;

                    double d0 = (float) (y + 1) - fluidHeight;

                    if (!player.specialFlying && ceilY >= d0) {
                        hasPushed = true;
                        vec3.add(FluidTypeFlowing.getFlow(player, x, y, z));
                    }
                }
            }
        }

        if (vec3.lengthSquared() > 0.0) {
            vec3.normalize();
            vec3.multiply(multiplier);
            player.baseTickAddVector(vec3);
        }

        return hasPushed;
    }

    public boolean updateFluidHeightAndDoFluidPushingModern(FluidTag tag, double multiplier) {
        SimpleCollisionBox aABB = player.boundingBox.copy().expand(-0.001);

        int floorX = GrimMathHelper.floor(aABB.minX);
        int ceilX = GrimMathHelper.ceil(aABB.maxX);
        int floorY = GrimMathHelper.floor(aABB.minY);
        int ceilY = GrimMathHelper.ceil(aABB.maxY);
        int floorZ = GrimMathHelper.floor(aABB.minZ);
        int ceilZ = GrimMathHelper.ceil(aABB.maxZ);
        if (CheckIfChunksLoaded.isChunksUnloadedAt(player, floorX, floorY, floorZ, ceilX, ceilY, ceilZ)) {
            return false;
        }
        double d2 = 0.0;
        boolean hasTouched = false;
        Vector vec3 = new Vector();
        int n7 = 0;

        for (int i = floorX; i < ceilX; ++i) {
            for (int j = floorY; j < ceilY; ++j) {
                for (int k = floorZ; k < ceilZ; ++k) {
                    double d3;

                    double fluidHeight;
                    if (tag == FluidTag.WATER) {
                        fluidHeight = player.compensatedWorld.getWaterFluidLevelAt(i, j, k);
                    } else {
                        fluidHeight = player.compensatedWorld.getLavaFluidLevelAt(i, j, k);
                    }

                    if (fluidHeight == 0 || (d3 = (float) j + fluidHeight) < aABB.minY)
                        continue;

                    hasTouched = true;
                    d2 = Math.max(d3 - aABB.minY, d2);

                    if (!player.specialFlying) {
                        Vector vec32 = FluidTypeFlowing.getFlow(player, i, j, k);
                        if (d2 < 0.4) {
                            vec32 = vec32.multiply(d2);
                        }
                        vec3 = vec3.add(vec32);
                        ++n7;
                    }

                }
            }
        }
        if (vec3.lengthSquared() > 0.0) {
            if (n7 > 0) {
                vec3 = vec3.multiply(1.0 / (double) n7);
            }

            if (player.inVehicle) {
                // This is a boat, normalize it for some reason.
                vec3 = vec3.normalize();
            }

            Vector vec33 = player.clientVelocity.clone();
            vec3 = vec3.multiply(multiplier);
            if (Math.abs(vec33.getX()) < 0.003 && Math.abs(vec33.getZ()) < 0.003 && vec3.length() < 0.0045000000000000005D) {
                vec3 = vec3.normalize().multiply(0.0045000000000000005);
            }

            // If the player is using 1.16+ - 1.15 and below don't have lava pushing
            if (tag != FluidTag.LAVA || player.getClientVersion().isNewerThanOrEquals(ClientVersion.v_1_16)) {
                player.baseTickAddVector(new Vector(vec3.getX(), vec3.getY(), vec3.getZ()));
            }
        }

        if (tag == FluidTag.LAVA) {
            player.slightlyTouchingLava = hasTouched && d2 <= 0.4D;
        }

        return hasTouched;
    }

    private boolean suffocatesAt(int x, int z) {
        SimpleCollisionBox axisAlignedBB = new SimpleCollisionBox(x, player.boundingBox.minY, z, x + 1.0, player.boundingBox.maxY, z + 1.0).expand(-1.0E-7);

        return Collisions.suffocatesAt(player, axisAlignedBB);
    }
}
