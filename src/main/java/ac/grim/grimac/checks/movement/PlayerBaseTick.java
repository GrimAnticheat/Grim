package ac.grim.grimac.checks.movement;

import ac.grim.grimac.GrimPlayer;
import ac.grim.grimac.utils.chunks.ChunkCache;
import ac.grim.grimac.utils.math.Mth;
import ac.grim.grimac.utils.nmsImplementations.BlockProperties;
import ac.grim.grimac.utils.nmsImplementations.CheckIfChunksLoaded;
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

        // LocalPlayer:aiStep line 647
        this.moveTowardsClosestSpace(player.lastX - player.boundingBox.b() * 0.35, player.lastZ + player.boundingBox.d() * 0.35);
        this.moveTowardsClosestSpace(player.lastX - player.boundingBox.b() * 0.35, player.lastZ - player.boundingBox.d() * 0.35);
        this.moveTowardsClosestSpace(player.lastX + player.boundingBox.b() * 0.35, player.lastZ - player.boundingBox.d() * 0.35);
        this.moveTowardsClosestSpace(player.lastX + player.boundingBox.b() * 0.35, player.lastZ + player.boundingBox.d() * 0.35);

        float f = BlockProperties.getBlockSpeedFactor(player);
        player.blockSpeedMultiplier = new Vector(f, 1.0, f);
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

        // Probably not async safe
        if (!player.boatData.boatUnderwater && player.boundingBox.maxY >= d0 && player.boundingBox.minY <= d0) {
            return;
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
        // Watersplash effect removed (Entity 981).  Shouldn't affect movement
        //player.fallDistance = 0.0f;
        //this.clearFire();
        if (player.playerVehicle instanceof EntityBoat) {
            player.wasTouchingWater = false;
        } else player.wasTouchingWater = this.updateFluidHeightAndDoFluidPushing(TagsFluid.WATER, 0.014);
    }

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
