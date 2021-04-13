package ac.grim.grimac.events.anticheat;

import ac.grim.grimac.GrimPlayer;
import ac.grim.grimac.utils.chunks.ChunkCache;
import ac.grim.grimac.utils.math.Mth;
import ac.grim.grimac.utils.nmsImplementations.CheckIfChunksLoaded;
import ac.grim.grimac.utils.nmsImplementations.Collisions;
import ac.grim.grimac.utils.nmsImplementations.FluidTypeFlowing;
import net.minecraft.server.v1_16_R3.*;
import org.bukkit.craftbukkit.v1_16_R3.CraftWorld;
import org.bukkit.util.Vector;

import java.util.Iterator;

public class PlayerBaseTick {
    GrimPlayer player;

    public PlayerBaseTick(GrimPlayer player) {
        this.player = player;
    }

    public void doBaseTick() {
        // LocalPlayer:aiStep line 728
        if (player.entityPlayer.isInWater() && player.isSneaking && !player.isFlying) {
            player.baseTickAddVector(new Vector(0, -0.04, 0));
        }

        // Let shifting and holding space not be a false positive by allowing sneaking to override this
        // TODO: Do we have to apply this to other velocities
        if (player.isFlying) {
            player.clientVelocityJumping = player.clientVelocity.clone().add(new Vector(0, player.entityPlayer.abilities.flySpeed * 3, 0));
        }

        if (player.isFlying && player.isSneaking) {
            player.baseTickAddVector(new Vector(0, player.entityPlayer.abilities.flySpeed * -3, 0));
        }

        updateInWaterStateAndDoFluidPushing();
        updateFluidOnEyes();

        // LocalPlayer:aiStep line 647
        this.moveTowardsClosestSpace(player.lastX - (double) player.entityPlayer.getWidth() * 0.35, player.lastZ + (double) player.entityPlayer.getWidth() * 0.35);
        this.moveTowardsClosestSpace(player.lastX - (double) player.entityPlayer.getWidth() * 0.35, player.lastZ - (double) player.entityPlayer.getWidth() * 0.35);
        this.moveTowardsClosestSpace(player.lastX + (double) player.entityPlayer.getWidth() * 0.35, player.lastZ - (double) player.entityPlayer.getWidth() * 0.35);
        this.moveTowardsClosestSpace(player.lastX + (double) player.entityPlayer.getWidth() * 0.35, player.lastZ + (double) player.entityPlayer.getWidth() * 0.35);

        // TODO: Swimming check
        //updateSwimming();

        // Put stuck speed here so it is on the right tick
        Collisions.handleInsideBlocks(player);

        // Flying players are not affected by cobwebs/sweet berry bushes
        if (player.isFlying) {
            player.stuckSpeedMultiplier = new Vector(1, 1, 1);
        }
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
        double d0 = player.entityPlayer.getHeadY() - 0.1111111119389534D;
        Entity entity = player.entityPlayer.getVehicle();
        if (entity instanceof EntityBoat) {
            EntityBoat entityboat = (EntityBoat) entity;
            if (!entityboat.aI() && entityboat.getBoundingBox().maxY >= d0 && entityboat.getBoundingBox().minY <= d0) {
                return;
            }
        }

        BlockPosition blockposition = new BlockPosition(player.x, d0, player.z);
        Fluid fluid = ChunkCache.getBlockDataAt(player.x, player.y, player.z).getFluid();
        Iterator iterator = TagsFluid.b().iterator();

        while (iterator.hasNext()) {
            Tag tag = (Tag) iterator.next();
            if (fluid.a(tag)) {
                double d1 = (float) blockposition.getY() + fluid.getHeight(player.entityPlayer.getWorld(), blockposition);
                if (d1 > d0) {
                    player.fluidOnEyes = tag;
                }

                return;
            }
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
        if (player.bukkitPlayer.getVehicle() instanceof EntityBoat) {
            player.wasTouchingWater = false;
        } else if (this.updateFluidHeightAndDoFluidPushing(TagsFluid.WATER, 0.014)) {
            // Watersplash effect removed (Entity 981).  Shouldn't affect movement
            player.fallDistance = 0.0f;
            player.wasTouchingWater = true;
            //this.clearFire();
        } else {
            player.wasTouchingWater = false;
        }
    }

    // TODO: Idk if this is right
    public boolean updateFluidHeightAndDoFluidPushing(Tag.e<FluidType> tag, double d) {
        AxisAlignedBB aABB = player.boundingBox.shrink(0.001);
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
                    if (!fluid.a(tag) || !((d3 = (float) j + fluid.getHeight(playerWorld, mutableBlockPos)) >= aABB.minY))
                        continue;
                    bl2 = true;
                    d2 = Math.max(d3 - aABB.minX, d2);

                    if (!player.isFlying) {
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
        return !((CraftWorld) player.playerWorld).getHandle().b(player.entityPlayer, axisAlignedBB, (blockState, blockPos) -> blockState.o(player.entityPlayer.getWorld(), blockPos)).allMatch(VoxelShape::isEmpty);
    }
}
