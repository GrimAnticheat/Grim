package ac.grim.grimac.utils.collisions;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.collisions.types.SimpleCollisionBox;
import ac.grim.grimac.utils.data.ProtocolVersion;
import ac.grim.grimac.utils.data.VectorData;
import ac.grim.grimac.utils.enums.MoverType;
import ac.grim.grimac.utils.nmsImplementations.CheckIfChunksLoaded;
import ac.grim.grimac.utils.nmsImplementations.CollisionData;
import ac.grim.grimac.utils.nmsImplementations.GetBoundingBox;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.BubbleColumn;
import org.bukkit.block.data.type.Ladder;
import org.bukkit.block.data.type.TrapDoor;
import org.bukkit.entity.Boat;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

public class Collisions {
    public static final double maxUpStep = 0.6f;
    // Entity line 686
    // This MUST return a new vector!!!
    // If it does not the predicted velocity will be overridden
    public static Vector collide(GrimPlayer player, double xWithCollision, double yWithCollision, double zWithCollision) {
        SimpleCollisionBox currentPosBB = player.boundingBox;

        List<SimpleCollisionBox> desiredMovementCollisionBoxes = getCollisionBoxes(player, currentPosBB.copy().expandToCoordinate(xWithCollision, yWithCollision, zWithCollision));
        SimpleCollisionBox setBB = currentPosBB.copy();
        double setX = 0;
        double setY = 0;
        double setZ = 0;

        double clonedX = xWithCollision;
        double clonedY = yWithCollision;
        double clonedZ = zWithCollision;

        // First, collisions are ran without any step height, in y -> x -> z order
        // Interestingly, MC-Market forks love charging hundreds for a slight change in this
        // In 1.7/1.8 cannoning jars, if Z > X, order is Y -> Z -> X, or Z < X, Y -> X -> Z
        // Mojang implemented the if Z > X thing in 1.14+
        if (yWithCollision != 0.0D) {
            for (SimpleCollisionBox bb : desiredMovementCollisionBoxes) {
                yWithCollision = bb.collideY(setBB, yWithCollision);
            }

            setBB.offset(0.0D, yWithCollision, 0.0D);
            setY += yWithCollision;
        }

        if (Math.abs(zWithCollision) > Math.abs(xWithCollision) && player.clientVersion >= 477) {
            if (zWithCollision != 0.0D) {
                for (SimpleCollisionBox bb : desiredMovementCollisionBoxes) {
                    zWithCollision = bb.collideZ(setBB, zWithCollision);
                }

                if (zWithCollision != 0) {
                    setBB.offset(0.0D, 0.0D, zWithCollision);
                    setZ += zWithCollision;
                }
            }

            if (xWithCollision != 0.0D) {
                for (SimpleCollisionBox bb : desiredMovementCollisionBoxes) {
                    xWithCollision = bb.collideX(setBB, xWithCollision);
                }

                if (xWithCollision != 0) {
                    setBB.offset(xWithCollision, 0.0D, 0.0D);
                    setX += xWithCollision;
                }
            }
        } else {
            if (xWithCollision != 0.0D) {
                for (SimpleCollisionBox bb : desiredMovementCollisionBoxes) {
                    xWithCollision = bb.collideX(setBB, xWithCollision);
                }

                if (xWithCollision != 0) {
                    setBB.offset(xWithCollision, 0.0D, 0.0D);
                    setX += xWithCollision;
                }
            }

            if (zWithCollision != 0.0D) {
                for (SimpleCollisionBox bb : desiredMovementCollisionBoxes) {
                    zWithCollision = bb.collideZ(setBB, zWithCollision);
                }

                if (zWithCollision != 0) {
                    setBB.offset(0.0D, 0.0D, zWithCollision);
                    setZ += zWithCollision;
                }
            }
        }


        boolean movingIntoGround = player.lastOnGround || clonedY != yWithCollision && clonedY < 0.0D;

        // If the player has x or z collision, is going in the downwards direction in the last or this tick, and can step up
        // If not, just return the collisions without stepping up that we calculated earlier
        if (player.getMaxUpStep() > 0.0F && movingIntoGround && (clonedX != xWithCollision || clonedZ != zWithCollision)) {
            double stepUpHeight = player.getMaxUpStep();
            // Undo the offsets done above, but keep the result in justAfterCollisionBB
            SimpleCollisionBox justAfterCollisionBB = setBB.copy();
            setBB = currentPosBB.copy();


            // Get a list of bounding boxes from the player's current bounding box to the wanted coordinates
            List<SimpleCollisionBox> stepUpCollisionBoxes = getCollisionBoxes(player, setBB.copy().expandToCoordinate(clonedX, stepUpHeight, clonedZ));

            // Adds a coordinate to the bounding box, extending it if the point lies outside the current ranges. - mcp
            // Note that this will include bounding boxes that we don't need, but the next code can handle it
            SimpleCollisionBox expandedToCoordinateBB = setBB.copy().expandToCoordinate(clonedX, 0.0D, clonedZ);
            double stepMaxClone = stepUpHeight;
            // See how far upwards we go in the Y axis with coordinate expanded collision
            for (SimpleCollisionBox bb : desiredMovementCollisionBoxes) {
                stepMaxClone = bb.collideY(expandedToCoordinateBB, stepMaxClone);
            }


            // TODO: We could probably return normal collision if stepMaxClone == 0 - as we aren't stepping on anything
            // Check some 1.8 jar for it - TacoSpigot would be the best bet for any optimizations here
            // I do need to debug that though. Not sure.
            SimpleCollisionBox yCollisionStepUpBB = currentPosBB.copy();
            double xSetYCol = 0;
            double ySetYCol = 0;
            double zSetYCol = 0;

            yCollisionStepUpBB.offset(0.0D, stepMaxClone, 0.0D);
            ySetYCol += stepMaxClone;

            double clonedClonedX;
            double clonedClonedZ;
            if (Math.abs(zWithCollision) > Math.abs(xWithCollision) && player.clientVersion >= 477) {
                // Calculate Z offset
                clonedClonedZ = clonedZ;
                for (SimpleCollisionBox bb : stepUpCollisionBoxes) {
                    clonedClonedZ = bb.collideZ(yCollisionStepUpBB, clonedClonedZ);
                }
                yCollisionStepUpBB.offset(0.0D, 0.0D, clonedClonedZ);
                zSetYCol += clonedClonedZ;
                // Calculate X offset
                clonedClonedX = clonedX;
                for (SimpleCollisionBox bb : stepUpCollisionBoxes) {
                    clonedClonedX = bb.collideX(yCollisionStepUpBB, clonedClonedX);
                }
                yCollisionStepUpBB.offset(clonedClonedX, 0.0D, 0.0D);
                xSetYCol += clonedClonedX;
            } else {
                // Calculate X offset
                clonedClonedX = clonedX;
                for (SimpleCollisionBox bb : stepUpCollisionBoxes) {
                    clonedClonedX = bb.collideX(yCollisionStepUpBB, clonedClonedX);
                }
                yCollisionStepUpBB.offset(clonedClonedX, 0.0D, 0.0D);
                xSetYCol += clonedClonedX;

                // Calculate Z offset
                clonedClonedZ = clonedZ;
                for (SimpleCollisionBox bb : stepUpCollisionBoxes) {
                    clonedClonedZ = bb.collideZ(yCollisionStepUpBB, clonedClonedZ);
                }
                yCollisionStepUpBB.offset(0.0D, 0.0D, clonedClonedZ);
                zSetYCol += clonedClonedZ;
            }

            // Then calculate collisions with the step up height added to the Y axis
            SimpleCollisionBox alwaysStepUpBB = currentPosBB.copy();
            double xAlways = 0;
            double yAlways = 0;
            double zAlways = 0;

            // Calculate y offset
            double stepUpHeightCloned = stepUpHeight;
            for (SimpleCollisionBox bb : stepUpCollisionBoxes) {
                stepUpHeightCloned = bb.collideY(alwaysStepUpBB, stepUpHeightCloned);
            }
            alwaysStepUpBB.offset(0.0D, stepUpHeightCloned, 0.0D);
            yAlways += stepUpHeightCloned;

            double zWithCollisionClonedOnceAgain;
            double xWithCollisionClonedOnceAgain;
            if (Math.abs(zWithCollision) > Math.abs(xWithCollision) && player.clientVersion >= 477) {
                // Calculate Z offset
                zWithCollisionClonedOnceAgain = clonedZ;
                for (SimpleCollisionBox bb : stepUpCollisionBoxes) {
                    zWithCollisionClonedOnceAgain = bb.collideZ(alwaysStepUpBB, zWithCollisionClonedOnceAgain);
                }
                alwaysStepUpBB.offset(0.0D, 0.0D, zWithCollisionClonedOnceAgain);
                zAlways += zWithCollisionClonedOnceAgain;
                // Calculate X offset
                xWithCollisionClonedOnceAgain = clonedX;
                for (SimpleCollisionBox bb : stepUpCollisionBoxes) {
                    xWithCollisionClonedOnceAgain = bb.collideX(alwaysStepUpBB, xWithCollisionClonedOnceAgain);
                }
                alwaysStepUpBB.offset(xWithCollisionClonedOnceAgain, 0.0D, 0.0D);
                xAlways += xWithCollisionClonedOnceAgain;
            } else {
                // Calculate X offset
                xWithCollisionClonedOnceAgain = clonedX;
                for (SimpleCollisionBox bb : stepUpCollisionBoxes) {
                    xWithCollisionClonedOnceAgain = bb.collideX(alwaysStepUpBB, xWithCollisionClonedOnceAgain);
                }
                alwaysStepUpBB.offset(xWithCollisionClonedOnceAgain, 0.0D, 0.0D);
                xAlways += xWithCollisionClonedOnceAgain;
                // Calculate Z offset
                zWithCollisionClonedOnceAgain = clonedZ;
                for (SimpleCollisionBox bb : stepUpCollisionBoxes) {
                    zWithCollisionClonedOnceAgain = bb.collideZ(alwaysStepUpBB, zWithCollisionClonedOnceAgain);
                }
                alwaysStepUpBB.offset(0.0D, 0.0D, zWithCollisionClonedOnceAgain);
                zAlways += zWithCollisionClonedOnceAgain;
            }


            double d23 = clonedClonedX * clonedClonedX + clonedClonedZ * clonedClonedZ;
            double d9 = xWithCollisionClonedOnceAgain * xWithCollisionClonedOnceAgain + zWithCollisionClonedOnceAgain * zWithCollisionClonedOnceAgain;

            double x;
            double y;
            double z;

            double originalSetX = setX;
            double originalSetY = setY;
            double originalSetZ = setZ;

            if (d23 > d9) {
                x = clonedClonedX;
                y = -stepMaxClone;
                z = clonedClonedZ;
                setBB = yCollisionStepUpBB;
                setX = xSetYCol;
                setY = ySetYCol;
                setZ = zSetYCol;
            } else {
                x = xWithCollisionClonedOnceAgain;
                y = -stepUpHeightCloned;
                z = zWithCollisionClonedOnceAgain;
                setBB = alwaysStepUpBB;
                setX = xAlways;
                setY = yAlways;
                setZ = zAlways;
            }

            for (SimpleCollisionBox bb : stepUpCollisionBoxes) {
                y = bb.collideY(setBB, y);
            }


            setBB.offset(0.0D, y, 0.0D);
            setY += y;

            if (xWithCollision * xWithCollision + zWithCollision * zWithCollision >= x * x + z * z) {
                setBB = justAfterCollisionBB;
                setX = originalSetX;
                setY = originalSetY;
                setZ = originalSetZ;
            }
        }

        // Convert bounding box movement back into a vector
        return new Vector(setX, setY, setZ);
        //return new Vector(setBB.minX - currentPosBB.minX, setBB.minY - currentPosBB.minY, setBB.minZ - currentPosBB.minZ);
    }

