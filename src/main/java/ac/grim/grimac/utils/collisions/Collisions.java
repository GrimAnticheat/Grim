package ac.grim.grimac.utils.collisions;

import ac.grim.grimac.GrimPlayer;
import ac.grim.grimac.utils.chunks.ChunkCache;
import ac.grim.grimac.utils.collisions.types.SimpleCollisionBox;
import ac.grim.grimac.utils.enums.MoverType;
import ac.grim.grimac.utils.nmsImplementations.BlockData;
import ac.grim.grimac.utils.nmsImplementations.CheckIfChunksLoaded;
import ac.grim.grimac.utils.nmsImplementations.GetBoundingBox;
import com.google.common.collect.Lists;
import net.minecraft.server.v1_16_R3.*;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.util.Vector;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class Collisions {
    public static final double maxUpStep = 0.6f;
    public static final BlockStateBoolean DRAG_DOWN = BlockProperties.e;

    // Entity line 686
    // This MUST return a new vector!!!
    // If it does not the predicted velocity will be overridden
    public static Vector collide(GrimPlayer grimPlayer, double xWithCollision, double yWithCollision, double zWithCollision) {
        SimpleCollisionBox currentPosBB = GetBoundingBox.getPlayerBoundingBox(grimPlayer.lastX, grimPlayer.lastY, grimPlayer.lastZ, grimPlayer.wasSneaking, grimPlayer.bukkitPlayer.isGliding(), grimPlayer.isSwimming, grimPlayer.bukkitPlayer.isSleeping(), grimPlayer.clientVersion);

        List<SimpleCollisionBox> desiredMovementCollisionBoxes = getCollisionBoxes(grimPlayer, currentPosBB.offset(xWithCollision, yWithCollision, zWithCollision));
        SimpleCollisionBox setBB = currentPosBB;

        double clonedX = xWithCollision;
        double clonedY = yWithCollision;
        double clonedZ = zWithCollision;

        // First, collisions are ran without any step height, in y -> x -> z order
        // Interestingly, MC-Market forks love charging hundreds for a slight change in this
        // In 1.7/1.8 cannoning jars, if Z > X, order is Y -> Z -> X, or Z < X, Y -> X -> Z
        // Mojang implemented the if Z > X thing in 1.14+
        if (yWithCollision != 0.0D) {
            for (SimpleCollisionBox bb : desiredMovementCollisionBoxes) {
                yWithCollision = setBB.collideY(bb, yWithCollision);
            }

            setBB = setBB.offset(0.0D, yWithCollision, 0.0D);
        }

        if (Math.abs(zWithCollision) > Math.abs(xWithCollision) && grimPlayer.clientVersion >= 477) {
            if (zWithCollision != 0.0D) {
                for (SimpleCollisionBox bb : desiredMovementCollisionBoxes) {
                    zWithCollision = setBB.collideZ(bb, zWithCollision);
                }

                if (zWithCollision != 0) {
                    setBB = setBB.offset(0.0D, 0.0D, zWithCollision);
                }
            }

            if (xWithCollision != 0.0D) {
                for (SimpleCollisionBox bb : desiredMovementCollisionBoxes) {
                    xWithCollision = setBB.collideX(bb, xWithCollision);
                }

                if (xWithCollision != 0) {
                    setBB = setBB.offset(xWithCollision, 0.0D, 0.0D);
                }
            }
        } else {
            if (xWithCollision != 0.0D) {
                for (SimpleCollisionBox bb : desiredMovementCollisionBoxes) {
                    xWithCollision = setBB.collideX(bb, xWithCollision);
                }

                if (xWithCollision != 0) {
                    setBB = setBB.offset(xWithCollision, 0.0D, 0.0D);
                }
            }

            if (zWithCollision != 0.0D) {
                for (SimpleCollisionBox bb : desiredMovementCollisionBoxes) {
                    zWithCollision = setBB.collideZ(bb, zWithCollision);
                }

                if (zWithCollision != 0) {
                    setBB = setBB.offset(0.0D, 0.0D, zWithCollision);
                }
            }
        }


        boolean movingIntoGround = grimPlayer.lastOnGround || clonedY != yWithCollision && clonedY < 0.0D;

        // If the player has x or z collision, is going in the downwards direction in the last or this tick, and can step up
        // If not, just return the collisions without stepping up that we calculated earlier
        if (grimPlayer.getMaxUpStep() > 0.0F && movingIntoGround && (clonedX != xWithCollision || clonedZ != zWithCollision)) {
            double stepUpHeight = grimPlayer.getMaxUpStep();
            // Undo the offsets done above, but keep the result in justAfterCollisionBB
            SimpleCollisionBox justAfterCollisionBB = setBB;
            setBB = currentPosBB;


            // Get a list of bounding boxes from the player's current bounding box to the wanted coordinates
            List<SimpleCollisionBox> stepUpCollisionBoxes = getCollisionBoxes(grimPlayer, setBB.expandToCoordinate(clonedX, stepUpHeight, clonedZ));

            // Adds a coordinate to the bounding box, extending it if the point lies outside the current ranges. - mcp
            // Note that this will include bounding boxes that we don't need, but the next code can handle it
            SimpleCollisionBox expandedToCoordinateBB = setBB.expandToCoordinate(clonedX, 0.0D, clonedZ);
            double stepMaxClone = stepUpHeight;
            // See how far upwards we go in the Y axis with coordinate expanded collision
            for (SimpleCollisionBox bb : desiredMovementCollisionBoxes) {
                stepMaxClone = expandedToCoordinateBB.collideY(bb, stepMaxClone);
            }


            // TODO: We could probably return normal collision if stepMaxClone == 0 - as we aren't stepping on anything
            // Check some 1.8 jar for it - TacoSpigot would be the best bet for any optimizations here
            // I do need to debug that though. Not sure.
            SimpleCollisionBox yCollisionStepUpBB = setBB;


            yCollisionStepUpBB = yCollisionStepUpBB.offset(0.0D, stepMaxClone, 0.0D);

            double clonedClonedX;
            double clonedClonedZ;
            if (Math.abs(zWithCollision) > Math.abs(xWithCollision) && grimPlayer.clientVersion >= 477) {
                // Calculate Z offset
                clonedClonedZ = clonedZ;
                for (SimpleCollisionBox bb : stepUpCollisionBoxes) {
                    clonedClonedZ = yCollisionStepUpBB.collideZ(bb, clonedClonedZ);
                }
                yCollisionStepUpBB = yCollisionStepUpBB.offset(0.0D, 0.0D, clonedClonedZ);
                // Calculate X offset
                clonedClonedX = clonedX;
                for (SimpleCollisionBox bb : stepUpCollisionBoxes) {
                    clonedClonedX = yCollisionStepUpBB.collideX(bb, clonedClonedX);
                }
                yCollisionStepUpBB = yCollisionStepUpBB.offset(clonedClonedX, 0.0D, 0.0D);
            } else {
                // Calculate X offset
                clonedClonedX = clonedX;
                for (SimpleCollisionBox bb : stepUpCollisionBoxes) {
                    clonedClonedX = yCollisionStepUpBB.collideX(bb, clonedClonedX);
                }
                yCollisionStepUpBB = yCollisionStepUpBB.offset(clonedClonedX, 0.0D, 0.0D);

                // Calculate Z offset
                clonedClonedZ = clonedZ;
                for (SimpleCollisionBox bb : stepUpCollisionBoxes) {
                    clonedClonedZ = yCollisionStepUpBB.collideZ(bb, clonedClonedZ);
                }
                yCollisionStepUpBB = yCollisionStepUpBB.offset(0.0D, 0.0D, clonedClonedZ);
            }

            // Then calculate collisions with the step up height added to the Y axis
            SimpleCollisionBox alwaysStepUpBB = setBB;
            // Calculate y offset
            double stepUpHeightCloned = stepUpHeight;
            for (SimpleCollisionBox bb : stepUpCollisionBoxes) {
                stepUpHeightCloned = alwaysStepUpBB.collideY(bb, stepUpHeightCloned);
            }
            alwaysStepUpBB = alwaysStepUpBB.offset(0.0D, stepUpHeightCloned, 0.0D);

            double zWithCollisionClonedOnceAgain;
            double xWithCollisionClonedOnceAgain;
            if (Math.abs(zWithCollision) > Math.abs(xWithCollision) && grimPlayer.clientVersion >= 477) {
                // Calculate Z offset
                zWithCollisionClonedOnceAgain = clonedZ;
                for (SimpleCollisionBox bb : stepUpCollisionBoxes) {
                    zWithCollisionClonedOnceAgain = alwaysStepUpBB.collideZ(bb, zWithCollisionClonedOnceAgain);
                }
                alwaysStepUpBB = alwaysStepUpBB.offset(0.0D, 0.0D, zWithCollisionClonedOnceAgain);
                // Calculate X offset
                xWithCollisionClonedOnceAgain = clonedX;
                for (SimpleCollisionBox bb : stepUpCollisionBoxes) {
                    xWithCollisionClonedOnceAgain = alwaysStepUpBB.collideX(bb, xWithCollisionClonedOnceAgain);
                }
                alwaysStepUpBB = alwaysStepUpBB.offset(xWithCollisionClonedOnceAgain, 0.0D, 0.0D);
            } else {
                // Calculate X offset
                xWithCollisionClonedOnceAgain = clonedX;
                for (SimpleCollisionBox bb : stepUpCollisionBoxes) {
                    xWithCollisionClonedOnceAgain = alwaysStepUpBB.collideX(bb, xWithCollisionClonedOnceAgain);
                }
                alwaysStepUpBB = alwaysStepUpBB.offset(xWithCollisionClonedOnceAgain, 0.0D, 0.0D);
                // Calculate Z offset
                zWithCollisionClonedOnceAgain = clonedZ;
                for (SimpleCollisionBox bb : stepUpCollisionBoxes) {
                    zWithCollisionClonedOnceAgain = alwaysStepUpBB.collideZ(bb, zWithCollisionClonedOnceAgain);
                }
                alwaysStepUpBB = alwaysStepUpBB.offset(0.0D, 0.0D, zWithCollisionClonedOnceAgain);
            }


            double d23 = clonedClonedX * clonedClonedX + clonedClonedZ * clonedClonedZ;
            double d9 = xWithCollisionClonedOnceAgain * xWithCollisionClonedOnceAgain + zWithCollisionClonedOnceAgain * zWithCollisionClonedOnceAgain;

            double x;
            double y;
            double z;
            if (d23 > d9) {
                x = clonedClonedX;
                y = -stepMaxClone;
                z = clonedClonedZ;
                setBB = yCollisionStepUpBB;
            } else {
                x = xWithCollisionClonedOnceAgain;
                y = -stepUpHeightCloned;
                z = zWithCollisionClonedOnceAgain;
                setBB = alwaysStepUpBB;
            }

            for (SimpleCollisionBox bb : stepUpCollisionBoxes) {
                y = setBB.collideY(bb, y);
            }

            setBB = setBB.offset(0.0D, y, 0.0D);

            if (xWithCollision * xWithCollision + zWithCollision * zWithCollision >= x * x + z * z) {
                setBB = justAfterCollisionBB;
            }
        }

        // Convert bounding box movement back into a vector
        return new Vector(setBB.minX - currentPosBB.minX, setBB.minY - currentPosBB.minY, setBB.minZ - currentPosBB.minZ);
    }

    private static int a(double var0, double var2, double var4) {
        return var0 > 0.0D ? MathHelper.floor(var4 + var0) + 1 : MathHelper.floor(var2 + var0) - 1;
    }

    // MCP mappings PlayerEntity 959
    // Mojang mappings 911
    public static Vector maybeBackOffFromEdge(Vector vec3, MoverType moverType, GrimPlayer grimPlayer) {
        //Player bukkitPlayer = grimPlayer.bukkitPlayer;

        /*if (!grimPlayer.specialFlying && (moverType == MoverType.SELF || moverType == MoverType.PLAYER) && grimPlayer.isSneaking && isAboveGround(grimPlayer)) {
            double d = vec3.getX();
            double d2 = vec3.getZ();
            while (d != 0.0 && noCollision(grimPlayer.entityPlayer, grimPlayer.boundingBox.d(d, -maxUpStep, 0.0))) {
                if (d < 0.05 && d >= -0.05) {
                    d = 0.0;
                    continue;
                }
                if (d > 0.0) {
                    d -= 0.05;
                    continue;
                }
                d += 0.05;
            }
            while (d2 != 0.0 && noCollision(grimPlayer.entityPlayer, grimPlayer.boundingBox.d(0.0, -maxUpStep, d2))) {
                if (d2 < 0.05 && d2 >= -0.05) {
                    d2 = 0.0;
                    continue;
                }
                if (d2 > 0.0) {
                    d2 -= 0.05;
                    continue;
                }
                d2 += 0.05;
            }
            while (d != 0.0 && d2 != 0.0 && noCollision(grimPlayer.entityPlayer, grimPlayer.boundingBox.d(d, -maxUpStep, d2))) {
                d = d < 0.05 && d >= -0.05 ? 0.0 : (d > 0.0 ? (d -= 0.05) : (d += 0.05));
                if (d2 < 0.05 && d2 >= -0.05) {
                    d2 = 0.0;
                    continue;
                }
                if (d2 > 0.0) {
                    d2 -= 0.05;
                    continue;
                }
                d2 += 0.05;
            }
            vec3 = new Vector(d, vec3.getY(), d2);
        }*/
        return vec3;
    }

    // TODO: Getting bounding box is wrong with lag, maybe not async safe
    private static boolean isAboveGround(GrimPlayer grimPlayer) {
        //Player bukkitPlayer = grimPlayer.bukkitPlayer;

        return false;
        /*return grimPlayer.lastOnGround || grimPlayer.fallDistance < Collisions.maxUpStep && !
                noCollision(grimPlayer.entityPlayer, grimPlayer.boundingBox.d(0.0, grimPlayer.fallDistance - Collisions.maxUpStep, 0.0));*/
    }

    public static void handleInsideBlocks(GrimPlayer grimPlayer) {
        // Use the bounding box for after the player's movement is applied
        SimpleCollisionBox aABB = GetBoundingBox.getPlayerBoundingBox(grimPlayer.x, grimPlayer.y, grimPlayer.z, grimPlayer.isSneaking, grimPlayer.bukkitPlayer.isGliding(), grimPlayer.isSwimming, grimPlayer.bukkitPlayer.isSleeping(), grimPlayer.clientVersion);
        Location blockPos = new Location(grimPlayer.playerWorld, aABB.minX + 0.001, aABB.minY + 0.001, aABB.minZ + 0.001);
        Location blockPos2 = new Location(grimPlayer.playerWorld, aABB.maxX - 0.001, aABB.maxY - 0.001, aABB.maxZ - 0.001);

        if (!CheckIfChunksLoaded.hasChunksAt(blockPos.getBlockX(), blockPos.getBlockY(), blockPos.getBlockZ(), blockPos2.getBlockX(), blockPos2.getBlockY(), blockPos2.getBlockZ()))
            return;

        for (int i = blockPos.getBlockX(); i <= blockPos2.getX(); ++i) {
            for (int j = blockPos.getBlockY(); j <= blockPos2.getY(); ++j) {
                for (int k = blockPos.getBlockZ(); k <= blockPos2.getZ(); ++k) {
                    Block block = ChunkCache.getBlockDataAt(i, j, k).getBlock();

                    if (block instanceof BlockWeb) {
                        grimPlayer.stuckSpeedMultiplier = new Vector(0.25, 0.05000000074505806, 0.25);
                    }

                    if (block instanceof BlockSweetBerryBush) {
                        grimPlayer.stuckSpeedMultiplier = new Vector(0.800000011920929, 0.75, 0.800000011920929);
                    }

                    if (block instanceof BlockBubbleColumn) {
                        IBlockData blockData = ChunkCache.getBlockDataAt(i, j, k);
                        IBlockData blockAbove = ChunkCache.getBlockDataAt(i, j + 1, k).getBlock().getBlockData();

                        if (blockAbove.isAir()) {
                            for (Vector vector : grimPlayer.getPossibleVelocitiesMinusKnockback()) {
                                if (blockData.get(DRAG_DOWN)) {
                                    vector.setY(Math.max(-0.9D, vector.getY() - 0.03D));
                                } else {
                                    vector.setY(Math.min(1.8D, vector.getY() + 0.1D));
                                }
                            }
                        } else {
                            for (Vector vector : grimPlayer.getPossibleVelocitiesMinusKnockback()) {
                                if (blockData.get(DRAG_DOWN)) {
                                    vector.setY(Math.max(-0.3D, vector.getY() - 0.03D));
                                } else {
                                    vector.setY(Math.min(0.7D, vector.getY() + 0.06D));
                                }
                            }
                        }
                    }

                    if (block instanceof BlockHoney) {
                        for (Vector vector : grimPlayer.getPossibleVelocitiesMinusKnockback()) {
                            if (isSlidingDown(vector, grimPlayer, i, j, j)) {
                                if (vector.getY() < -0.13D) {
                                    double d0 = -0.05 / vector.getY();
                                    vector.setX(vector.getX() * d0);
                                    vector.setY(-0.05D);
                                    vector.setZ(vector.getZ() * d0);
                                } else {
                                    vector.setY(-0.05D);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private static boolean isSlidingDown(Vector vector, GrimPlayer grimPlayer, int locationX, int locationY, int locationZ) {
        if (grimPlayer.onGround) {
            return false;
        } else if (grimPlayer.y > locationY + 0.9375D - 1.0E-7D) {
            return false;
        } else if (vector.getY() >= -0.08D) {
            return false;
        } else {
            double d0 = Math.abs((double) locationX + 0.5D - grimPlayer.lastX);
            double d1 = Math.abs((double) locationZ + 0.5D - grimPlayer.lastZ);
            // Calculate player width using bounding box, which will change while swimming or gliding
            double d2 = 0.4375D + ((grimPlayer.boundingBox.maxX - grimPlayer.boundingBox.minX) / 2.0F);
            return d0 + 1.0E-7D > d2 || d1 + 1.0E-7D > d2;
        }
    }

    public static boolean noCollision(Entity p_226665_1_, AxisAlignedBB p_226665_2_) {
        return noCollision(p_226665_1_, p_226665_2_, (p_234863_0_) -> {
            return true;
        });
    }

    public static boolean noCollision(@Nullable Entity p_234865_1_, AxisAlignedBB
            p_234865_2_, Predicate<Entity> p_234865_3_) {
        // TODO: Optimize this - meaning rip out anything 1.13+
        // I still don't understand why we have 1.13 collisions

        //return getCollisions(p_234865_1_, p_234865_2_, p_234865_3_).allMatch(VoxelShape::isEmpty);
        return true;
    }

    /*public static Stream<VoxelShape> getCollisions(@Nullable Entity p_234867_1_, AxisAlignedBB
            p_234867_2_, Predicate<Entity> p_234867_3_) {
        return Stream.concat(getBlockCollisions(p_234867_1_, p_234867_2_), getEntityCollisions(p_234867_1_, p_234867_2_, p_234867_3_));
    }

    public static Stream<VoxelShape> getBlockCollisions(@Nullable Entity p_226666_1_, AxisAlignedBB p_226666_2_) {
        return StreamSupport.stream(new CachedVoxelShapeSpliterator(p_226666_1_, p_226666_2_), false);
    }*/

    // Just a test
    // grimPlayer will be used eventually to get blocks from the player's cache
    public static List<SimpleCollisionBox> getCollisionBoxes(GrimPlayer grimPlayer, SimpleCollisionBox wantedBB) {
        List<SimpleCollisionBox> listOfBlocks = new ArrayList<>();

        // Not the fasted way to iterate but everything is broken anyways
        for (int x = (int) Math.floor(wantedBB.minX); x < Math.ceil(wantedBB.maxX); x++) {
            for (int y = (int) Math.floor(wantedBB.minY); x < Math.ceil(wantedBB.maxY); x++) {
                for (int z = (int) Math.floor(wantedBB.minZ); x < Math.ceil(wantedBB.maxZ); x++) {
                    BlockData.getData(Material.getMaterial(ChunkCache.getBlockDataAt(x, y, z).getMaterial().toString()));
                }
            }
        }

        return listOfBlocks;
    }

    // TODO: We need to use the grim player's bounding box
    /*public static Stream<VoxelShape> getEntityCollisions(Entity p_230318_1_, AxisAlignedBB
            p_230318_2_, Predicate<Entity> p_230318_3_) {
        /*if (p_230318_2_.a() < 1.0E-7D) { // a() -> getSize()
            return Stream.empty();
        } else {
            AxisAlignedBB axisalignedbb = p_230318_2_.grow(1.0E-7D); // g() -> inflate()
            return getEntities(p_230318_1_, axisalignedbb, p_230318_3_.and((p_234892_2_) -> {
                if (p_234892_2_.getBoundingBox().c(axisalignedbb)) { // c() -> intersects()
                    // The player entity is not going to be null
                    /*if (p_230318_1_ == null) {
                        if (p_234892_2_.canBeCollidedWith()) {
                            return true;
                        }
                    return p_230318_1_.canCollideWith(p_234892_2_);
                }

                return false;
            })).stream().map(Entity::getBoundingBox).map(VoxelShapes::a);
        }*/
    //}

    public static List<Entity> getEntities(@Nullable Entity p_175674_1_, AxisAlignedBB
            p_175674_2_, @Nullable Predicate<? super Entity> p_175674_3_) {
        List<Entity> list = Lists.newArrayList();
        int i = MathHelper.floor((p_175674_2_.minX - 2.0D) / 16.0D);
        int j = MathHelper.floor((p_175674_2_.maxX + 2.0D) / 16.0D);
        int k = MathHelper.floor((p_175674_2_.minZ - 2.0D) / 16.0D);
        int l = MathHelper.floor((p_175674_2_.maxZ + 2.0D) / 16.0D);

        // TODO: This entire method lmao
        /*for (int i1 = i; i1 <= j; ++i1) {
            for (int j1 = k; j1 <= l; ++j1) {
                Chunk chunk = abstractchunkprovider.getChunk(i1, j1, false);
                if (chunk != null) {
                    chunk.getEntities(p_175674_1_, p_175674_2_, list, p_175674_3_);
                }
            }
        }*/

        return list;
    }

    public static boolean onClimbable(GrimPlayer grimPlayer) {
        // spectator check

        IBlockData blockData = ChunkCache.getBlockDataAt(grimPlayer.x, grimPlayer.y, grimPlayer.z);
        if (blockData.a(TagsBlock.CLIMBABLE)) {
            return true;
        }

        return blockData.getBlock() instanceof BlockTrapdoor && trapdoorUsableAsLadder(grimPlayer.x, grimPlayer.y, grimPlayer.z, blockData);
    }

    private static boolean trapdoorUsableAsLadder(double x, double y, double z, IBlockData blockData) {
        if (blockData.get(BlockTrapdoor.OPEN)) {
            IBlockData blockBelow = ChunkCache.getBlockDataAt(x, y - 1, z);
            return blockBelow.a(Blocks.LADDER) && blockBelow.get(BlockLadder.FACING) == blockData.get(BlockLadder.FACING);
        }

        return false;
    }

    private static int a(double d0, double d1) {
        if (d0 >= -1.0E-7D && d1 <= 1.0000001D) {
            for (int i = 0; i <= 3; ++i) {
                double d2 = d0 * (double) (1 << i);
                double d3 = d1 * (double) (1 << i);
                boolean flag = Math.abs(d2 - Math.floor(d2)) < 1.0E-7D;
                boolean flag1 = Math.abs(d3 - Math.floor(d3)) < 1.0E-7D;

                if (flag && flag1) {
                    return i;
                }
            }

            return -1;
        } else {
            return -1;
        }
    }
}
