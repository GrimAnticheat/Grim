package org.abyssmc.reaperac.checks.movement.predictions;

import org.abyssmc.reaperac.GrimPlayer;
import org.abyssmc.reaperac.utils.enums.MoverType;
import org.abyssmc.reaperac.utils.math.MovementVectorsCalc;
import org.abyssmc.reaperac.utils.nmsImplementations.Collisions;
import org.abyssmc.reaperac.utils.nmsImplementations.JumpPower;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class BasePrediction {
    public static Vector guessBestMovement(float f, GrimPlayer grimPlayer) {
        Player bukkitPlayer = grimPlayer.bukkitPlayer;
        double bestMovementGuess = Integer.MAX_VALUE;

        // Iterate the isJumping values - thanks StackOverflow!
        for (boolean isJumping = false, done = false; !done; done = isJumping, isJumping = true) {
            for (int movementX = -1; movementX <= 1; movementX++) {
                for (int movementZ = -1; movementZ <= 1; movementZ++) {
                    Vector clonedClientVelocity = grimPlayer.clientVelocity.clone();
                    double movementXWithShifting = movementX;
                    double movementZWithShifting = movementZ;

                    if (bukkitPlayer.isSneaking()) {
                        movementXWithShifting *= 0.3;
                        movementZWithShifting *= 0.3;
                    }

                    if (isJumping) {
                        clonedClientVelocity = JumpPower.jumpFromGround(grimPlayer);
                    }

                    Vector movementInput = MovementVectorsCalc.getInputVector(new Vector(movementXWithShifting * 0.98, 0, movementZWithShifting * 0.98), f, bukkitPlayer.getLocation().getYaw());
                    clonedClientVelocity.add(movementInput);
                    clonedClientVelocity = Collisions.collide(Collisions.maybeBackOffFromEdge(clonedClientVelocity, MoverType.SELF, grimPlayer), grimPlayer);

                    double closeness = grimPlayer.actualMovement.clone().subtract(clonedClientVelocity).lengthSquared();

                    if (closeness < bestMovementGuess) {
                        bestMovementGuess = closeness;
                        grimPlayer.bestX = movementXWithShifting * 0.98;
                        grimPlayer.bestZ = movementZWithShifting * 0.98;

                        grimPlayer.bestJumping = isJumping;
                        grimPlayer.bestMovement = clonedClientVelocity;
                    }
                }
            }
        }

        return grimPlayer.bestMovement;
    }
}
