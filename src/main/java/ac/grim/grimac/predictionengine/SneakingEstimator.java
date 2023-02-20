package ac.grim.grimac.predictionengine;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.type.PostPredictionCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.predictionengine.predictions.PredictionEngine;
import ac.grim.grimac.utils.anticheat.update.PredictionComplete;
import ac.grim.grimac.utils.collisions.datatypes.SimpleCollisionBox;
import ac.grim.grimac.utils.data.VectorData;

import java.util.ArrayList;
import java.util.List;

/**
 * ┌──────────────► 1.14 players leave sneaking two ticks
 * │                after they jump off the ground
 * │
 * │
 * ├──────────────► 1.8 players leave sneaking the tick after
 * │                when they jump off of the ground
 * │
 * <p>
 * Additionally, sneaking does NOT reset the amount of velocity a player gets from moving
 * This means that they accumulate velocity when sneaking against the edge
 * <p>
 * 1.14 players have sneaking slowdown delayed by 2 (!)(?) ticks fucking up any uncertainty quite badly
 * 1.8 players have sneaking slowdown applied immediately
 * <p>
 * Now 1.14 players having this delay isn't a big deal, although it makes god bridging painfully annoying
 * But without the idle packet, this kills predictions.  Thanks for this stupidity, Mojang.
 * <p>
 * So, this is a value patch like 0.03 because it can be "close enough" that it's better just to not skip ticks
 **/
public class SneakingEstimator extends Check implements PostPredictionCheck {
    SimpleCollisionBox sneakingPotentialHiddenVelocity = new SimpleCollisionBox();
    List<VectorData> possible = new ArrayList<>();

    public SneakingEstimator(GrimPlayer player) {
        super(player);
    }

    public void storePossibleVelocities(List<VectorData> possible) {
        this.possible = possible;
    }

    public SimpleCollisionBox getSneakingPotentialHiddenVelocity() {
        return sneakingPotentialHiddenVelocity;
    }

    @Override
    public void onPredictionComplete(final PredictionComplete predictionComplete) {
        if (!predictionComplete.isChecked()) return;

        double trueFriction = player.lastOnGround ? player.friction * 0.91 : 0.91;
        if (player.wasTouchingLava) trueFriction = 0.5;
        if (player.wasTouchingWater) trueFriction = 0.96;
        if (player.isGliding) trueFriction = 0.99;

        // START HACKERY

        // Avoid calling the method if the player isn't sneaking
        if (!player.uncertaintyHandler.stuckOnEdge.hasOccurredSince(0)) {
            sneakingPotentialHiddenVelocity = new SimpleCollisionBox();
            return;
        }

        for (VectorData data : possible) {
            // Don't let the player always get jumping bonus, for example
            if (data.isJump() == player.predictedVelocity.isJump() && data.isKnockback() == player.predictedVelocity.isKnockback() && data.isExplosion() == player.predictedVelocity.isExplosion()) {
                // Fuck, we are compounding this which is very dangerous. After light testing seems fine... can we do better?
                if (player.uncertaintyHandler.lastStuckWest.hasOccurredSince(0) || player.uncertaintyHandler.lastStuckNorth.hasOccurredSince(0)) {
                    sneakingPotentialHiddenVelocity.minX = Math.min(sneakingPotentialHiddenVelocity.minX, data.vector.getX());
                    sneakingPotentialHiddenVelocity.minZ = Math.min(sneakingPotentialHiddenVelocity.minZ, data.vector.getZ());
                }

                if (player.uncertaintyHandler.lastStuckEast.hasOccurredSince(0) || player.uncertaintyHandler.lastStuckSouth.hasOccurredSince(0)) {
                    sneakingPotentialHiddenVelocity.maxX = Math.max(sneakingPotentialHiddenVelocity.maxX, data.vector.getX());
                    sneakingPotentialHiddenVelocity.maxZ = Math.max(sneakingPotentialHiddenVelocity.maxZ, data.vector.getZ());
                }
            }
        }
        // END HACKERY


        sneakingPotentialHiddenVelocity.minX *= trueFriction;
        sneakingPotentialHiddenVelocity.minZ *= trueFriction;
        sneakingPotentialHiddenVelocity.maxX *= trueFriction;
        sneakingPotentialHiddenVelocity.maxZ *= trueFriction;

        sneakingPotentialHiddenVelocity.minX = Math.min(-0.15, sneakingPotentialHiddenVelocity.minX);
        sneakingPotentialHiddenVelocity.minZ = Math.min(-0.15, sneakingPotentialHiddenVelocity.minZ);
        sneakingPotentialHiddenVelocity.maxX = Math.max(0.15, sneakingPotentialHiddenVelocity.maxX);
        sneakingPotentialHiddenVelocity.maxZ = Math.max(0.15, sneakingPotentialHiddenVelocity.maxZ);

        // Now we just have to handle reducing this velocity over ticks so we know when it's being abused
        if (!player.uncertaintyHandler.lastStuckEast.hasOccurredSince(0)) {
            sneakingPotentialHiddenVelocity.maxX = 0;
        }
        if (!player.uncertaintyHandler.lastStuckWest.hasOccurredSince(0)) {
            sneakingPotentialHiddenVelocity.minX = 0;
        }
        if (!player.uncertaintyHandler.lastStuckNorth.hasOccurredSince(0)) {
            sneakingPotentialHiddenVelocity.minZ = 0;
        }
        if (!player.uncertaintyHandler.lastStuckSouth.hasOccurredSince(0)) {
            sneakingPotentialHiddenVelocity.maxZ = 0;
        }
    }
}
