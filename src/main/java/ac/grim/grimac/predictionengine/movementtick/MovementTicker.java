package ac.grim.grimac.predictionengine.movementtick;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.predictionengine.PlayerBaseTick;
import ac.grim.grimac.predictionengine.predictions.PredictionEngine;
import ac.grim.grimac.predictionengine.predictions.PredictionEngineElytra;
import ac.grim.grimac.utils.collisions.datatypes.SimpleCollisionBox;
import ac.grim.grimac.utils.data.ReachInterpolationData;
import ac.grim.grimac.utils.data.VectorData;
import ac.grim.grimac.utils.data.packetentity.PacketEntity;
import ac.grim.grimac.utils.data.packetentity.PacketEntityStrider;
import ac.grim.grimac.utils.enums.EntityType;
import ac.grim.grimac.utils.math.GrimMath;
import ac.grim.grimac.utils.nmsImplementations.*;
import io.github.retrooper.packetevents.utils.player.ClientVersion;
import io.github.retrooper.packetevents.utils.vector.Vector3d;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class MovementTicker {
    private static final Material SLIME_BLOCK = XMaterial.SLIME_BLOCK.parseMaterial();
    private static final Material HONEY_BLOCK = XMaterial.HONEY_BLOCK.parseMaterial();

    public final Player bukkitPlayer;
    public final GrimPlayer player;

    public MovementTicker(GrimPlayer player) {
        this.player = player;
        this.bukkitPlayer = player.bukkitPlayer;
    }

    public void move(Vector inputVel, Vector collide) {
        if (player.stuckSpeedMultiplier.getX() < 0.99) {
            player.clientVelocity = new Vector();
        }

        Material onBlock = BlockProperties.getOnBlock(player, player.x, player.y, player.z);

        if (inputVel.getX() != collide.getX()) {
            player.clientVelocity.setX(0);
        }

        if (inputVel.getZ() != collide.getZ()) {
            player.clientVelocity.setZ(0);
        }

        player.horizontalCollision = !GrimMath.equal(inputVel.getX(), collide.getX()) || !GrimMath.equal(inputVel.getZ(), collide.getZ());
        player.verticalCollision = inputVel.getY() != collide.getY();

        // Avoid order of collisions being wrong because 0.03 movements
        // Stepping movement USUALLY means the vehicle in on the ground as vehicles can't jump
        // Can be wrong with swim hopping into step, but this is rare and difficult to pull off
        // and would require a huge rewrite to support this rare edge case
        player.isActuallyOnGround = (player.verticalCollision && inputVel.getY() < 0.0D)
                || (player.inVehicle && player.uncertaintyHandler.isStepMovement);
        // If the player is on the ground with a y velocity of 0, let the player decide (too close to call)
        if (inputVel.getY() == -SimpleCollisionBox.COLLISION_EPSILON && collide.getY() > -SimpleCollisionBox.COLLISION_EPSILON && collide.getY() <= 0)
            player.isActuallyOnGround = player.onGround;
        player.clientClaimsLastOnGround = player.onGround;

        // We can't tell the difference between stepping and swim hopping, so just let the player's onGround status be the truth
        // Pistons/shulkers are a bit glitchy so just trust the client when they are affected by them
        // The player's onGround status isn't given when riding a vehicle, so we don't have a choice in whether we calculate or not
        //
        // Trust the onGround status if the player is near the ground and they sent a ground packet
        if (player.inVehicle || !player.exemptOnGround()) {
            player.onGround = player.isActuallyOnGround;
        }

        // This is around the place where the new bounding box gets set
        player.boundingBox = GetBoundingBox.getCollisionBoxForPlayer(player, player.x, player.y, player.z);
        // This is how the player checks for fall damage
        // By running fluid pushing for the player
        if (!player.wasTouchingWater && (player.playerVehicle == null || player.playerVehicle.type != EntityType.BOAT)) {
            new PlayerBaseTick(player).updateInWaterStateAndDoWaterCurrentPushing();
        }

        if (player.onGround) {
            player.fallDistance = 0;
        } else if (collide.getY() < 0) {
            player.fallDistance = (player.fallDistance) - collide.getY();
        }

        // Striders call the method for inside blocks AGAIN!
        if (player.playerVehicle instanceof PacketEntityStrider) {
            Collisions.handleInsideBlocks(player);
        }

        if (inputVel.getY() != collide.getY()) {
            // If the client supports slime blocks
            // And the block is a slime block
            // Or the block is honey and was replaced by viaversion
            if (player.getClientVersion().isNewerThanOrEquals(ClientVersion.v_1_8)
                    && (onBlock == SLIME_BLOCK || (onBlock == HONEY_BLOCK && player.getClientVersion().isOlderThanOrEquals(ClientVersion.v_1_14_4)))) {
                if (player.isSneaking) { // Slime blocks use shifting instead of sneaking
                    player.clientVelocity.setY(0);
                } else {
                    if (player.clientVelocity.getY() < 0.0) {
                        player.clientVelocity.setY(-player.clientVelocity.getY() *
                                (player.playerVehicle != null && !EntityType.isLivingEntity(player.playerVehicle.bukkitEntityType) ? 0.8 : 1.0));
                    }
                }
            } else if (Materials.checkFlag(onBlock, Materials.BED) && player.getClientVersion().isNewerThanOrEquals(ClientVersion.v_1_12)) {
                if (player.clientVelocity.getY() < 0.0) {
                    player.clientVelocity.setY(-player.clientVelocity.getY() * 0.6600000262260437 *
                            (player.playerVehicle != null && !EntityType.isLivingEntity(player.playerVehicle.bukkitEntityType) ? 0.8 : 1.0));
                }
            } else {
                player.clientVelocity.setY(0);
            }
        }

        collide = PredictionEngine.clampMovementToHardBorder(player, collide, collide);

        // The game disregards movements smaller than 1e-7 (such as in boats)
        if (collide.lengthSquared() < 1e-7) {
            collide = new Vector();
        }

        // This is where vanilla moves the bounding box and sets it
        player.predictedVelocity = new VectorData(collide.clone(), player.predictedVelocity.lastVector, player.predictedVelocity.vectorType);

        player.clientVelocity.multiply(player.blockSpeedMultiplier);

        // Reset stuck speed so it can update
        player.uncertaintyHandler.lastStuckSpeedMultiplier--;
        if (player.stuckSpeedMultiplier.getX() < 0.99) {
            player.uncertaintyHandler.lastStuckSpeedMultiplier = 0;
        }

        player.stuckSpeedMultiplier = new Vector(1, 1, 1);

        // 1.15 and older clients use the handleInsideBlocks method for lava
        if (player.getClientVersion().isOlderThan(ClientVersion.v_1_16))
            player.wasTouchingLava = false;

        Collisions.handleInsideBlocks(player);

        if (player.stuckSpeedMultiplier.getX() < 0.9) {
            // Reset fall distance if stuck in block
            player.fallDistance = 0;
        }

        // Flying players are not affected by cobwebs/sweet berry bushes
        if (player.specialFlying) {
            player.stuckSpeedMultiplier = new Vector(1, 1, 1);
        }
    }

    public void livingEntityAIStep() {
        if (player.playerVehicle == null) {
            playerEntityTravel();
        } else {
            livingEntityTravel();
        }

        player.uncertaintyHandler.xNegativeUncertainty = 0;
        player.uncertaintyHandler.xPositiveUncertainty = 0;
        player.uncertaintyHandler.yNegativeUncertainty = 0;
        player.uncertaintyHandler.yPositiveUncertainty = 0;
        player.uncertaintyHandler.zNegativeUncertainty = 0;
        player.uncertaintyHandler.zPositiveUncertainty = 0;

        if (player.isFlying) {
            SimpleCollisionBox playerBox = GetBoundingBox.getCollisionBoxForPlayer(player, player.lastX, player.lastY, player.lastZ);
            if (!Collisions.isEmpty(player, playerBox.copy().offset(0, 0.1, 0))) {
                player.uncertaintyHandler.yPositiveUncertainty = player.flySpeed * 5;
            }

            if (!Collisions.isEmpty(player, playerBox.copy().offset(0, -0.1, 0))) {
                player.uncertaintyHandler.yNegativeUncertainty = player.flySpeed * -5;
            }
        }

        // 1.7 and 1.8 do not have player collision
        if (player.getClientVersion().isOlderThanOrEquals(ClientVersion.v_1_8))
            return;

        int possibleCollidingEntities = 0;

        // Players in vehicles do not have collisions
        if (!player.inVehicle) {
            // Calculate the offset of the player to colliding other stuff
            Vector3d playerPos = new Vector3d(player.x, player.y, player.z);
            SimpleCollisionBox playerBox = GetBoundingBox.getCollisionBoxForPlayer(player, player.x, player.y, player.z);
            SimpleCollisionBox expandedPlayerBox = playerBox.copy().expand(1);

            for (PacketEntity entity : player.compensatedEntities.entityMap.values()) {
                if (entity.position.distanceSquared(playerPos) < 12) {
                    // Players can only push living entities
                    // Players can also push boats or minecarts
                    // The one exemption to a living entity is an armor stand
                    if ((!EntityType.isLivingEntity(entity.bukkitEntityType) && entity.type != EntityType.BOAT && !EntityType.isMinecart(entity.type)) || entity.type == EntityType.ARMOR_STAND)
                        continue;

                    double width = BoundingBoxSize.getWidth(entity);
                    double height = BoundingBoxSize.getHeight(entity);

                    SimpleCollisionBox entityBox = ReachInterpolationData.combineCollisionBox(
                            GetBoundingBox.getBoundingBoxFromPosAndSize(entity.position.getX(), entity.position.getY(), entity.position.getZ(), width, height),
                            GetBoundingBox.getBoundingBoxFromPosAndSize(entity.lastTickPosition.getX(), entity.lastTickPosition.getY(), entity.lastTickPosition.getZ(), width, height));

                    if (expandedPlayerBox.isCollided(entityBox))
                        possibleCollidingEntities++;

                    if (!playerBox.isCollided(entityBox))
                        continue;

                    double xDist = player.x - entity.position.x;
                    double zDist = player.z - entity.position.z;
                    double maxLength = Math.max(Math.abs(xDist), Math.abs(zDist));
                    if (maxLength >= 0.01) {
                        maxLength = Math.sqrt(maxLength);
                        xDist /= maxLength;
                        zDist /= maxLength;

                        double d3 = 1.0D / maxLength;
                        d3 = Math.min(d3, 1.0);

                        xDist *= d3;
                        zDist *= d3;
                        xDist *= -0.05F;
                        zDist *= -0.05F;

                        if (xDist > 0) {
                            player.uncertaintyHandler.xNegativeUncertainty += xDist;
                        } else {
                            player.uncertaintyHandler.zNegativeUncertainty += xDist;
                        }

                        if (zDist > 0) {
                            player.uncertaintyHandler.xPositiveUncertainty += zDist;
                        } else {
                            player.uncertaintyHandler.zPositiveUncertainty += zDist;
                        }
                    }
                }
            }
        }

        player.uncertaintyHandler.collidingEntities.add(possibleCollidingEntities);

        // Work around a bug introduced in 1.14 where a player colliding with an X and Z wall maintains X momentum
        if (player.getClientVersion().isOlderThan(ClientVersion.v_1_14))
            return;

        boolean xAxisPositiveCollision = !Collisions.isEmpty(player, player.boundingBox.copy().expand(player.clientVelocity.getX(), 0, player.clientVelocity.getZ()).expand(0, -0.01, -0.01).expandMax(player.speed, 0, 0));
        boolean xAxisNegativeCollision = !Collisions.isEmpty(player, player.boundingBox.copy().expand(player.clientVelocity.getX(), 0, player.clientVelocity.getZ()).expand(0, -0.01, -0.01).expandMin(-player.speed, 0, 0));
        boolean zAxisCollision = !Collisions.isEmpty(player, player.boundingBox.copy().expand(player.clientVelocity.getX(), 0, player.clientVelocity.getZ()).expand(-0.01, -0.01, player.speed));

        // Stupid game!  It thinks you are colliding on the Z axis when your Z movement is below 1e-7
        // (This code is rounding the small movements causing this bug)
        // if (Math.abs(p_2124373) < 1.0E-7D) {
        //     return 0.0D;
        // }
        //
        // While there likely is a better implementation to detect this, have fun with fastmath!
        //
        // This combines with the XZ axis bug to create some strange behavior
        zAxisCollision = zAxisCollision || player.actualMovement.getZ() == 0;

        // Technically we should only give uncertainty on the axis of which this occurs
        // Unfortunately, for some reason, riding entities break this.
        //
        // Also use magic value for gliding, as gliding isn't typical player movement
        if (zAxisCollision && (xAxisPositiveCollision || xAxisNegativeCollision)) {
            double playerSpeed = player.speed;

            if (player.wasTouchingWater) {
                float swimSpeed = 0.02F;
                if (player.depthStriderLevel > 0.0F) {
                    swimSpeed += (player.speed - swimSpeed) * player.depthStriderLevel / 3.0F;
                }
                playerSpeed = swimSpeed;
            } else if (player.wasTouchingLava) {
                playerSpeed = 0.02F;
            } else if (player.isGliding) {
                playerSpeed = 0.4;
            }

            player.uncertaintyHandler.xNegativeUncertainty -= playerSpeed * 4;
            player.uncertaintyHandler.xPositiveUncertainty += playerSpeed * 4;
        }
    }

    public void playerEntityTravel() {
        if (player.specialFlying && player.playerVehicle == null) {
            double oldY = player.clientVelocity.getY();
            double oldYJumping = oldY + player.flySpeed * 3;
            livingEntityTravel();

            if (player.predictedVelocity.isKnockback() || player.predictedVelocity.isTrident()
                    || player.uncertaintyHandler.yPositiveUncertainty != 0 || player.uncertaintyHandler.yNegativeUncertainty != 0 || player.isGliding) {
                player.clientVelocity.setY(player.actualMovement.getY() * 0.6);
            } else if (Math.abs(oldY - player.actualMovement.getY()) < (oldYJumping - player.actualMovement.getY())) {
                player.clientVelocity.setY(oldY * 0.6);
            } else {
                player.clientVelocity.setY(oldYJumping * 0.6);
            }

        } else {
            livingEntityTravel();
        }
    }

    public void doWaterMove(float swimSpeed, boolean isFalling, float swimFriction) {
    }

    public void doLavaMove() {
    }

    public void doNormalMove(float blockFriction) {
    }

    public void livingEntityTravel() {
        double playerGravity = 0.08;

        boolean isFalling = player.actualMovement.getY() <= 0.0;
        if (isFalling && player.slowFallingAmplifier != 0) {
            playerGravity = 0.01;
            // Set fall distance to 0 if the player has slow falling
            player.fallDistance = 0;
        }

        player.gravity = playerGravity;

        float swimFriction;

        double lavaLevel = 0;
        if (canStandOnLava())
            lavaLevel = player.compensatedWorld.getLavaFluidLevelAt(GrimMath.floor(player.lastX), GrimMath.floor(player.lastY), GrimMath.floor(player.lastZ));

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
                swimSpeed += (player.speed - swimSpeed) * player.depthStriderLevel / 3.0F;
            }

            if (XMaterial.supports(13) && player.dolphinsGraceAmplifier != 0) {
                swimFriction = 0.96F;
            }

            doWaterMove(swimSpeed, isFalling, swimFriction);

            // 1.13 and below players can't climb ladders while touching water
            // yes, 1.13 players cannot climb ladders underwater
            if (player.getClientVersion().isNewerThanOrEquals(ClientVersion.v_1_14) && player.isClimbing) {
                player.lastWasClimbing = FluidFallingAdjustedMovement.getFluidFallingAdjustedMovement(player, playerGravity, isFalling, player.clientVelocity.clone().setY(0.2D * 0.8F)).getY();
            }

        } else {
            if (player.wasTouchingLava && !player.specialFlying && !(lavaLevel > 0 && canStandOnLava())) {

                doLavaMove();

                // Lava movement changed in 1.16
                if (player.getClientVersion().isNewerThanOrEquals(ClientVersion.v_1_16) && player.slightlyTouchingLava) {
                    player.clientVelocity = player.clientVelocity.multiply(new Vector(0.5D, 0.800000011920929D, 0.5D));
                    player.clientVelocity = FluidFallingAdjustedMovement.getFluidFallingAdjustedMovement(player, playerGravity, isFalling, player.clientVelocity);
                } else {
                    player.clientVelocity.multiply(0.5D);
                }

                if (player.hasGravity)
                    player.clientVelocity.add(new Vector(0.0D, -playerGravity / 4.0D, 0.0D));

            } else if (player.isGliding) {
                // Set fall distance to 1 if the player’s y velocity is greater than -0.5 when falling
                if (player.clientVelocity.getY() > -0.5)
                    player.fallDistance = 1;

                new PredictionEngineElytra().guessBestMovement(0, player);

            } else {
                float blockFriction = BlockProperties.getBlockFrictionUnderPlayer(player);
                player.friction = player.lastOnGround ? blockFriction * 0.91f : 0.91f;

                doNormalMove(blockFriction);
            }
        }
    }

    public boolean canStandOnLava() {
        return false;
    }
}