package ac.grim.grimac.predictionengine.movementTick;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.data.VectorData;
import ac.grim.grimac.utils.enums.MoverType;
import ac.grim.grimac.utils.math.GrimMathHelper;
import ac.grim.grimac.utils.math.MovementVectorsCalc;
import ac.grim.grimac.utils.nmsImplementations.*;
import io.github.retrooper.packetevents.utils.player.ClientVersion;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;

public class MovementTicker {
    private static final Material slime = XMaterial.SLIME_BLOCK.parseMaterial();
    public final Player bukkitPlayer;
    public final GrimPlayer player;

    public MovementTicker(GrimPlayer player) {
        this.player = player;
        this.bukkitPlayer = player.bukkitPlayer;
    }

    public static Vector cutVectorsToPlayerMovement(Vector vectorToCutTo, Vector vectorOne, Vector vectorTwo) {
        double xMin = Math.min(vectorOne.getX(), vectorTwo.getX());
        double xMax = Math.max(vectorOne.getX(), vectorTwo.getX());
        double yMin = Math.min(vectorOne.getY(), vectorTwo.getY());
        double yMax = Math.max(vectorOne.getY(), vectorTwo.getY());
        double zMin = Math.min(vectorOne.getZ(), vectorTwo.getZ());
        double zMax = Math.max(vectorOne.getZ(), vectorTwo.getZ());

        Vector cutCloned = vectorToCutTo.clone();

        if (xMin > vectorToCutTo.getX() || xMax < vectorToCutTo.getX()) {
            if (Math.abs(vectorToCutTo.getX() - xMin) < Math.abs(vectorToCutTo.getX() - xMax)) {
                cutCloned.setX(xMin);
            } else {
                cutCloned.setX(xMax);
            }
        }

        if (yMin > vectorToCutTo.getY() || yMax < vectorToCutTo.getY()) {
            if (Math.abs(vectorToCutTo.getY() - yMin) < Math.abs(vectorToCutTo.getY() - yMax)) {
                cutCloned.setY(yMin);
            } else {
                cutCloned.setY(yMax);
            }
        }

        if (zMin > vectorToCutTo.getZ() || zMax < vectorToCutTo.getZ()) {
            if (Math.abs(vectorToCutTo.getZ() - zMin) < Math.abs(vectorToCutTo.getZ() - zMax)) {
                cutCloned.setZ(zMin);
            } else {
                cutCloned.setZ(zMax);
            }
        }

        return cutCloned;
    }

    public void move(MoverType moverType, Vector inputVel) {
        move(moverType, inputVel.multiply(player.stuckSpeedMultiplier), inputVel.multiply(player.stuckSpeedMultiplier));
    }

    public void livingEntityAIStep() {
        // Living Entity line 2153
        double minimumMovement = 0.003D;
        if (player.getClientVersion().isOlderThanOrEquals(ClientVersion.v_1_8))
            minimumMovement = 0.005D;

        for (VectorData vector : player.getPossibleVelocitiesMinusKnockback()) {
            if (Math.abs(vector.vector.getX()) < minimumMovement) {
                vector.vector.setX(0D);
            }

            if (Math.abs(vector.vector.getY()) < minimumMovement) {
                vector.vector.setY(0D);
            }

            if (Math.abs(vector.vector.getZ()) < minimumMovement) {
                vector.vector.setZ(0D);
            }
        }

        playerEntityTravel();
    }

    // Player line 1208
    public void playerEntityTravel() {
        if (player.specialFlying && player.bukkitPlayer.getVehicle() == null) {
            double oldY = player.clientVelocity.getY();
            double oldYJumping = oldY + player.flySpeed * 3;
            livingEntityTravel();

            if (player.predictedVelocity.hasVectorType(VectorData.VectorType.Knockback)) {
                player.baseTickSetY(player.actualMovement.getY() * 0.6);
            } else if (Math.abs(oldY - player.actualMovement.getY()) < (oldYJumping - player.actualMovement.getY())) {
                player.baseTickSetY(oldY * 0.6);
            } else {
                player.baseTickSetY(oldYJumping * 0.6);
            }

        } else {
            livingEntityTravel();
        }
    }

