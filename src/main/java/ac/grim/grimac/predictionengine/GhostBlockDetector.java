package ac.grim.grimac.predictionengine;

import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PostPredictionCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.PredictionComplete;
import ac.grim.grimac.utils.collisions.datatypes.SimpleCollisionBox;
import ac.grim.grimac.utils.data.packetentity.PacketEntity;
import ac.grim.grimac.utils.enums.EntityType;
import ac.grim.grimac.utils.math.GrimMath;
import ac.grim.grimac.utils.nmsImplementations.Collisions;
import io.github.retrooper.packetevents.utils.player.ClientVersion;

@CheckData(buffer = 3, maxBuffer = 3)
public class GhostBlockDetector extends PostPredictionCheck {

    public GhostBlockDetector(GrimPlayer player) {
        super(player);
    }

    // Must process data first to get rid of false positives from ghost blocks
    public void onPredictionComplete(final PredictionComplete predictionComplete) {
        // If the offset is low, there probably isn't ghost blocks
        if (predictionComplete.getOffset() < 0.001) return;

        boolean shouldResync = isGhostBlock();

        if (shouldResync) {
            // GHOST BLOCK DETECTED!  What now?
            // 0.01 - 0.001 = 6 vl to resync
            // 0.1 - 0.01 = 3 vl to resync
            // 0.1+ = 1 vl to resync
            if (predictionComplete.getOffset() < 0.01) decreaseBuffer(0.5);
            else if (predictionComplete.getOffset() < 0.1) decreaseBuffer(1);
            else decreaseBuffer(3);

            if (getBuffer() <= 0) {
                predictionComplete.setOffset(0);
                player.getSetbackTeleportUtil().executeForceResync();
            }
        } else {
            increaseBuffer(0.025);
        }
    }

    private boolean isGhostBlock() {
        if (true) return false;

        // Player is on glitchy block (1.8 client on anvil/wooden chest)
        if (player.uncertaintyHandler.isOrWasNearGlitchyBlock) {
            return true;
        }

        // Reliable way to check if the player is colliding vertically with a block that doesn't exist
        if (player.clientClaimsLastOnGround && player.clientControlledVerticalCollision && Collisions.collide(player, 0, -SimpleCollisionBox.COLLISION_EPSILON, 0).getY() == -SimpleCollisionBox.COLLISION_EPSILON) {
            return true;
        }

        // Player is colliding upwards into a ghost block
        if (player.y > player.lastY && Math.abs((player.y + player.pose.height) % (1 / 64D)) < 0.00001 && Collisions.collide(player, 0, SimpleCollisionBox.COLLISION_EPSILON, 0).getY() == SimpleCollisionBox.COLLISION_EPSILON) {
            return true;
        }

        // Somewhat reliable way to detect if the player is colliding in the X negative/X positive axis on a ghost block
        if (GrimMath.distanceToHorizontalCollision(player.x) < 1e-7) {
            boolean xPosCol = Collisions.collide(player, SimpleCollisionBox.COLLISION_EPSILON, 0, 0).getX() != SimpleCollisionBox.COLLISION_EPSILON;
            boolean xNegCol = Collisions.collide(player, -SimpleCollisionBox.COLLISION_EPSILON, 0, 0).getX() != -SimpleCollisionBox.COLLISION_EPSILON;

            if (!xPosCol && !xNegCol) {
                return true;
            }
        }

        // Somewhat reliable way to detect if the player is colliding in the Z negative/Z positive axis on a ghost block
        if (GrimMath.distanceToHorizontalCollision(player.z) < 1e-7) {
            boolean zPosCol = Collisions.collide(player, 0, 0, SimpleCollisionBox.COLLISION_EPSILON).getZ() != SimpleCollisionBox.COLLISION_EPSILON;
            boolean zNegCol = Collisions.collide(player, 0, 0, -SimpleCollisionBox.COLLISION_EPSILON).getZ() != -SimpleCollisionBox.COLLISION_EPSILON;

            if (!zPosCol && !zNegCol) {
                return true;
            }
        }

        // Boats are moved client sided by 1.7/1.8 players, and have a mind of their own
        // Simply setback, don't ban, if a player gets a violation by a boat.
        // Note that we allow setting back to the ground for this one, to try and mitigate
        // the effect that this buggy behavior has on players
        if (player.getClientVersion().isOlderThan(ClientVersion.v_1_9)) {
            SimpleCollisionBox largeExpandedBB = player.boundingBox.copy().expand(12, 0.5, 12);

            for (PacketEntity entity : player.compensatedEntities.entityMap.values()) {
                if (entity.type == EntityType.BOAT) {
                    if (entity.getPossibleCollisionBoxes().isIntersected(largeExpandedBB)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }
}
