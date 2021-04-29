package ac.grim.grimac.checks.movement;

import ac.grim.grimac.GrimPlayer;
import ac.grim.grimac.utils.chunks.ChunkCache;
import ac.grim.grimac.utils.collisions.Collisions;
import ac.grim.grimac.utils.collisions.types.SimpleCollisionBox;
import ac.grim.grimac.utils.enums.Pose;
import ac.grim.grimac.utils.math.Mth;
import ac.grim.grimac.utils.nmsImplementations.BlockProperties;
import ac.grim.grimac.utils.nmsImplementations.CheckIfChunksLoaded;
import ac.grim.grimac.utils.nmsImplementations.FluidTypeFlowing;
import ac.grim.grimac.utils.nmsImplementations.GetBoundingBox;
import net.minecraft.server.v1_16_R3.*;
import org.bukkit.Bukkit;
import org.bukkit.block.data.BlockData;
import org.bukkit.craftbukkit.v1_16_R3.CraftWorld;
import org.bukkit.entity.Boat;
import org.bukkit.util.Vector;

public class PlayerBaseTick {
    GrimPlayer player;

    public PlayerBaseTick(GrimPlayer player) {
        this.player = player;
    }

    public void doBaseTick() {
        // Keep track of basetick stuff
        player.baseTickSet = new Vector();
        player.baseTickAddition = new Vector(0, 0, 0);

        // LocalPlayer:aiStep line 728
        if (player.entityPlayer.isInWater() && player.isSneaking && !player.specialFlying && !player.inVehicle) {
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
        player.crouching = !player.specialFlying && !player.isSwimming && canEnterPose(Pose.CROUCHING) && (player.isSneaking || !player.bukkitPlayer.isSleeping() || !canEnterPose(Pose.STANDING));

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
    }

    protected boolean canEnterPose(Pose pose) {
        return Collisions.isEmpty(player, getBoundingBoxForPose(pose).expand(-1.0E-7D));
    }

    protected SimpleCollisionBox getBoundingBoxForPose(Pose pose) {
        float radius = pose.width / 2.0F;
        return new SimpleCollisionBox(player.lastX - radius, player.lastY, player.lastZ - radius, player.lastX + radius, player.lastY + pose.height, player.lastZ + radius);
    }

    // Entity line 937
    public void updateInWaterStateAndDoFluidPushing() {
        player.fluidHeight.clear();
        updateInWaterStateAndDoWaterCurrentPushing();
        double d = player.entityPlayer.world.getDimensionManager().isNether() ? 0.007 : 0.0023333333333333335;
        this.updateFluidHeightAndDoFluidPushing(TagsFluid.LAVA, d);
    }

    private void updateFluidOnEyes() {
        player.wasEyeInWater = player.isEyeInFluid(TagsFluid.WATER);
        player.fluidOnEyes = null;
        double d0 = player.lastY + GetBoundingBox.getEyeHeight(player.isSneaking, player.bukkitPlayer.isGliding(), player.isSwimming, player.bukkitPlayer.isSleeping(), player.clientVersion) - 0.1111111119389534D;

        if (player.playerVehicle instanceof Boat && !player.boatData.boatUnderwater && player.boundingBox.maxY >= d0 && player.boundingBox.minY <= d0) {
            return;
        }

        BlockData eyeFluid = ChunkCache.getBukkitBlockDataAt((int) Math.floor(player.lastX), (int) Math.floor(d0), (int) Math.floor(player.lastZ));

        // TODO: Support 1.12 with Material.STATIONARY_WATER
        if (eyeFluid.getMaterial() == org.bukkit.Material.WATER) {
            double d1 = (float) Math.floor(d0) + ChunkCache.getWaterFluidLevelAt((int) Math.floor(player.lastX), (int) Math.floor(d0), (int) Math.floor(player.lastZ));
            if (d1 > d0) {
                player.fluidOnEyes = TagsFluid.WATER;
            }
        } else if (eyeFluid.getMaterial() == org.bukkit.Material.LAVA) {
            double d1 = (float) Math.floor(d0) + ChunkCache.getWaterFluidLevelAt((int) Math.floor(player.lastX), (int) Math.floor(d0), (int) Math.floor(player.lastZ));
            if (d1 > d0) {
                player.fluidOnEyes = TagsFluid.LAVA;
            }
        }
    }

    public void updateSwimming() {
        // This doesn't seem like the right place for determining swimming, but it's fine for now
        if (player.isFlying) {
            player.isSwimming = false;
        } else {

            Bukkit.broadcastMessage("Is touching water " + player.wasTouchingWater);
            Bukkit.broadcastMessage("Is eyes in water " + player.wasEyeInWater);
            Bukkit.broadcastMessage("Is sprinting " + player.isPacketSprinting);


            if (player.inVehicle) {
                player.isSwimming = false;
            } else if (player.isSwimming) {
                player.isSwimming = player.lastSprinting && player.wasTouchingWater;
            } else {
                player.isSwimming = player.lastSprinting && player.wasEyeInWater && player.wasTouchingWater;
            }

            Bukkit.broadcastMessage("Is swimming " + player.isSwimming);
        }
    }


    private void moveTowardsClosestSpace(double xPosition, double zPosition) {
        BlockPosition blockPos = new BlockPosition(xPosition, player.lastY, zPosition);

        if (!this.suffocatesAt(blockPos)) {
            return;
        }
        double relativeXMovement = xPosition - blockPos.getX();
        double relativeZMovement = zPosition - blockPos.getZ();
        EnumDirection direction = null;
        double lowestValue = Double.MAX_VALUE;
        for (EnumDirection direction2 : new EnumDirection[]{EnumDirection.WEST, EnumDirection.EAST, EnumDirection.NORTH, EnumDirection.SOUTH}) {
            double d6;
            double d7 = direction2.n().a(relativeXMovement, 0.0, relativeZMovement);
            d6 = direction2.e() == EnumDirection.EnumAxisDirection.POSITIVE ? 1.0 - d7 : d7;
            // d7 and d6 flip the movement direction based on desired movement direction
            if (d6 >= lowestValue || this.suffocatesAt(blockPos.shift(direction2))) continue;
            lowestValue = d6;
            direction = direction2;
        }
        if (direction != null) {
            if (direction.n() == EnumDirection.EnumAxis.X) {
                player.baseTickSetX(0.1 * (double) direction.getAdjacentX());
            } else {
                player.baseTickSetZ(0.1 * (double) direction.getAdjacentZ());
            }
        }
    }

    // Entity line 945
    void updateInWaterStateAndDoWaterCurrentPushing() {
        if (player.playerVehicle instanceof Boat) {
            player.wasTouchingWater = false;
            return;
        }

        player.wasTouchingWater = this.updateFluidHeightAndDoFluidPushing(TagsFluid.WATER, 0.014);
    }

    public boolean updateFluidHeightAndDoFluidPushing(Tag.e<FluidType> tag, double d) {
        SimpleCollisionBox aABB = player.boundingBox.expand(-0.001);
        int n2 = Mth.floor(aABB.minX);
        int n3 = Mth.ceil(aABB.maxX);
        int n4 = Mth.floor(aABB.minY);
        int n5 = Mth.ceil(aABB.maxY);
        int n6 = Mth.floor(aABB.minZ);
        int n = Mth.ceil(aABB.maxZ);
        if (!CheckIfChunksLoaded.hasChunksAt(n2, n4, n6, n3, n5, n)) {
            return false;
        }
        double d2 = 0.0;
        boolean bl2 = false;
        Vec3D vec3 = Vec3D.ORIGIN;
        int n7 = 0;
        BlockPosition.MutableBlockPosition mutableBlockPos = new BlockPosition.MutableBlockPosition();
        WorldServer playerWorld = ((CraftWorld) player.playerWorld).getHandle();
        for (int i = n2; i < n3; ++i) {
            for (int j = n4; j < n5; ++j) {
                for (int k = n6; k < n; ++k) {
                    double d3;
                    mutableBlockPos.d(i, j, k);
                    Fluid fluid = ChunkCache.getBlockDataAt(i, j, k).getFluid();
                    // TODO: This is not async safe!
                    if (!fluid.a(tag) || !((d3 = (float) j + fluid.getHeight(playerWorld, mutableBlockPos)) >= aABB.minY))
                        continue;
                    bl2 = true;
                    d2 = Math.max(d3 - aABB.minY, d2);

                    if (!player.specialFlying) {
                        Vec3D vec32 = FluidTypeFlowing.getFlow(mutableBlockPos, fluid);
                        if (d2 < 0.4) {
                            vec32 = vec32.a(d2);
                        }
                        vec3 = vec3.e(vec32);
                        ++n7;
                    }

                }
            }
        }
        if (vec3.f() > 0.0) {
            if (n7 > 0) {
                vec3 = vec3.a(1.0 / (double) n7);
            }

            if (player.inVehicle) {
                // This is a boat, normalize it for some reason.
                vec3 = vec3.d();
            }

            Vector vec33 = player.clientVelocity.clone();
            vec3 = vec3.a(d);
            if (Math.abs(vec33.getX()) < 0.003 && Math.abs(vec33.getZ()) < 0.003 && vec3.f() < 0.0045000000000000005D) {
                vec3 = vec3.d().a(0.0045000000000000005);
            }
            player.baseTickAddVector(new Vector(vec3.x, vec3.y, vec3.z));
        }
        player.fluidHeight.put(tag, d2);
        return bl2;
    }

    private boolean suffocatesAt(BlockPosition blockPos2) {
        AxisAlignedBB axisAlignedBB = new AxisAlignedBB(blockPos2.getX(), player.boundingBox.minY, blockPos2.getZ(), blockPos2.getX() + 1.0, player.boundingBox.maxY, blockPos2.getZ() + 1.0).grow(-1.0E-7, -1.0E-7, -1.0E-7);
        // It looks like the method it usually calls is gone from the server?
        // So we have to just do the allMatch ourselves.
        // TODO: This is not async safe!
        return !((CraftWorld) player.playerWorld).getHandle().b(player.entityPlayer, axisAlignedBB, (blockState, blockPos) -> blockState.o(player.entityPlayer.getWorld(), blockPos)).allMatch(VoxelShape::isEmpty);
    }
}