    // Entity line 527
    // TODO: Entity piston and entity shulker (want to) call this method too.
    public void move(MoverType moverType, Vector inputVel, Vector collide) {
        // Something about noClip
        // Piston movement exemption
        // What is a motion multiplier?

        if (player.stuckSpeedMultiplier.getX() < 0.99) {
            player.baseTickSetX(0);
            player.baseTickSetY(0);
            player.baseTickSetZ(0);
            player.clientVelocity = new Vector();
        }

        // Optimization - we run collisions before this occasionally so don't repeat them
        if (inputVel == collide) {
            // This is when client velocity is no longer referenced by inputVel
            if (!player.inVehicle) {
                inputVel = Collisions.maybeBackOffFromEdge(inputVel, moverType, player);
            }

            collide = Collisions.collide(player, inputVel.getX(), inputVel.getY(), inputVel.getZ());
        }

        // This is where vanilla moves the bounding box and sets it
        player.predictedVelocity = new VectorData(collide.clone(), player.predictedVelocity.lastVector, player.predictedVelocity.vectorType);

        player.horizontalCollision = !GrimMathHelper.equal(inputVel.getX(), collide.getX()) || !GrimMathHelper.equal(inputVel.getZ(), collide.getZ());
        player.verticalCollision = inputVel.getY() != collide.getY();
        player.isActuallyOnGround = player.verticalCollision && inputVel.getY() < 0.0D;

        Material onBlock = BlockProperties.getOnBlock(player, new Location(player.playerWorld, player.x, player.y, player.z));

        double xBeforeZero = player.clientVelocity.getX();
        if (inputVel.getX() != collide.getX()) {
            player.clientVelocity.setX(0);
        }

        // Strangely, collision on the Z axis resets X set to zero.  Is this a bug or a feature?  Doesn't matter.
        if (inputVel.getZ() != collide.getZ()) {
            player.clientVelocity.setX(xBeforeZero);
            player.clientVelocity.setZ(0);
        }

        if (inputVel.getY() != collide.getY()) {
            if (onBlock == slime && player.getClientVersion().isNewerThanOrEquals(ClientVersion.v_1_8)) {
                if (player.isSneaking) { // Slime blocks use shifting instead of sneaking
                    player.clientVelocity.setY(0);
                } else {
                    if (player.clientVelocity.getY() < 0.0) {
                        player.clientVelocity.setY(-player.clientVelocity.getY() * (player.inVehicle ? 0.8 : 1.0));
                    }
                }
            } else if (Materials.checkFlag(onBlock, Materials.BED) && player.getClientVersion().isNewerThanOrEquals(ClientVersion.v_1_12)) {
                if (player.clientVelocity.getY() < 0.0) {
                    player.clientVelocity.setY(-player.clientVelocity.getY() * 0.6600000262260437 * (player.inVehicle ? 0.8 : 1.0));
                }
            } else {
                player.clientVelocity.setY(0);
            }
        }

        // Warning: onGround changes every tick. Current implementation works fine with this vanilla feature.
        if (onBlock == slime && player.getClientVersion().isNewerThanOrEquals(ClientVersion.v_1_8)) {
            if ((player.inVehicle || player.onGround) && !player.isSneaking) {
                double absVelocityY = Math.abs(player.clientVelocity.getY());
                if (absVelocityY < 0.1) {
                    double d1 = 0.4D + absVelocityY * 0.2D;
                    player.clientVelocity.multiply(new Vector(d1, 1, d1));
                }
            }
        }

        player.clientVelocity.multiply(player.blockSpeedMultiplier);

        // Reset stuck speed so it can update
        player.lastStuckSpeedMultiplier = player.stuckSpeedMultiplier;
        player.stuckSpeedMultiplier = new Vector(1, 1, 1);

        Collisions.handleInsideBlocks(player);

        // Flying players are not affected by cobwebs/sweet berry bushes
        if (player.specialFlying) {
            player.stuckSpeedMultiplier = new Vector(1, 1, 1);
        }
    }

    public void doWaterMove(float swimSpeed, boolean isFalling, float swimFriction) {
    }

    public void doLavaMove() {
    }

    public void doNormalMove(float blockFriction) {
    }

