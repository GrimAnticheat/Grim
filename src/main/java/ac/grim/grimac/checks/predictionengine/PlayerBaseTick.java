package ac.grim.grimac.checks.predictionengine;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.collisions.Collisions;
import ac.grim.grimac.utils.collisions.types.SimpleCollisionBox;
import ac.grim.grimac.utils.enums.FluidTag;
import ac.grim.grimac.utils.enums.Pose;
import ac.grim.grimac.utils.math.Mth;
import ac.grim.grimac.utils.nmsImplementations.BlockProperties;
import ac.grim.grimac.utils.nmsImplementations.CheckIfChunksLoaded;
import ac.grim.grimac.utils.nmsImplementations.FluidTypeFlowing;
import ac.grim.grimac.utils.nmsImplementations.GetBoundingBox;
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
            if (player.isFallFlying) {
                pose = Pose.FALL_FLYING;
            } else if (player.bukkitPlayer.isSleeping()) {
                pose = Pose.SLEEPING;
            } else if (player.isSwimming) {
                pose = Pose.SWIMMING;
            } else if (player.bukkitPlayer.isRiptiding()) {
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
        player.fluidHeight.clear();
        updateInWaterStateAndDoWaterCurrentPushing();
        double d = player.playerWorld.getEnvironment() == World.Environment.NETHER ? 0.007 : 0.0023333333333333335;
        this.updateFluidHeightAndDoFluidPushing(FluidTag.LAVA, d);
    }

    private void updateFluidOnEyes() {
        player.wasEyeInWater = player.isEyeInFluid(FluidTag.WATER);
        player.fluidOnEyes = null;
        double d0 = player.lastY + GetBoundingBox.getEyeHeight(player.isCrouching, player.bukkitPlayer.isGliding(), player.isSwimming, player.bukkitPlayer.isSleeping(), player.clientVersion) - 0.1111111119389534D;

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
        player.wasTouchingWater = this.updateFluidHeightAndDoFluidPushing(FluidTag.WATER, 0.014) && !(player.playerVehicle instanceof Boat);
    }

    public boolean updateFluidHeightAndDoFluidPushing(FluidTag tag, double d) {
        SimpleCollisionBox aABB = player.boundingBox.copy().expand(-0.001);
        int n2 = Mth.floor(aABB.minX);
        int n3 = Mth.ceil(aABB.maxX);
        int n4 = Mth.floor(aABB.minY);
        int n5 = Mth.ceil(aABB.maxY);
        int n6 = Mth.floor(aABB.minZ);
        int n = Mth.ceil(aABB.maxZ);
        if (CheckIfChunksLoaded.isChunksUnloadedAt(player, n2, n4, n6, n3, n5, n)) {
            return false;
        }
        double d2 = 0.0;
        boolean bl2 = false;
        Vector vec3 = new Vector();
        int n7 = 0;

        for (int i = n2; i < n3; ++i) {
            for (int j = n4; j < n5; ++j) {
                for (int k = n6; k < n; ++k) {
                    double d3;

                    double fluidHeight;
                    if (tag == FluidTag.WATER) {
                        fluidHeight = player.compensatedWorld.getWaterFluidLevelAt(i, j, k);
                    } else {
                        fluidHeight = player.compensatedWorld.getLavaFluidLevelAt(i, j, k);
                    }

                    if (fluidHeight == 0 || (d3 = (float) j + fluidHeight) < aABB.minY)
                        continue;

                    bl2 = true;
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
        if (vec3.lengthSquared() > 0.0) { // distance
            if (n7 > 0) {
                vec3 = vec3.multiply(1.0 / (double) n7); // multiply
            }

            if (player.inVehicle) {
                // This is a boat, normalize it for some reason.
                vec3 = vec3.normalize(); // normalize
            }

            Vector vec33 = player.clientVelocity.clone();
            vec3 = vec3.multiply(d); // multiply
            if (Math.abs(vec33.getX()) < 0.003 && Math.abs(vec33.getZ()) < 0.003 && vec3.length() < 0.0045000000000000005D) {
                vec3 = vec3.normalize().multiply(0.0045000000000000005); // normalize then multiply
            }

            // If the player is using 1.16+ - 1.15 and below don't have lava pushing
            if (tag != FluidTag.LAVA || player.clientVersion > 700) {
                player.baseTickAddVector(new Vector(vec3.getX(), vec3.getY(), vec3.getZ()));
            }
        }
        player.fluidHeight.put(tag, d2);
        return bl2;
    }

    private boolean suffocatesAt(int x, int z) {
        SimpleCollisionBox axisAlignedBB = new SimpleCollisionBox(x, player.boundingBox.minY, z, x + 1.0, player.boundingBox.maxY, z + 1.0).expand(-1.0E-7);

        return Collisions.suffocatesAt(player, axisAlignedBB);
    }
}
