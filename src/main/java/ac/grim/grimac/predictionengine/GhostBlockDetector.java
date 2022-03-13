package ac.grim.grimac.predictionengine;

import ac.grim.grimac.checks.type.PostPredictionCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.PredictionComplete;
import ac.grim.grimac.utils.collisions.datatypes.SimpleCollisionBox;
import ac.grim.grimac.utils.data.packetentity.PacketEntity;
import ac.grim.grimac.utils.math.GrimMath;
import ac.grim.grimac.utils.nmsutil.Collisions;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import org.bukkit.util.Vector;

public class GhostBlockDetector extends PostPredictionCheck {

    public GhostBlockDetector(GrimPlayer player) {
        super(player);
    }

    // Must process data first to get rid of false positives from ghost blocks
    public void onPredictionComplete(final PredictionComplete predictionComplete) {
        // If the offset is low, there probably isn't ghost blocks
        // However, if we would flag nofall, check for ghost blocks
        if (predictionComplete.getOffset() < 0.001 && (player.clientClaimsLastOnGround == player.onGround || player.inVehicle))
            return;

        boolean shouldResync = isGhostBlock();

        if (shouldResync) {
            // I once used a buffer for this, but it should be very accurate now.
            if (player.clientClaimsLastOnGround != player.onGround) {
                // Rethink this.  Is there a better way to force the player's ground for the next tick?
                // No packet for it, so I think this is sadly the best way.
                player.onGround = player.clientClaimsLastOnGround;
            }

            predictionComplete.setOffset(0);
            player.getSetbackTeleportUtil().executeForceResync();
        }
    }

    private boolean isGhostBlock() {
        if (player.actualMovement.length() < 50) { // anti-crash
            // If the player entered a block, it is likely because of ghost blocks
            // TODO: There has to be a better way to write this anti-ghost block check
            // This entire anti ghost thing is terribly messy.
            // It constantly sees ghost blocks where they aren't any
            // It make it so stuff like vanilla Jesus doesn't flag and only setsback
            // and it makes the Phase check practically useless in terms of flagging
            //
            // One solution is to figure out all the possibilities where ghost blocks are created
            // Placing blocks, pistons, etc. and this isn't a terrible idea.
            Vector phase = Collisions.collide(player, player.actualMovement.getX(), player.actualMovement.getY(), player.actualMovement.getZ());
            if (phase.getX() != player.actualMovement.getX() || phase.getY() != player.actualMovement.getY() || phase.getZ() != player.actualMovement.getZ()) {
                return true;
            }
        }

        // Player is on glitchy block (1.8 client on anvil/wooden chest)
        if (player.uncertaintyHandler.isOrWasNearGlitchyBlock) {
            return true;
        }

        // Reliable way to check if the player is colliding vertically with a block that doesn't exist
        // Vehicles don't send on ground
        if ((player.inVehicle || player.clientClaimsLastOnGround) && player.clientControlledVerticalCollision && !player.onGround) {
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
        if (player.getClientVersion().isOlderThan(ClientVersion.V_1_9)) {
            SimpleCollisionBox largeExpandedBB = player.boundingBox.copy().expand(12, 0.5, 12);

            for (PacketEntity entity : player.compensatedEntities.entityMap.values()) {
                if (entity.type == EntityTypes.BOAT) {
                    if (entity.getPossibleCollisionBoxes().isIntersected(largeExpandedBB)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }
}