    // LivingEntity line 1741
    public void livingEntityTravel() {
        double playerGravity = 0.08;

        boolean isFalling = player.clientVelocity.getY() <= 0.0;
        if (isFalling && player.slowFallingAmplifier > 0) {
            playerGravity = 0.01;
            //this.fallDistance = 0.0f;
        }

        player.gravity = playerGravity;

        float swimFriction;

        if (player.wasTouchingWater && !player.specialFlying) {
            // 0.8F seems hardcoded in
            swimFriction = player.isSprinting && player.getClientVersion().isNewerThanOrEquals(ClientVersion.v_1_13) ? 0.9F : 0.8F;
            float swimSpeed = 0.02F;

            if (player.depthStriderLevel > 3.0F) {
                player.depthStriderLevel = 3.0F;
            }

            if (!player.lastOnGround) {
                player.depthStriderLevel *= 0.5F;
            }

            if (player.depthStriderLevel > 0.0F) {
                swimFriction += (0.54600006F - swimFriction) * player.depthStriderLevel / 3.0F;
                swimSpeed += (player.movementSpeed - swimSpeed) * player.depthStriderLevel / 3.0F;
            }

            if (XMaterial.supports(13) && player.dolphinsGraceAmplifier > 0) {
                swimFriction = 0.96F;
            }

            doWaterMove(swimSpeed, isFalling, swimFriction);

            // 1.12 and below players can't climb ladders while touching water
            if (player.getClientVersion().isNewerThanOrEquals(ClientVersion.v_1_13) && player.isClimbing) {
                player.clientVelocityOnLadder = FluidFallingAdjustedMovement.getFluidFallingAdjustedMovement(player, playerGravity, isFalling, player.clientVelocity.clone().setY(0.16));
            }

        } else {
            if (player.wasTouchingLava && !player.specialFlying && !canStandOnLava()) {

                doLavaMove();

                // Unsure which client version that lava movement changed but it's most likely 1.13
                if (player.getClientVersion().isNewerThanOrEquals(ClientVersion.v_1_13) && player.slightlyTouchingLava) {
                    player.clientVelocity = player.clientVelocity.multiply(new Vector(0.5D, 0.800000011920929D, 0.5D));
                    player.clientVelocity = FluidFallingAdjustedMovement.getFluidFallingAdjustedMovement(player, playerGravity, isFalling, player.clientVelocity);
                } else {
                    player.clientVelocity.multiply(0.5D);
                }

                // Removed reference to gravity
                player.clientVelocity.add(new Vector(0.0D, -playerGravity / 4.0D, 0.0D));

            } else if (player.isGliding) {
                Vector currentLook = MovementVectorsCalc.getVectorForRotation(player, player.yRot, player.xRot);
                Vector lastLook = MovementVectorsCalc.getVectorForRotation(player, player.lastYRot, player.lastXRot);

                // Tick order of player movements vs firework isn't constant
                int maxFireworks = player.compensatedFireworks.getMaxFireworksAppliedPossible() * 2;

                Set<Vector> possibleVelocities = new HashSet<>();

                // Vector 1: All possible fireworks * 2 on the past look vector
                // Vector 2: All possible fireworks * 2 on the next look vector
                // Vector 3: No fireworks at all for the first look vector
                // Vector 4: No fireworks at all for the second look vector
                //
                // The client's velocity clone is then forced to be between vector 1 and 3
                // The client's velocity clone is then forced to be between vector 2 and 4
                //
                // The closest of these two vector clones are the predicted velocity.
                for (VectorData possibleVelocity : player.getPossibleVelocities()) {
                    if (maxFireworks > 0) {
                        Vector boostOne = possibleVelocity.vector.clone();
                        Vector boostTwo = possibleVelocity.vector.clone();

                        Vector noFireworksOne = getElytraMovement(boostOne.clone(), currentLook).multiply(player.stuckSpeedMultiplier).multiply(new Vector(0.99, 0.98, 0.99));
                        Vector noFireworksTwo = getElytraMovement(boostTwo.clone(), lastLook).multiply(player.stuckSpeedMultiplier).multiply(new Vector(0.99, 0.98, 0.99));

                        for (int i = 0; i < maxFireworks; i++) {
                            boostOne.add(new Vector(currentLook.getX() * 0.1 + (currentLook.getX() * 1.5 - boostOne.getX()) * 0.5, currentLook.getY() * 0.1 + (currentLook.getY() * 1.5 - boostOne.getY()) * 0.5, (currentLook.getZ() * 0.1 + (currentLook.getZ() * 1.5 - boostOne.getZ()) * 0.5)));
                            boostTwo.add(new Vector(lastLook.getX() * 0.1 + (lastLook.getX() * 1.5 - boostTwo.getX()) * 0.5, lastLook.getY() * 0.1 + (lastLook.getY() * 1.5 - boostTwo.getY()) * 0.5, (lastLook.getZ() * 0.1 + (lastLook.getZ() * 1.5 - boostTwo.getZ()) * 0.5)));
                        }

                        getElytraMovement(boostOne, currentLook).multiply(player.stuckSpeedMultiplier).multiply(new Vector(0.99, 0.98, 0.99));
                        getElytraMovement(boostTwo, lastLook).multiply(player.stuckSpeedMultiplier).multiply(new Vector(0.99, 0.98, 0.99));

                        Vector cutOne = cutVectorsToPlayerMovement(player.actualMovement, boostOne, noFireworksTwo);
                        Vector cutTwo = cutVectorsToPlayerMovement(player.actualMovement, boostTwo, noFireworksOne);
                        Vector cutCombined = cutVectorsToPlayerMovement(player.actualMovement, cutOne, cutTwo);

                        possibleVelocities.add(cutCombined);

                    } else {
                        Vector noFireworksOne = getElytraMovement(possibleVelocity.vector.clone(), currentLook).multiply(player.stuckSpeedMultiplier).multiply(new Vector(0.99, 0.98, 0.99));
                        Vector noFireworksTwo = getElytraMovement(possibleVelocity.vector.clone(), lastLook).multiply(player.stuckSpeedMultiplier).multiply(new Vector(0.99, 0.98, 0.99));

                        if (noFireworksOne.distanceSquared(player.actualMovement) < noFireworksTwo.distanceSquared(player.actualMovement)) {
                            possibleVelocities.add(noFireworksOne);
                        } else {
                            possibleVelocities.add(noFireworksTwo);
                        }
                    }
                }


                double bestInput = Double.MAX_VALUE;
                Vector bestCollisionVel = null;

                for (Vector clientVelAfterInput : possibleVelocities) {
                    Vector backOff = Collisions.maybeBackOffFromEdge(clientVelAfterInput, MoverType.SELF, player);
                    Vector outputVel = Collisions.collide(player, backOff.getX(), backOff.getY(), backOff.getZ());
                    double resultAccuracy = outputVel.distance(player.actualMovement);

                    if (resultAccuracy < bestInput) {
                        bestInput = resultAccuracy;
                        player.clientVelocity = backOff.clone();
                        bestCollisionVel = outputVel.clone();

                        // Optimization - Close enough, other inputs won't get closer
                        if (resultAccuracy < 0.01) break;
                    }
                }

                new MovementTickerPlayer(player).move(MoverType.SELF, player.clientVelocity, bestCollisionVel);

            } else {
                float blockFriction = BlockProperties.getBlockFrictionUnderPlayer(player);
                player.friction = player.lastOnGround ? blockFriction * 0.91f : 0.91f;

                doNormalMove(blockFriction);
            }
        }

    }

