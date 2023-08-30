package ac.grim.grimac.predictionengine.movementtick;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.predictionengine.PlayerBaseTick;
import ac.grim.grimac.predictionengine.predictions.PredictionEngine;
import ac.grim.grimac.predictionengine.predictions.PredictionEngineElytra;
import ac.grim.grimac.utils.collisions.datatypes.SimpleCollisionBox;
import ac.grim.grimac.utils.data.VectorData;
import ac.grim.grimac.utils.data.packetentity.PacketEntity;
import ac.grim.grimac.utils.data.packetentity.PacketEntityStrider;
import ac.grim.grimac.utils.math.GrimMath;
import ac.grim.grimac.utils.nmsutil.*;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.world.states.defaulttags.BlockTags;
import com.github.retrooper.packetevents.protocol.world.states.type.StateType;
import com.github.retrooper.packetevents.protocol.world.states.type.StateTypes;
import com.github.retrooper.packetevents.util.Vector3d;
import org.bukkit.util.Vector;

public class MovementTicker {
    public final GrimPlayer player;

    public MovementTicker(GrimPlayer player) {
        this.player = player;
    }

    public static void handleEntityCollisions(GrimPlayer player) {
        // 1.7 and 1.8 do not have player collision
        if (player.getClientVersion().isNewerThan(ClientVersion.V_1_8)) {
            int possibleCollidingEntities = 0;

            // Players in vehicles do not have collisions
            if (!player.compensatedEntities.getSelf().inVehicle()) {
                // Calculate the offset of the player to colliding other stuff
                SimpleCollisionBox playerBox = GetBoundingBox.getBoundingBoxFromPosAndSize(player.lastX, player.lastY, player.lastZ, 0.6f, 1.8f);
                SimpleCollisionBox expandedPlayerBox = playerBox.copy().expandToAbsoluteCoordinates(player.x, player.y, player.z).expand(1);

                for (PacketEntity entity : player.compensatedEntities.entityMap.values()) {
                    // Players can only push living entities
                    // Players can also push boats or minecarts
                    // The one exemption to a living entity is an armor stand
                    if (!entity.isLivingEntity() && !EntityTypes.isTypeInstanceOf(entity.type, EntityTypes.BOAT) && !entity.isMinecart() || entity.type == EntityTypes.ARMOR_STAND)
                        continue;

                    SimpleCollisionBox entityBox = entity.getPossibleCollisionBoxes();

                    if (expandedPlayerBox.isCollided(entityBox))
                        possibleCollidingEntities++;
                }
            }

            if (player.isGliding && possibleCollidingEntities > 0) {
                // Horizontal starting movement affects vertical movement with elytra, hack around this.
                // This can likely be reduced but whatever, I don't see this as too much of a problem
                player.uncertaintyHandler.yNegativeUncertainty -= 0.05;
                player.uncertaintyHandler.yPositiveUncertainty += 0.05;
            }

            player.uncertaintyHandler.collidingEntities.add(possibleCollidingEntities);
        }
    }

