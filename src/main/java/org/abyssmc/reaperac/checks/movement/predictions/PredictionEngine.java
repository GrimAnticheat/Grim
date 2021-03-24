package org.abyssmc.reaperac.checks.movement.predictions;

import org.abyssmc.reaperac.GrimPlayer;
import org.abyssmc.reaperac.utils.enums.FluidTag;
import org.abyssmc.reaperac.utils.enums.MoverType;
import org.abyssmc.reaperac.utils.math.Mth;
import org.abyssmc.reaperac.utils.nmsImplementations.Collisions;
import org.abyssmc.reaperac.utils.nmsImplementations.JumpPower;
import org.bukkit.Bukkit;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

public class PredictionEngine {
    public Vector guessBestMovement(float f, GrimPlayer grimPlayer) {
        List<Vector> possibleInputs = getPossiblePlayerInputs(f, grimPlayer.bukkitPlayer.getLocation().getYaw(), grimPlayer);

        double bestMovementGuess = Double.MAX_VALUE;

        Vector jumpingAdditionalMovement = JumpPower.jumpFromGround(grimPlayer);
        Bukkit.broadcastMessage("Additional jumping movement " + jumpingAdditionalMovement);

        // TODO: This bug is caused by an input with velocity being added to the jumping, which sets this input to 0
        // TODO: Optimize the isJumping method to try and guess whether the player is jumping
        for (boolean collide = false, done1 = false; !done1; done1 = collide, collide = true) {
            for (boolean isJumping = false, done2 = false; !done2; done2 = isJumping, isJumping = true) {
                for (Vector lastOutputs : grimPlayer.getPossibleVelocities()) {
                    for (Vector vector : possibleInputs) {
                        Vector movementAddition = lastOutputs.clone().add(vector);

                        // LivingEntity line 1873 - handling on ladder movement
                        // handleOnClimbable is on line 1890 in LivingEntity
                        movementAddition = this.handleOnClimbable(movementAddition, grimPlayer);

                        if (collide) {
                            movementAddition = Collisions.collide(Collisions.maybeBackOffFromEdge(movementAddition, MoverType.SELF, grimPlayer), grimPlayer);
                        }

                        if (isJumping) {
                            // LivingEntity line 2185
                            // TODO: Add an anti-jump spam check (no jumping faster than once every 10 ticks)
                            // Not sure whether jumping too fast is a cheat... but eventually it will
                            if (!grimPlayer.entityPlayer.abilities.isFlying) {
                                double d7 = grimPlayer.fluidHeight.getDouble(FluidTag.LAVA) > 0 ? grimPlayer.fluidHeight.getDouble(FluidTag.LAVA) : grimPlayer.fluidHeight.getDouble(FluidTag.WATER);
                                boolean bl = grimPlayer.fluidHeight.getDouble(FluidTag.LAVA) > 0 && d7 > 0.0;
                                final double d8 = 0.4;
                                if (bl && (!grimPlayer.lastOnGround || d7 > d8)) {
                                    movementAddition.add(new Vector(0, 0.04, 0));
                                } else if (grimPlayer.fluidHeight.getDouble(FluidTag.LAVA) > 0 && (!grimPlayer.lastOnGround || d7 > d8)) {
                                    movementAddition.add(new Vector(0, 0.04, 0));
                                } else if ((grimPlayer.lastOnGround || bl && d7 <= d8) /*&& this.noJumpDelay == 0*/) {
                                    movementAddition.add(jumpingAdditionalMovement);
                                    //this.noJumpDelay = 10;
                                }
                            } else {
                                // LocalPlayer line 747
                                // PlayerBaseTick handles shifting, since we know when the player shifts but not jumps
                                movementAddition.add(new Vector(0, grimPlayer.entityPlayer.abilities.flySpeed * 3.0f, 0));
                            }
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

                                    Bukkit.broadcastMessage("Best inputs " + grimPlayer.bestInputs);
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
        Bukkit.broadcastMessage("Best inputs " + grimPlayer.bestInputs);
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

                if (grimPlayer.lastSneaking && !grimPlayer.entityPlayer.abilities.isFlying) {
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

    public Vector handleOnClimbable(Vector vector, GrimPlayer grimPlayer) {
        return vector;
    }
}
