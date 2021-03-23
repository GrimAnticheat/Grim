package org.abyssmc.reaperac.checks.movement.predictions;

import org.abyssmc.reaperac.GrimPlayer;
import org.abyssmc.reaperac.utils.enums.MoverType;
import org.abyssmc.reaperac.utils.math.Mth;
import org.abyssmc.reaperac.utils.nmsImplementations.Collisions;
import org.abyssmc.reaperac.utils.nmsImplementations.JumpPower;
import org.bukkit.Material;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

public class PredictionEngineTwo {
    public static Vector guessBestMovement(float f, GrimPlayer grimPlayer) {
        List<Vector> possibleInputs = getPossiblePlayerInputs(f, grimPlayer.bukkitPlayer.getLocation().getYaw(), grimPlayer);

        double bestMovementGuess = Double.MAX_VALUE;

        Vector jumpingAdditionalMovement = JumpPower.jumpFromGround(grimPlayer);

        // TODO: This bug is caused by an input with velocity being added to the jumping, which sets this input to 0
        // TODO: Optimize the isJumping method to try and guess whether the player is jumping
        for (boolean collide = false, done1 = false; !done1; done1 = collide, collide = true) {
            for (boolean isJumping = false, done2 = false; !done2; done2 = isJumping, isJumping = true) {
                for (Vector lastOutputs : grimPlayer.getPossibleVelocities()) {
                    for (Vector vector : possibleInputs) {
                        Vector movementAddition = lastOutputs.clone().add(vector);

                        // LivingEntity line 1873 - handling on ladder movement
                        // handleOnClimbable is on line 1890 in LivingEntity
                        if (grimPlayer.lastClimbing) {
                            movementAddition.setX(Mth.clamp(movementAddition.getX(), -0.15000000596046448, 0.15000000596046448));
                            movementAddition.setZ(Mth.clamp(movementAddition.getZ(), -0.15000000596046448, 0.15000000596046448));
                            movementAddition.setY(Math.max(movementAddition.getY(), -0.15000000596046448));

                            if (movementAddition.getY() < 0.0 && !grimPlayer.bukkitPlayer.getWorld().getBlockAt(grimPlayer.bukkitPlayer.getLocation()).getType().equals(Material.SCAFFOLDING) && grimPlayer.bukkitPlayer.isSneaking()) {
                                movementAddition.setY(0.0);
                            }
                        }

                        if (collide) {
                            movementAddition = Collisions.collide(Collisions.maybeBackOffFromEdge(movementAddition, MoverType.SELF, grimPlayer), grimPlayer);
                        }

                        if (isJumping) {
                            movementAddition.add(jumpingAdditionalMovement);
                        }

                        double closeness = grimPlayer.actualMovement.clone().subtract(movementAddition).lengthSquared();

                        if (closeness < bestMovementGuess) {
                            bestMovementGuess = closeness;
                            grimPlayer.bestInputResult = movementAddition;
                            grimPlayer.bestPreviousVelocity = lastOutputs;

                            // debug
                            int element = possibleInputs.indexOf(vector);
                            int x = element % 3 - 1;
                            int z = element / 3 - 1;
                            grimPlayer.bestInputs = new Vector(x, 0, z);

                            if (closeness < 0.001) {
                                Vector withCollisions = Collisions.collide(Collisions.maybeBackOffFromEdge(grimPlayer.bestInputResult, MoverType.SELF, grimPlayer), grimPlayer);
                                if (grimPlayer.actualMovement.clone().subtract(withCollisions).lengthSquared() < 0.001) {
                                    grimPlayer.possibleKnockback.remove(grimPlayer.bestPreviousVelocity);
                                    return withCollisions;
                                }
                            }
                        }
                    }
                }
            }
        }

        // TODO: Make this less of a hack
        grimPlayer.possibleKnockback.remove(grimPlayer.bestPreviousVelocity);
        return Collisions.collide(Collisions.maybeBackOffFromEdge(grimPlayer.bestInputResult, MoverType.SELF, grimPlayer), grimPlayer);
    }


    public static List<Vector> getPossiblePlayerInputs(float f, float f2, GrimPlayer grimPlayer) {
        List<Vector> possibleMovements = new ArrayList<>();

        // 1 means go in the X direction
        float f3 = Mth.sin(f2 * 0.017453292f);
        // 1 means go in the Z direction
        float f4 = Mth.cos(f2 * 0.017453292f);

        double movementXWithShifting;
        double movementZWithShifting;

        for (int x = -1; x <= 1; x += 1) {
            for (double z = -1; z <= 1; z += 1) {
                movementXWithShifting = x * 0.98;
                movementZWithShifting = z * 0.98;

                // TODO: Multiply movement by 0.2 if the player is eating an item
                // event.getItem().getType().isEdible()

                if (grimPlayer.lastSneaking) {
                    movementXWithShifting *= 0.3;
                    movementZWithShifting *= 0.3;
                }


                Vector vec3 = new Vector(movementXWithShifting, 0, movementZWithShifting);

                if (movementXWithShifting == 0 && movementZWithShifting == 0) {
                    possibleMovements.add(new Vector());
                    continue;
                }

                double d = vec3.lengthSquared();
                Vector vec32 = (d > 1.0 ? vec3.normalize() : vec3).multiply(f);

                double xCloseness = vec32.getX() * f4 - vec32.getZ() * f3;
                double zCloseness = vec32.getZ() * f4 + vec32.getX() * f3;
                possibleMovements.add(new Vector(xCloseness, 0, zCloseness));
            }
        }

        return possibleMovements;
    }
}