    public void move(Vector inputVel, Vector collide) {
        if (player.stuckSpeedMultiplier.getX() < 0.99) {
            player.clientVelocity = new Vector();
        }

        if (inputVel.getX() != collide.getX()) {
            player.clientVelocity.setX(0);
        }

        if (inputVel.getZ() != collide.getZ()) {
            player.clientVelocity.setZ(0);
        }

        player.verticalCollision = inputVel.getY() != collide.getY();

        // Avoid order of collisions being wrong because 0.03 movements
        // Stepping movement USUALLY means the vehicle in on the ground as vehicles can't jump
        // Can be wrong with swim hopping into step, but this is rare and difficult to pull off
        // and would require a huge rewrite to support this rare edge case
        boolean calculatedOnGround = (player.verticalCollision && inputVel.getY() < 0.0D);

        // If the player is on the ground with a y velocity of 0, let the player decide (too close to call)
        if (inputVel.getY() == -SimpleCollisionBox.COLLISION_EPSILON && collide.getY() > -SimpleCollisionBox.COLLISION_EPSILON && collide.getY() <= 0 && !player.compensatedEntities.getSelf().inVehicle())
            calculatedOnGround = player.onGround;
        player.clientClaimsLastOnGround = player.onGround;

        // Fix step movement inside of water
        // Swim hop into step is very unlikely, as step requires y < 0, while swim hop forces y = 0.3
        if (player.compensatedEntities.getSelf().inVehicle() && player.clientControlledVerticalCollision && player.uncertaintyHandler.isStepMovement &&
                (inputVel.getY() <= 0 || player.predictedVelocity.isSwimHop())) {
            calculatedOnGround = true;
        }

        // We can't tell the difference between stepping and swim hopping, so just let the player's onGround status be the truth
        // Pistons/shulkers are a bit glitchy so just trust the client when they are affected by them
        // The player's onGround status isn't given when riding a vehicle, so we don't have a choice in whether we calculate or not
        //
        // Trust the onGround status if the player is near the ground and they sent a ground packet
        if (player.compensatedEntities.getSelf().inVehicle() || !player.exemptOnGround()) {
            player.onGround = calculatedOnGround;
        }

        // This is around the place where the new bounding box gets set
        player.boundingBox = GetBoundingBox.getCollisionBoxForPlayer(player, player.x, player.y, player.z);
        // This is how the player checks for fall damage
        // By running fluid pushing for the player
        if (!player.wasTouchingWater && (player.compensatedEntities.getSelf().getRiding() == null || !EntityTypes.isTypeInstanceOf(player.compensatedEntities.getSelf().getRiding().type, EntityTypes.BOAT))) {
            new PlayerBaseTick(player).updateInWaterStateAndDoWaterCurrentPushing();
        }

        if (player.onGround) {
            player.fallDistance = 0;
        } else if (collide.getY() < 0) {
            player.fallDistance = (player.fallDistance) - collide.getY();
            player.vehicleData.lastYd = collide.getY();
        }

        // Striders call the method for inside blocks AGAIN!
        if (player.compensatedEntities.getSelf().getRiding() instanceof PacketEntityStrider) {
            Collisions.handleInsideBlocks(player);
        }

        player.mainSupportingBlockData = MainSupportingBlockPosFinder.findMainSupportingBlockPos(player, player.mainSupportingBlockData, new Vector3d(collide.getX(), collide.getY(), collide.getZ()), player.boundingBox, player.onGround);
        StateType onBlock = BlockProperties.getOnPos(player, player.mainSupportingBlockData, new Vector3d(player.x, player.y, player.z));

        // Hack with 1.14+ poses issue
        if (inputVel.getY() != collide.getY()) {
            // If the client supports slime blocks
            // And the block is a slime block
            // Or the block is honey and was replaced by viaversion
            if (player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_8)
                    && (onBlock == StateTypes.SLIME_BLOCK || (onBlock == StateTypes.HONEY_BLOCK && player.getClientVersion().isOlderThanOrEquals(ClientVersion.V_1_14_4)))) {
                if (player.isSneaking) { // Slime blocks use shifting instead of sneaking
                    player.clientVelocity.setY(0);
                } else {
                    if (player.clientVelocity.getY() < 0.0) {
                        player.clientVelocity.setY(-player.clientVelocity.getY() *
                                (player.compensatedEntities.getSelf().getRiding() != null && !player.compensatedEntities.getSelf().getRiding().isLivingEntity() ? 0.8 : 1.0));
                    }
                }
            } else if (BlockTags.BEDS.contains(onBlock) && player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_12)) {
                if (player.clientVelocity.getY() < 0.0) {
                    player.clientVelocity.setY(-player.clientVelocity.getY() * 0.6600000262260437 *
                            (player.compensatedEntities.getSelf().getRiding() != null && !player.compensatedEntities.getSelf().getRiding().isLivingEntity() ? 0.8 : 1.0));
                }
            } else {
                player.clientVelocity.setY(0);
            }
        }

        collide = PredictionEngine.clampMovementToHardBorder(player, collide);

        // The game disregards movements smaller than 1e-7 (such as in boats)
        if (collide.lengthSquared() < 1e-7) {
            collide = new Vector();
        }

        // This is where vanilla moves the bounding box and sets it
        player.predictedVelocity = new VectorData(collide.clone(), player.predictedVelocity.lastVector, player.predictedVelocity.vectorType);

        float f = BlockProperties.getBlockSpeedFactor(player, player.mainSupportingBlockData, new Vector3d(player.x, player.y, player.z));
        player.clientVelocity.multiply(new Vector(f, 1, f));

        // Reset stuck speed so it can update
        if (player.stuckSpeedMultiplier.getX() < 0.99) {
            player.uncertaintyHandler.lastStuckSpeedMultiplier.reset();
        }

        player.stuckSpeedMultiplier = new Vector(1, 1, 1);

        // 1.15 and older clients use the handleInsideBlocks method for lava
        if (player.getClientVersion().isOlderThan(ClientVersion.V_1_16))
            player.wasTouchingLava = false;

        Collisions.handleInsideBlocks(player);

        if (player.stuckSpeedMultiplier.getX() < 0.9) {
            // Reset fall distance if stuck in block
            player.fallDistance = 0;
        }

        // Flying players are not affected by cobwebs/sweet berry bushes
        if (player.isFlying) {
            player.stuckSpeedMultiplier = new Vector(1, 1, 1);
        }
    }

    public void livingEntityAIStep() {
        handleEntityCollisions(player);

        SimpleCollisionBox oldBB = player.boundingBox.copy();

        if (player.compensatedEntities.getSelf().getRiding() == null) {
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

        // A 1.8 player may spawn and get -0.1 gravity instead of -0.08 gravity
        if (player.uncertaintyHandler.lastTeleportTicks.hasOccurredSince(0)) {
            player.uncertaintyHandler.yNegativeUncertainty -= 0.02;
        }

        if (player.isFlying) {
            SimpleCollisionBox playerBox = GetBoundingBox.getCollisionBoxForPlayer(player, player.lastX, player.lastY, player.lastZ);
            if (!Collisions.isEmpty(player, playerBox.copy().offset(0, 0.1, 0))) {
                player.uncertaintyHandler.yPositiveUncertainty = player.flySpeed * 5;
            }

            if (!Collisions.isEmpty(player, playerBox.copy().offset(0, -0.1, 0))) {
                player.uncertaintyHandler.yNegativeUncertainty = player.flySpeed * -5;
            }
        }

        // Work around a bug introduced in 1.14 where a player colliding with an X and Z wall maintains X momentum
        if (player.getClientVersion().isOlderThan(ClientVersion.V_1_14) || player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_18_2)) // 1.18.2 fixes this.
            return;

        // YXZ or YZX collision order
        // Except 0.03 causing apparent XZY or ZXY collision order
        // Meaning we should scan upwards!
        oldBB.expand(-SimpleCollisionBox.COLLISION_EPSILON);

        double posX = Math.max(0, player.predictedVelocity.vector.getX()) + SimpleCollisionBox.COLLISION_EPSILON;
        double negX = Math.min(0, player.predictedVelocity.vector.getX()) - SimpleCollisionBox.COLLISION_EPSILON;
        double posZ = Math.max(0, player.predictedVelocity.vector.getZ()) + SimpleCollisionBox.COLLISION_EPSILON;
        double negZ = Math.min(0, player.predictedVelocity.vector.getZ()) - SimpleCollisionBox.COLLISION_EPSILON;

        boolean xAxisCollision = !Collisions.isEmpty(player, oldBB.expandMin(negX, 0, 0).expandMax(posX, 0, 0));
        boolean zAxisCollision = !Collisions.isEmpty(player, oldBB.expandMin(0, 0, negZ).expandMax(0, 0, posZ));

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
        if (zAxisCollision && xAxisCollision) {
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
                // Horizontal movement affects vertical movement with elytra, hack around this.
                // This can likely be reduced but whatever, I don't see this as too much of a problem
                player.uncertaintyHandler.yNegativeUncertainty -= 0.05;
                player.uncertaintyHandler.yPositiveUncertainty += 0.05;
            }

            player.uncertaintyHandler.xNegativeUncertainty -= playerSpeed * 3;
            player.uncertaintyHandler.xPositiveUncertainty += playerSpeed * 3;
        }
    }

    public void playerEntityTravel() {
        if (player.isFlying && player.compensatedEntities.getSelf().getRiding() == null) {
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
        if (isFalling && player.compensatedEntities.getSlowFallingAmplifier() != null) {
            playerGravity = 0.01;
            // Set fall distance to 0 if the player has slow falling
            player.fallDistance = 0;
        }

        player.gravity = playerGravity;

        float swimFriction;

        double lavaLevel = 0;
        if (canStandOnLava())
            lavaLevel = player.compensatedWorld.getLavaFluidLevelAt(GrimMath.floor(player.lastX), GrimMath.floor(player.lastY), GrimMath.floor(player.lastZ));

        if (player.wasTouchingWater && !player.isFlying) {
            // 0.8F seems hardcoded in
            // 1.13+ players on skeleton horses swim faster! Cool feature.
            boolean isSkeletonHorse = player.compensatedEntities.getSelf().getRiding() != null && player.compensatedEntities.getSelf().getRiding().type == EntityTypes.SKELETON_HORSE && player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_13);
            swimFriction = player.isSprinting && player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_13) ? 0.9F : (isSkeletonHorse ? 0.96F : 0.8F);
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

            if (player.compensatedEntities.getDolphinsGraceAmplifier() != null) {
                swimFriction = 0.96F;
            }

            player.friction = swimFriction; // Not vanilla, just useful for other grim stuff
            doWaterMove(swimSpeed, isFalling, swimFriction);

            player.isClimbing = Collisions.onClimbable(player, player.x, player.y, player.z);

            // 1.13 and below players can't climb ladders while touching water
            // yes, 1.13 players cannot climb ladders underwater
            if (player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_14) && player.isClimbing) {
                player.lastWasClimbing = FluidFallingAdjustedMovement.getFluidFallingAdjustedMovement(player, playerGravity, isFalling, player.clientVelocity.clone().setY(0.2D * 0.8F)).getY();
            }

        } else {
            if (player.wasTouchingLava && !player.isFlying && !(lavaLevel > 0 && canStandOnLava())) {
                player.friction = 0.5F; // Not vanilla, just useful for other grim stuff

                doLavaMove();

                // Lava movement changed in 1.16
                if (player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_16) && player.slightlyTouchingLava) {
                    player.clientVelocity = player.clientVelocity.multiply(new Vector(0.5D, 0.800000011920929D, 0.5D));
                    player.clientVelocity = FluidFallingAdjustedMovement.getFluidFallingAdjustedMovement(player, playerGravity, isFalling, player.clientVelocity);
                } else {
                    player.clientVelocity.multiply(0.5D);
                }

                if (player.hasGravity)
                    player.clientVelocity.add(new Vector(0.0D, -playerGravity / 4.0D, 0.0D));

            } else if (player.isGliding) {
                player.friction = 0.99F; // Not vanilla, just useful for other grim stuff
                // Set fall distance to 1 if the player’s y velocity is greater than -0.5 when falling
                if (player.clientVelocity.getY() > -0.5)
                    player.fallDistance = 1;

                new PredictionEngineElytra().guessBestMovement(0, player);

            } else {
                float blockFriction = BlockProperties.getFriction(player, player.mainSupportingBlockData, new Vector3d(player.lastX, player.lastY, player.lastZ));
                player.friction = player.lastOnGround ? blockFriction * 0.91f : 0.91f;

                doNormalMove(blockFriction);
            }
        }
    }

    public boolean canStandOnLava() {
        return false;
    }
}