    public Vector getElytraMovement(Vector vector, Vector lookVector) {
        float yRotRadians = player.yRot * 0.017453292F;
        double d2 = Math.sqrt(lookVector.getX() * lookVector.getX() + lookVector.getZ() * lookVector.getZ());
        double d3 = vector.clone().setY(0).length();
        double d4 = lookVector.length();
        float f3 = player.trigHandler.cos(yRotRadians);
        f3 = (float) ((double) f3 * (double) f3 * Math.min(1.0D, d4 / 0.4D));
        vector.add(new Vector(0.0D, player.gravity * (-1.0D + (double) f3 * 0.75D), 0.0D));
        double d5;
        if (vector.getY() < 0.0D && d2 > 0.0D) {
            d5 = vector.getY() * -0.1D * (double) f3;
            vector.add(new Vector(lookVector.getX() * d5 / d2, d5, lookVector.getZ() * d5 / d2));
        }

        if (yRotRadians < 0.0F && d2 > 0.0D) {
            d5 = d3 * (double) (-player.trigHandler.sin(yRotRadians)) * 0.04D;
            vector.add(new Vector(-lookVector.getX() * d5 / d2, d5 * 3.2D, -lookVector.getZ() * d5 / d2));
        }

        if (d2 > 0) {
            vector.add(new Vector((lookVector.getX() / d2 * d3 - vector.getX()) * 0.1D, 0.0D, (lookVector.getZ() / d2 * d3 - vector.getZ()) * 0.1D));
        }

        return vector;
    }

    public boolean canStandOnLava() {
        return false;
    }
}