    // MCP mappings PlayerEntity 959
    // Mojang mappings 911
    public static Vector maybeBackOffFromEdge(Vector vec3, MoverType moverType, GrimPlayer player) {
        if (!player.specialFlying && (moverType == MoverType.SELF || moverType == MoverType.PLAYER) && player.isSneaking && isAboveGround(player)) {
            double d = vec3.getX();
            double d2 = vec3.getZ();
            while (d != 0.0 && isEmpty(player, player.boundingBox.copy().offset(d, -maxUpStep, 0.0))) {
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
            while (d2 != 0.0 && isEmpty(player, player.boundingBox.copy().offset(0.0, -maxUpStep, d2))) {
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
            while (d != 0.0 && d2 != 0.0 && isEmpty(player, player.boundingBox.copy().offset(d, -maxUpStep, d2))) {
                d = d < 0.05 && d >= -0.05 ? 0.0 : (d > 0.0 ? d - 0.05 : d + 0.05);
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
        }
        return vec3;
    }

    private static boolean isAboveGround(GrimPlayer player) {
        //Player bukkitPlayer = player.bukkitPlayer;

        return player.lastOnGround || player.fallDistance < Collisions.maxUpStep &&
                !isEmpty(player, player.boundingBox.copy().offset(0.0, player.fallDistance - Collisions.maxUpStep, 0.0));
    }

    public static void handleInsideBlocks(GrimPlayer player) {
        // Use the bounding box for after the player's movement is applied

        // This is a terrible hack :(
        SimpleCollisionBox aABB;
        if (player.playerVehicle instanceof Boat) {
            aABB = GetBoundingBox.getBoatBoundingBox(player.x, player.y, player.z);
        } else {
            aABB = GetBoundingBox.getPlayerBoundingBox(player, player.x, player.y, player.z);
        }

        Location blockPos = new Location(player.playerWorld, aABB.minX + 0.001D, aABB.minY + 0.001D, aABB.minZ + 0.001D);
        Location blockPos2 = new Location(player.playerWorld, aABB.maxX - 0.001D, aABB.maxY - 0.001D, aABB.maxZ - 0.001D);

        if (CheckIfChunksLoaded.isChunksUnloadedAt(player, blockPos.getBlockX(), blockPos.getBlockY(), blockPos.getBlockZ(), blockPos2.getBlockX(), blockPos2.getBlockY(), blockPos2.getBlockZ()))
            return;

        for (int i = blockPos.getBlockX(); i <= blockPos2.getBlockX(); ++i) {
            for (int j = blockPos.getBlockY(); j <= blockPos2.getBlockY(); ++j) {
                for (int k = blockPos.getBlockZ(); k <= blockPos2.getBlockZ(); ++k) {
                    BlockData block = player.compensatedWorld.getBukkitBlockDataAt(i, j, k);
                    Material blockType = block.getMaterial();

                    if (blockType == Material.COBWEB) {
                        player.stuckSpeedMultiplier = new Vector(0.25, 0.05000000074505806, 0.25);
                    }

                    if (blockType == Material.SWEET_BERRY_BUSH) {
                        player.stuckSpeedMultiplier = new Vector(0.800000011920929, 0.75, 0.800000011920929);
                    }

                    if (blockType == Material.BUBBLE_COLUMN) {
                        BlockData blockAbove = player.compensatedWorld.getBukkitBlockDataAt(i, j + 1, k);
                        BubbleColumn bubbleColumn = (BubbleColumn) block;

                        if (player.playerVehicle instanceof Boat) {
                            if (!blockAbove.getMaterial().isAir()) {
                                if (bubbleColumn.isDrag()) {
                                    player.clientVelocity.setY(Math.max(-0.3D, player.clientVelocity.getY() - 0.03D));
                                } else {
                                    player.clientVelocity.setY(Math.min(0.7D, player.clientVelocity.getY() + 0.06D));
                                }
                            }
                        } else {
                            if (blockAbove.getMaterial().isAir()) {
                                for (VectorData vector : player.getPossibleVelocitiesMinusKnockback()) {
                                    if (bubbleColumn.isDrag()) {
                                        vector.vector.setY(Math.max(-0.9D, vector.vector.getY() - 0.03D));
                                    } else {
                                        vector.vector.setY(Math.min(1.8D, vector.vector.getY() + 0.1D));
                                    }
                                }
                            } else {
                                for (VectorData vector : player.getPossibleVelocitiesMinusKnockback()) {
                                    if (bubbleColumn.isDrag()) {
                                        vector.vector.setY(Math.max(-0.3D, vector.vector.getY() - 0.03D));
                                    } else {
                                        vector.vector.setY(Math.min(0.7D, vector.vector.getY() + 0.06D));
                                    }
                                }
                            }
                        }
                    }

                    if (blockType == Material.HONEY_BLOCK) {
                        for (VectorData vector : player.getPossibleVelocitiesMinusKnockback()) {
                            if (isSlidingDown(vector.vector, player, i, j, j)) {
                                if (vector.vector.getY() < -0.13D) {
                                    double d0 = -0.05 / vector.vector.getY();
                                    vector.vector.setX(vector.vector.getX() * d0);
                                    vector.vector.setY(-0.05D);
                                    vector.vector.setZ(vector.vector.getZ() * d0);
                                } else {
                                    vector.vector.setY(-0.05D);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private static boolean isSlidingDown(Vector vector, GrimPlayer player, int locationX, int locationY, int locationZ) {
        if (player.onGround) {
            return false;
        } else if (player.y > locationY + 0.9375D - 1.0E-7D) {
            return false;
        } else if (vector.getY() >= -0.08D) {
            return false;
        } else {
            double d0 = Math.abs((double) locationX + 0.5D - player.lastX);
            double d1 = Math.abs((double) locationZ + 0.5D - player.lastZ);
            // Calculate player width using bounding box, which will change while swimming or gliding
            double d2 = 0.4375D + ((player.pose.width) / 2.0F);
            return d0 + 1.0E-7D > d2 || d1 + 1.0E-7D > d2;
        }
    }

    // Just a test
    // player will be used eventually to get blocks from the player's cache
    public static List<SimpleCollisionBox> getCollisionBoxes(GrimPlayer player, SimpleCollisionBox wantedBB) {
        List<SimpleCollisionBox> listOfBlocks = new ArrayList<>();

        // Not the fasted way to iterate but everything is broken anyways
        for (int x = (int) Math.floor(wantedBB.minX); x <= Math.ceil(wantedBB.maxX); x++) {
            for (int y = (int) Math.floor(wantedBB.minY); y <= Math.ceil(wantedBB.maxY); y++) {
                for (int z = (int) Math.floor(wantedBB.minZ); z <= Math.ceil(wantedBB.maxZ); z++) {
                    org.bukkit.block.data.BlockData data = player.compensatedWorld.getBukkitBlockDataAt(x, y, z);
                    CollisionData.getData(data.getMaterial()).getMovementCollisionBox(data, x, y, z, ProtocolVersion.v1_16_5).downCast(listOfBlocks);
                }
            }
        }

        return listOfBlocks;
    }

    public static boolean isEmpty(GrimPlayer player, SimpleCollisionBox playerBB) {
        for (CollisionBox collisionBox : getCollisionBoxes(player, playerBB)) {
            if (collisionBox.isCollided(playerBB)) return false;
        }

        return true;
    }

    public static boolean suffocatesAt(GrimPlayer player, SimpleCollisionBox playerBB) {
        List<SimpleCollisionBox> listOfBlocks = new ArrayList<>();

        // Not the fasted way to iterate but everything is broken anyways
        for (int x = (int) Math.floor(playerBB.minX); x <= Math.ceil(playerBB.maxX); x++) {
            for (int y = (int) Math.floor(playerBB.minY); y <= Math.ceil(playerBB.maxY); y++) {
                for (int z = (int) Math.floor(playerBB.minZ); z <= Math.ceil(playerBB.maxZ); z++) {
                    org.bukkit.block.data.BlockData data = player.compensatedWorld.getBukkitBlockDataAt(x, y, z);

                    if (!data.getMaterial().isOccluding()) continue;
                    CollisionBox box = CollisionData.getData(data.getMaterial()).getMovementCollisionBox(data, x, y, z, ProtocolVersion.v1_16_5);
                    if (!box.isFullBlock()) continue;

                    box.downCast(listOfBlocks);
                }
            }
        }


        for (CollisionBox collisionBox : listOfBlocks) {
            if (collisionBox.isCollided(playerBB)) return true;
        }

        return false;
    }

    public static boolean onClimbable(GrimPlayer player) {
        // spectator check

        BlockData blockData = player.compensatedWorld.getBukkitBlockDataAt(player.x, player.y, player.z);

        if (Tag.CLIMBABLE.isTagged(blockData.getMaterial())) {
            return true;
        }

        return Tag.TRAPDOORS.isTagged(blockData.getMaterial()) && trapdoorUsableAsLadder(player, player.x, player.y, player.z, (TrapDoor) blockData);
    }

    private static boolean trapdoorUsableAsLadder(GrimPlayer player, double x, double y, double z, TrapDoor blockData) {
        if (blockData.isOpen()) {
            BlockData blockBelow = player.compensatedWorld.getBukkitBlockDataAt(x, y - 1, z);

            if (blockBelow.getMaterial() == Material.LADDER) {
                Ladder ladder = (Ladder) blockBelow;
                return ladder.getFacing() == blockData.getFacing();
            }
        }

        return false;
    }

    // 1.12 collision boxes
    /*public List<Entity> getEntitiesWithinAABBExcludingEntity(@Nullable Entity entityIn, AxisAlignedBB bb) {
        return this.getEntitiesInAABBexcluding(entityIn, bb, EntitySelectors.NOT_SPECTATING);
    }

    public List<Entity> getEntitiesInAABBexcluding(@Nullable Entity entityIn, AxisAlignedBB boundingBox, @Nullable Predicate<? super Entity> predicate) {
        List<Entity> list = Lists.<Entity>newArrayList();
        int j2 = MathHelper.floor((boundingBox.minX - 2.0D) / 16.0D);
        int k2 = MathHelper.floor((boundingBox.maxX + 2.0D) / 16.0D);
        int l2 = MathHelper.floor((boundingBox.minZ - 2.0D) / 16.0D);
        int i3 = MathHelper.floor((boundingBox.maxZ + 2.0D) / 16.0D);

        for (int j3 = j2; j3 <= k2; ++j3) {
            for (int k3 = l2; k3 <= i3; ++k3) {
                if (this.isChunkLoaded(j3, k3, true)) {
                    this.getChunkFromChunkCoords(j3, k3).getEntitiesWithinAABBForEntity(entityIn, boundingBox, list, predicate);
                }
            }
        }

        return list;
    }*/
}
