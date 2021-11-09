package ac.grim.grimac.predictionengine;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.collisions.datatypes.SimpleCollisionBox;
import ac.grim.grimac.utils.enums.EntityType;
import ac.grim.grimac.utils.enums.FluidTag;
import ac.grim.grimac.utils.enums.Pose;
import ac.grim.grimac.utils.math.GrimMath;
import ac.grim.grimac.utils.nmsutil.*;
import io.github.retrooper.packetevents.utils.player.ClientVersion;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
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
        return new SimpleCollisionBox(x - radius, y, z - radius, x + radius, y + pose.height, z + radius, false);
    }

    public void doBaseTick() {
        // Keep track of basetick stuff
        player.baseTickAddition = new Vector();
        player.baseTickWaterPushing = new Vector();

        if (player.specialFlying && player.isSneaking && !player.inVehicle) {
            player.baseTickAddVector(new Vector(0, player.flySpeed * -3, 0));
        }

        updateInWaterStateAndDoFluidPushing();
        updateFluidOnEyes();
        updateSwimming();

        // If in lava, fall distance is multiplied by 0.5
        if (player.wasTouchingLava)
            player.fallDistance *= 0.5;

        // You cannot crouch while flying, only shift - could be specific to 1.14?
        if (player.wasTouchingWater && player.isSneaking && !player.specialFlying && !player.inVehicle) {
            player.baseTickAddVector(new Vector(0, -0.04, 0));
        }

        // LocalPlayer:aiStep determining crouching
        // Tick order is entityBaseTick and then the aiStep stuff
        // This code is in the wrong place, I'll fix it later

        player.isCrouching = player.getClientVersion().isNewerThanOrEquals(ClientVersion.v_1_14) ? !player.wasFlying && !player.isSwimming && canEnterPose(player, Pose.CROUCHING, player.lastX, player.lastY, player.lastZ)
                && ((player.isCrouching || player.getClientVersion().isNewerThan(ClientVersion.v_1_14_4) ? player.wasSneaking : player.isSneaking)
                || player.isInBed || !canEnterPose(player, Pose.STANDING, player.lastX, player.lastY, player.lastZ))
                : player.isSneaking; // Sneaking on 1.7-1.13 is just the status the player sends us.  Nothing complicated.
        player.isSlowMovement = player.isCrouching || (player.getClientVersion().isNewerThanOrEquals(ClientVersion.v_1_14) &&
                // If the player is in the swimming pose
                // Or if the player is not gliding, and the player's pose is fall flying
                // and the player is not touching water (yes, this also can override the gliding slowness)
                (player.pose == Pose.SWIMMING || (!player.isGliding && player.pose == Pose.FALL_FLYING)) && !player.wasTouchingWater);


        // Players in boats don't care about being in blocks
        if (!player.inVehicle) {
            this.moveTowardsClosestSpace(player.lastX - (player.boundingBox.maxX - player.boundingBox.minX) * 0.35, player.lastZ + (player.boundingBox.maxZ - player.boundingBox.minZ) * 0.35);
            this.moveTowardsClosestSpace(player.lastX - (player.boundingBox.maxX - player.boundingBox.minX) * 0.35, player.lastZ - (player.boundingBox.maxZ - player.boundingBox.minZ) * 0.35);
            this.moveTowardsClosestSpace(player.lastX + (player.boundingBox.maxX - player.boundingBox.minX) * 0.35, player.lastZ - (player.boundingBox.maxZ - player.boundingBox.minZ) * 0.35);
            this.moveTowardsClosestSpace(player.lastX + (player.boundingBox.maxX - player.boundingBox.minX) * 0.35, player.lastZ + (player.boundingBox.maxZ - player.boundingBox.minZ) * 0.35);
        }

        float f = BlockProperties.getBlockSpeedFactor(player);
        player.blockSpeedMultiplier = new Vector(f, 1.0, f);

        if (player.getClientVersion().isOlderThan(ClientVersion.v_1_14)) {
            updatePlayerSize();
        }
    }

    // 1.16 eye in water is a tick behind
    // 1.15 eye in water is the most recent result
    private void updateFluidOnEyes() {
        player.wasEyeInWater = player.isEyeInFluid(FluidTag.WATER);
        player.fluidOnEyes = null;

        double d0 = player.lastY + player.getEyeHeight() - 0.1111111119389534D;

        if (player.playerVehicle != null && player.playerVehicle.type == EntityType.BOAT && !player.vehicleData.boatUnderwater && player.boundingBox.maxY >= d0 && player.boundingBox.minY <= d0) {
            return;
        }

        double d1 = (float) Math.floor(d0) + player.compensatedWorld.getWaterFluidLevelAt(player.lastX, d0, player.lastZ);
        if (d1 > d0) {
            player.fluidOnEyes = FluidTag.WATER;
            if (player.getClientVersion().isOlderThanOrEquals(ClientVersion.v_1_15_2))
                player.wasEyeInWater = true;
            return;
        }

        if (player.getClientVersion().isOlderThanOrEquals(ClientVersion.v_1_15_2))
            player.wasEyeInWater = false;

        d1 = (float) Math.floor(d0) + player.compensatedWorld.getWaterFluidLevelAt(player.lastX, d0, player.lastZ);
        if (d1 > d0) {
            player.fluidOnEyes = FluidTag.LAVA;
        }
    }

    public void updateInWaterStateAndDoFluidPushing() {
        updateInWaterStateAndDoWaterCurrentPushing();
        double d = player.playerWorld.getEnvironment() == World.Environment.NETHER ? 0.007 : 0.0023333333333333335;
        // 1.15 and below clients use block collisions to check for being in lava
        if (player.getClientVersion().isNewerThanOrEquals(ClientVersion.v_1_16))
            player.wasTouchingLava = this.updateFluidHeightAndDoFluidPushing(FluidTag.LAVA, d);
            // 1.13 and below clients use this stupid method to check if in lava
        else if (player.getClientVersion().isOlderThan(ClientVersion.v_1_14)) {
            SimpleCollisionBox playerBox = player.boundingBox.copy().expand(-0.1F, -0.4F, -0.1F);
            player.wasTouchingLava = player.compensatedWorld.containsLava(playerBox);
        }
    }

    // 1.14
    public void updatePlayerPose() {
        if (canEnterPose(player, Pose.SWIMMING, player.x, player.y, player.z)) {
            Pose pose;
            if (player.isGliding) {
                pose = Pose.FALL_FLYING;
            } else if (player.isInBed) {
                pose = Pose.SLEEPING;
            } else if (player.isSwimming) {
                pose = Pose.SWIMMING;
            } else if (player.isRiptidePose) {
                pose = Pose.SPIN_ATTACK;
            } else if (player.getClientVersion().isNewerThanOrEquals(ClientVersion.v_1_9) && player.getClientVersion().isOlderThan(ClientVersion.v_1_14) && player.isSneaking) {
                pose = Pose.NINE_CROUCHING;
            } else if (player.getClientVersion().isNewerThanOrEquals(ClientVersion.v_1_14) && player.isCrouching && !player.specialFlying) {
                pose = Pose.CROUCHING;
            } else {
                pose = Pose.STANDING;
            }

            // I'm not too sure about this code, but it appears like this is only a 1.14+ feature
            // In my testing this seems good but still don't have full confidence for versions like 1.13
            if (player.getClientVersion().isNewerThanOrEquals(ClientVersion.v_1_14) &&
                    !player.inVehicle && !canEnterPose(player, pose, player.x, player.y, player.z)) {
                if (canEnterPose(player, Pose.CROUCHING, player.x, player.y, player.z)) {
                    pose = Pose.CROUCHING;
                } else {
                    pose = Pose.SWIMMING;
                }
            }

            player.pose = pose;
            player.boundingBox = GetBoundingBox.getCollisionBoxForPlayer(player, player.x, player.y, player.z);
        }
    }

    // 1.13 and below
    public void updatePlayerSize() {
        Pose pose;
        if (player.isGliding) {
            pose = Pose.FALL_FLYING;
        } else if (player.isInBed) {
            pose = Pose.SLEEPING;
        } else if (!player.isSwimming && !player.isRiptidePose) {
            if (player.isSneaking && player.getClientVersion().isNewerThanOrEquals(ClientVersion.v_1_9)) {
                pose = Pose.NINE_CROUCHING;
            } else {
                pose = Pose.STANDING;
            }
        } else {
            pose = Pose.SWIMMING;
        }

        // 1.13 actually compares widths and heights etc. but this should also work.
        if (pose != player.pose) {
            Pose oldPose = player.pose;
            player.pose = pose;

            SimpleCollisionBox box = GetBoundingBox.getCollisionBoxForPlayer(player, player.lastX, player.lastY, player.lastZ);
            boolean collides = !Collisions.isEmpty(player, box);

            if (collides) {
                // Revert, the player does not have room to enter this new pose
                player.pose = oldPose;
                return;
            }
        }

        player.boundingBox = GetBoundingBox.getCollisionBoxForPlayer(player, player.lastX, player.lastY, player.lastZ);
    }

    public void updateSwimming() {
        // This doesn't seem like the right place for determining swimming, but it's fine for now
        if (player.getClientVersion().isOlderThanOrEquals(ClientVersion.v_1_12_2)) {
            player.isSwimming = false;
        } else if (player.isFlying) {
            player.isSwimming = false;
        } else {
            if (player.inVehicle) {
                player.isSwimming = false;
            } else if (player.isSwimming) {
                player.isSwimming = player.lastSprinting && player.wasTouchingWater;
            } else {
                // Requirement added in 1.17 to fix player glitching between two swimming states
                // while swimming with feet in air and eyes in water
                boolean feetInWater = player.getClientVersion().isOlderThan(ClientVersion.v_1_17)
                        || player.compensatedWorld.getWaterFluidLevelAt(player.lastX, player.lastY, player.lastZ) > 0;
                player.isSwimming = player.lastSprinting && player.wasEyeInWater && player.wasTouchingWater && feetInWater;
            }
        }
    }


    private void moveTowardsClosestSpace(double xPosition, double zPosition) {
        if (player.getClientVersion().isNewerThanOrEquals(ClientVersion.v_1_14)) {
            moveTowardsClosestSpaceModern(xPosition, zPosition);
        } else {
            moveTowardsClosestSpaceLegacy(xPosition, zPosition);
        }
    }

    // Mojang is incompetent and this will push the player out a lot when using elytras
    private void moveTowardsClosestSpaceLegacy(double x, double z) {
        int floorX = GrimMath.floor(x);
        int floorZ = GrimMath.floor(z);
        int floorY = GrimMath.floor(player.lastY + 0.5);

        double d0 = x - floorX;
        double d1 = z - floorZ;

        boolean suffocates;

        if (player.isSwimming) {
            SimpleCollisionBox blockPos = new SimpleCollisionBox(floorX, floorY, floorZ, floorX + 1.0, floorY + 1, floorZ + 1.0, false).expand(-1.0E-7);
            suffocates = Collisions.suffocatesAt(player, blockPos);
        } else {
            suffocates = !clearAbove(floorX, floorY, floorZ);
        }

        if (suffocates) {
            int i = -1;
            double d2 = 9999.0D;
            if (clearAbove(floorX - 1, floorY, floorZ) && d0 < d2) {
                d2 = d0;
                i = 0;
            }

            if (clearAbove(floorX + 1, floorY, floorZ) && 1.0D - d0 < d2) {
                d2 = 1.0D - d0;
                i = 1;
            }

            if (clearAbove(floorX, floorY, floorZ - 1) && d1 < d2) {
                d2 = d1;
                i = 4;
            }

            if (clearAbove(floorX, floorY, floorZ + 1) && 1.0D - d1 < d2) {
                i = 5;
            }

            if (i == 0) {
                player.clientVelocity.setX(-0.1F);
            }

            if (i == 1) {
                player.clientVelocity.setX(0.1F);
            }

            if (i == 4) {
                player.clientVelocity.setZ(-0.1F);
            }

            if (i == 5) {
                player.clientVelocity.setZ(0.1F);
            }
        }
    }

    // 1.14+
    private void moveTowardsClosestSpaceModern(double xPosition, double zPosition) {
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
                player.clientVelocity.setX(0.1 * (double) direction.getModX());
            } else {
                player.clientVelocity.setZ(0.1 * (double) direction.getModZ());
            }
        }
    }

    public void updateInWaterStateAndDoWaterCurrentPushing() {
        player.wasTouchingWater = this.updateFluidHeightAndDoFluidPushing(FluidTag.WATER, 0.014) && !(player.playerVehicle != null && player.playerVehicle.type == EntityType.BOAT);
        if (player.wasTouchingWater)
            player.fallDistance = 0;
    }

    public boolean updateFluidHeightAndDoFluidPushing(FluidTag tag, double multiplier) {
        if (player.getClientVersion().isNewerThanOrEquals(ClientVersion.v_1_13)) {
            return updateFluidHeightAndDoFluidPushingModern(tag, multiplier);
        }

        return updateFluidHeightAndDoFluidPushingLegacy(tag, multiplier);
    }

    public boolean updateFluidHeightAndDoFluidPushingLegacy(FluidTag tag, double multiplier) {
        SimpleCollisionBox aABB = player.boundingBox.copy().expand(0, -0.4, 0).expand(-0.001);

        int floorX = GrimMath.floor(aABB.minX);
        int ceilX = GrimMath.ceil(aABB.maxX);
        int floorY = GrimMath.floor(aABB.minY);
        int ceilY = GrimMath.ceil(aABB.maxY);
        int floorZ = GrimMath.floor(aABB.minZ);
        int ceilZ = GrimMath.ceil(aABB.maxZ);
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

        // all clients using legacy fluid pushing are not pushed by lava
        if (tag == FluidTag.WATER && vec3.lengthSquared() > 0.0) {
            vec3.normalize();
            vec3.multiply(multiplier);
            player.baseTickAddWaterPushing(vec3);
            player.baseTickAddVector(vec3);
        }

        return hasPushed;
    }

    public boolean updateFluidHeightAndDoFluidPushingModern(FluidTag tag, double multiplier) {
        SimpleCollisionBox aABB = player.boundingBox.copy().expand(-0.001);

        int floorX = GrimMath.floor(aABB.minX);
        int ceilX = GrimMath.ceil(aABB.maxX);
        int floorY = GrimMath.floor(aABB.minY);
        int ceilY = GrimMath.ceil(aABB.maxY);
        int floorZ = GrimMath.floor(aABB.minZ);
        int ceilZ = GrimMath.ceil(aABB.maxZ);
        if (CheckIfChunksLoaded.isChunksUnloadedAt(player, floorX, floorY, floorZ, ceilX, ceilY, ceilZ)) {
            return false;
        }
        double d2 = 0.0;
        boolean hasTouched = false;
        Vector vec3 = new Vector();
        int n7 = 0;

        for (int x = floorX; x < ceilX; ++x) {
            for (int y = floorY; y < ceilY; ++y) {
                for (int z = floorZ; z < ceilZ; ++z) {
                    double fluidHeightToWorld;

                    double fluidHeight;
                    if (tag == FluidTag.WATER) {
                        fluidHeight = player.compensatedWorld.getWaterFluidLevelAt(x, y, z);
                    } else {
                        fluidHeight = player.compensatedWorld.getLavaFluidLevelAt(x, y, z);
                    }

                    if (player.getClientVersion().isOlderThan(ClientVersion.v_1_14))
                        fluidHeight = Math.min(fluidHeight, 8 / 9D);

                    if (fluidHeight == 0 || (fluidHeightToWorld = y + fluidHeight) < aABB.minY)
                        continue;

                    hasTouched = true;
                    d2 = Math.max(fluidHeightToWorld - aABB.minY, d2);

                    if (!player.specialFlying) {
                        Vector vec32 = FluidTypeFlowing.getFlow(player, x, y, z);
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
                vec3 = vec3.multiply(1.0 / n7);
            }

            if (player.inVehicle) {
                // This is a riding entity, normalize it for some reason.
                vec3 = vec3.normalize();
            }

            // If the player is using 1.16+ - 1.15 and below don't have lava pushing
            if (tag != FluidTag.LAVA || player.getClientVersion().isNewerThanOrEquals(ClientVersion.v_1_16)) {
                // Store the vector before handling 0.003, so knockback can use it
                player.baseTickAddWaterPushing(vec3);

                vec3 = vec3.multiply(multiplier);
                if (Math.abs(player.clientVelocity.getX()) < 0.003 && Math.abs(player.clientVelocity.getZ()) < 0.003 && vec3.length() < 0.0045000000000000005D) {
                    vec3 = vec3.normalize().multiply(0.0045000000000000005);
                }

                player.baseTickAddVector(vec3);
            }
        }

        if (tag == FluidTag.LAVA) {
            player.slightlyTouchingLava = hasTouched && d2 <= 0.4D;
        }

        if (tag == FluidTag.WATER) {
            player.slightlyTouchingWater = hasTouched && d2 <= 0.4D;
        }

        return hasTouched;
    }

    private boolean suffocatesAt(int x, int z) {
        SimpleCollisionBox axisAlignedBB = new SimpleCollisionBox(x, player.boundingBox.minY, z, x + 1.0, player.boundingBox.maxY, z + 1.0, false).expand(-1.0E-7);
        return Collisions.suffocatesAt(player, axisAlignedBB);
    }

    private boolean clearAbove(int x, int y, int z) {
        return !Collisions.doesBlockSuffocate(player, x, y, z) && !Collisions.doesBlockSuffocate(player, x, y + 1, z);
    }
}
