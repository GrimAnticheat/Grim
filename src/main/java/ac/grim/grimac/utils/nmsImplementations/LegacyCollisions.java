package ac.grim.grimac.utils.nmsImplementations;

import ac.grim.grimac.GrimPlayer;
import ac.grim.grimac.utils.chunks.ChunkCache;
import ac.grim.grimac.utils.nmsImplementations.tuinityVoxelShapes.AxisAlignedBB;
import net.minecraft.server.v1_16_R3.MathHelper;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;


public class LegacyCollisions {
    public static Vector collide(GrimPlayer grimPlayer, double xWithCollision, double yWithCollision, double zWithCollision) {
        AxisAlignedBB currentPosBB = GetBoundingBox.getPlayerBoundingBox(grimPlayer.lastX, grimPlayer.lastY, grimPlayer.lastZ, grimPlayer.wasSneaking, grimPlayer.bukkitPlayer.isGliding(), grimPlayer.isSwimming, grimPlayer.bukkitPlayer.isSleeping(), grimPlayer.clientVersion);

        List<AxisAlignedBB> desiredMovementCollisionBoxes = getCollisionBoxes(grimPlayer, currentPosBB.a(xWithCollision, yWithCollision, zWithCollision));
        AxisAlignedBB setBB = currentPosBB;

        double clonedX = xWithCollision;
        double clonedY = yWithCollision;
        double clonedZ = zWithCollision;

        // First, collisions are ran without any step height, in y -> x -> z order
        // Interestingly, MC-Market forks love charging hundreds for a slight change in this
        // In 1.7/1.8 cannoning jars, if Z > X, order is Y -> Z -> X, or Z < X, Y -> X -> Z
        // Mojang implemented the if Z > X thing in 1.14+
        if (yWithCollision != 0.0D) {
            for (AxisAlignedBB bb : desiredMovementCollisionBoxes) {
                yWithCollision = AxisAlignedBB.collideY(bb, currentPosBB, yWithCollision);
            }

            setBB = setBB.offset(0.0D, yWithCollision, 0.0D);
        }

        if (xWithCollision != 0.0D) {
            for (AxisAlignedBB bb : desiredMovementCollisionBoxes) {
                xWithCollision = AxisAlignedBB.collideY(bb, currentPosBB, xWithCollision);
            }

            if (xWithCollision != 0) {
                setBB = setBB.offset(xWithCollision, 0.0D, 0.0D);
            }
        }

        if (zWithCollision != 0.0D) {
            for (AxisAlignedBB bb : desiredMovementCollisionBoxes) {
                zWithCollision = AxisAlignedBB.collideZ(bb, currentPosBB, zWithCollision);
            }

            if (zWithCollision != 0) {
                setBB = setBB.offset(0.0D, 0.0D, zWithCollision);
            }
        }


        boolean movingIntoGround = grimPlayer.lastOnGround || clonedY != yWithCollision && clonedY < 0.0D;

        // If the player has x or z collision, is going in the downwards direction in the last or this tick, and can step up
        // If not, just return the collisions without stepping up that we calculated earlier
        if (grimPlayer.getMaxUpStep() > 0.0F && movingIntoGround && (clonedX != xWithCollision || clonedZ != zWithCollision)) {
            double d14 = xWithCollision;
            double d6 = yWithCollision;
            double d7 = zWithCollision;
            double stepUpHeight = grimPlayer.getMaxUpStep();

            // Get a list of bounding boxes from the player's current bounding box to the wanted coordinates
            List<AxisAlignedBB> stepUpCollisionBoxes = getCollisionBoxes(grimPlayer, setBB.expandToCoordinate(clonedX, stepUpHeight, clonedZ));


            // Adds a coordinate to the bounding box, extending it if the point lies outside the current ranges. - mcp
            // Note that this will include bounding boxes that we don't need, but the next code can handle it
            AxisAlignedBB expandedToCoordinateBB = setBB.expandToCoordinate(clonedX, 0.0D, clonedZ);
            double stepMaxClone = stepUpHeight;
            // See how far upwards we go in the Y axis with coordinate expanded collision
            for (AxisAlignedBB bb : desiredMovementCollisionBoxes) {
                stepMaxClone = AxisAlignedBB.collideY(bb, expandedToCoordinateBB, stepMaxClone);
            }


            // TODO: We could probably return normal collision if stepMaxClone == 0 - as we aren't stepping on anything
            // Check some 1.8 jar for it - TacoSpigot would be the best bet for any optimizations here
            // I do need to debug that though. Not sure.
            AxisAlignedBB yCollisionStepUpBB = setBB;


            yCollisionStepUpBB = yCollisionStepUpBB.offset(0.0D, stepMaxClone, 0.0D);

            // Calculate X offset
            double clonedClonedX = clonedX;
            for (AxisAlignedBB bb : stepUpCollisionBoxes) {
                clonedClonedX = AxisAlignedBB.collideX(bb, yCollisionStepUpBB, clonedClonedX);
            }
            yCollisionStepUpBB = yCollisionStepUpBB.offset(clonedClonedX, 0.0D, 0.0D);

            // Calculate Z offset
            double clonedClonedZ = clonedZ;
            for (AxisAlignedBB bb : stepUpCollisionBoxes) {
                clonedClonedZ = AxisAlignedBB.collideZ(bb, yCollisionStepUpBB, clonedClonedZ);
            }
            yCollisionStepUpBB = yCollisionStepUpBB.offset(0.0D, 0.0D, clonedClonedZ);


            // Then calculate collisions with the step up height added to the Y axis
            AxisAlignedBB alwaysStepUpBB = setBB;
            // Calculate y offset
            double stepUpHeightCloned = stepUpHeight;
            for (AxisAlignedBB bb : stepUpCollisionBoxes) {
                stepUpHeightCloned = AxisAlignedBB.collideY(bb, alwaysStepUpBB, stepUpHeightCloned);
            }
            alwaysStepUpBB = alwaysStepUpBB.offset(0.0D, stepUpHeightCloned, 0.0D);
            // Calculate X offset
            double xWithCollisionClonedOnceAgain = clonedX;
            for (AxisAlignedBB bb : stepUpCollisionBoxes) {
                xWithCollisionClonedOnceAgain = AxisAlignedBB.collideX(bb, alwaysStepUpBB, xWithCollisionClonedOnceAgain);
            }
            alwaysStepUpBB = alwaysStepUpBB.offset(xWithCollisionClonedOnceAgain, 0.0D, 0.0D);
            // Calculate Z offset
            double zWithCollisionClonedOnceAgain = clonedZ;
            for (AxisAlignedBB bb : stepUpCollisionBoxes) {
                zWithCollisionClonedOnceAgain = AxisAlignedBB.collideX(bb, alwaysStepUpBB, zWithCollisionClonedOnceAgain);
            }
            alwaysStepUpBB = alwaysStepUpBB.offset(0.0D, 0.0D, zWithCollisionClonedOnceAgain);


            double d23 = clonedClonedX * clonedClonedX + clonedClonedZ * clonedClonedZ;
            double d9 = xWithCollisionClonedOnceAgain * xWithCollisionClonedOnceAgain + zWithCollisionClonedOnceAgain * zWithCollisionClonedOnceAgain;
            setBB = d23 > d9 ? yCollisionStepUpBB : alwaysStepUpBB;

            for (AxisAlignedBB bb : stepUpCollisionBoxes) {
                yWithCollision = AxisAlignedBB.collideY(bb, setBB, yWithCollision);
            }

            setBB = setBB.offset(0.0D, yWithCollision, 0.0D);

            if (d14 * d14 + d7 * d7 >= xWithCollision * xWithCollision + zWithCollision * zWithCollision) {
                setBB = currentPosBB;
            }
        }

        // Convert bounding box movement back into a vector
        return new Vector(setBB.minX - currentPosBB.minX, setBB.minY - currentPosBB.minY, setBB.minZ - currentPosBB.minZ);
    }

    // Just a test
    // grimPlayer will be used eventually to get blocks from the player's cache
    public static List<AxisAlignedBB> getCollisionBoxes(GrimPlayer grimPlayer, AxisAlignedBB wantedBB) {
        List<AxisAlignedBB> listOfBlocks = new ArrayList<>();

        for (int minY = MathHelper.floor(wantedBB.minY) - 1; minY < Math.ceil(wantedBB.maxY) + 1; minY++) {
            for (int minZ = MathHelper.floor(wantedBB.minZ) - 1; minZ < Math.ceil(wantedBB.maxZ) + 1; minZ++) {
                for (int minX = MathHelper.floor(wantedBB.minX) - 1; minX < Math.ceil(wantedBB.maxX) + 1; minX++) {
                    if (ChunkCache.getBlockAt(minX, minY, minZ) != 0) {
                        listOfBlocks.add(new AxisAlignedBB(minX, minY, minZ, minX + 1, minY + 1, minZ + 1));
                    }
                }
            }
        }

        return listOfBlocks;
    }
}