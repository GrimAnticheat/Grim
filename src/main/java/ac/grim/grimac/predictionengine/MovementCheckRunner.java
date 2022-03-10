package ac.grim.grimac.predictionengine;

import ac.grim.grimac.checks.impl.movement.EntityControl;
import ac.grim.grimac.checks.type.PositionCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.predictionengine.movementtick.MovementTickerHorse;
import ac.grim.grimac.predictionengine.movementtick.MovementTickerPig;
import ac.grim.grimac.predictionengine.movementtick.MovementTickerPlayer;
import ac.grim.grimac.predictionengine.movementtick.MovementTickerStrider;
import ac.grim.grimac.predictionengine.predictions.PredictionEngineNormal;
import ac.grim.grimac.predictionengine.predictions.rideable.BoatPredictionEngine;
import ac.grim.grimac.utils.anticheat.update.PositionUpdate;
import ac.grim.grimac.utils.anticheat.update.PredictionComplete;
import ac.grim.grimac.utils.collisions.datatypes.SimpleCollisionBox;
import ac.grim.grimac.utils.data.VectorData;
import ac.grim.grimac.utils.data.packetentity.PacketEntityHorse;
import ac.grim.grimac.utils.data.packetentity.PacketEntityRideable;
import ac.grim.grimac.utils.enums.Pose;
import ac.grim.grimac.utils.math.GrimMath;
import ac.grim.grimac.utils.math.VectorUtils;
import ac.grim.grimac.utils.nmsutil.Collisions;
import ac.grim.grimac.utils.nmsutil.GetBoundingBox;
import ac.grim.grimac.utils.nmsutil.Riptide;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.item.enchantment.type.EnchantmentTypes;
import com.github.retrooper.packetevents.protocol.item.type.ItemType;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.world.states.defaulttags.BlockTags;
import com.github.retrooper.packetevents.protocol.world.states.type.StateTypes;
import com.github.retrooper.packetevents.util.Vector3d;
import org.bukkit.GameMode;
import org.bukkit.util.Vector;

public class MovementCheckRunner extends PositionCheck {
    // Averaged over 500 predictions (Defaults set slightly above my 3600x results)
    public static double predictionNanos = 0.3 * 1e6;
    // Averaged over 20000 predictions
    public static double longPredictionNanos = 0.3 * 1e6;

    public MovementCheckRunner(GrimPlayer player) {
        super(player);
    }

    public void processAndCheckMovementPacket(PositionUpdate data) {
        // The player is in an unloaded chunk and didn't teleport
        // OR
        // This teleport wasn't valid as the player STILL hasn't loaded this damn chunk.
        // Keep re-teleporting until they load the chunk!
        if (player.getSetbackTeleportUtil().insideUnloadedChunk()) {
            if (player.inVehicle) return;

            player.lastOnGround = player.clientClaimsLastOnGround; // Stop a false on join
            if (player.getSetbackTeleportUtil().getRequiredSetBack() == null) return; // Not spawned yet
            if (!data.isTeleport()) {
                // Teleport the player back to avoid players being able to simply ignore transactions
                player.getSetbackTeleportUtil().executeForceResync();
                return;
            }
        }

        long start = System.nanoTime();
        check(data);
        long length = System.nanoTime() - start;

        if (!player.disableGrim) {
            predictionNanos = (predictionNanos * 499 / 500d) + (length / 500d);
            longPredictionNanos = (longPredictionNanos * 19999 / 20000d) + (length / 20000d);
        }
    }

    private void handleTeleport(PositionUpdate update) {
        player.lastX = player.x;
        player.lastY = player.y;
        player.lastZ = player.z;
        player.uncertaintyHandler.lastTeleportTicks = 0;

        // Reset velocities
        // Teleporting a vehicle does not reset its velocity
        if (!player.inVehicle) {
            player.clientVelocity = new Vector();
        }

        player.lastWasClimbing = 0;
        player.fallDistance = 0;
        player.canSwimHop = false;

        // Teleports OVERRIDE explosions and knockback
        player.checkManager.getExplosionHandler().forceExempt();
        player.checkManager.getExplosionHandler().handlePlayerExplosion(0);
        player.checkManager.getKnockbackHandler().forceExempt();
        player.checkManager.getKnockbackHandler().handlePlayerKb(0);

        // Manually call prediction complete to handle teleport
        player.getSetbackTeleportUtil().onPredictionComplete(new PredictionComplete(0, update));

        player.uncertaintyHandler.lastHorizontalOffset = 0;
        player.uncertaintyHandler.lastVerticalOffset = 0;

        player.boundingBox = GetBoundingBox.getCollisionBoxForPlayer(player, player.x, player.y, player.z);
    }

    private void check(PositionUpdate update) {
        player.uncertaintyHandler.stuckOnEdge--;
        player.uncertaintyHandler.lastStuckEast--;
        player.uncertaintyHandler.lastStuckWest--;
        player.uncertaintyHandler.lastStuckSouth--;
        player.uncertaintyHandler.lastStuckNorth--;

        if (update.isTeleport()) {
            handleTeleport(update);
            return;
        }

        // Stop people from crashing predictions with timer
        if (player.getSetbackTeleportUtil().blockPredictions) return;

        player.onGround = update.isOnGround();

        if (!player.specialFlying && player.isSneaking && Collisions.isAboveGround(player)) {
            // Before we do player block placements, determine if the shifting glitch occurred
            // The 0.03 and maintaining velocity is just brutal
            //
            // 16 - Magic number to stop people from crashing the server
            double posX = Math.max(0.1, Math.max(player.actualMovement.getX(), 16) + 0.1);
            double posZ = Math.max(0.1, Math.max(player.actualMovement.getZ(), 16) + 0.1);
            double negX = Math.min(-0.1, Math.max(player.actualMovement.getX(), 16) - 0.1);
            double negZ = Math.min(-0.1, Math.max(player.actualMovement.getZ(), 16) - 0.1);

            Vector NE = Collisions.maybeBackOffFromEdge(new Vector(posX, 0, posZ), player, true);
            Vector NW = Collisions.maybeBackOffFromEdge(new Vector(negX, 0, negZ), player, true);
            Vector SE = Collisions.maybeBackOffFromEdge(new Vector(posX, 0, posZ), player, true);
            Vector SW = Collisions.maybeBackOffFromEdge(new Vector(negX, 0, negZ), player, true);

            boolean isEast = NE.getX() != posX || SE.getX() != posX;
            boolean isWest = NW.getX() != negX || SW.getX() != negX;
            boolean isNorth = NE.getZ() != posZ || NW.getZ() != posZ;
            boolean isSouth = SE.getZ() != posZ || SW.getZ() != posZ;

            if (isEast) player.uncertaintyHandler.lastStuckEast = 0;
            if (isWest) player.uncertaintyHandler.lastStuckWest = 0;
            if (isSouth) player.uncertaintyHandler.lastStuckSouth = 0;
            if (isNorth) player.uncertaintyHandler.lastStuckNorth = 0;

            if (player.uncertaintyHandler.lastStuckEast > -3) {
                player.uncertaintyHandler.xPositiveUncertainty += player.speed;
            }

            if (player.uncertaintyHandler.lastStuckWest > -3) {
                player.uncertaintyHandler.xNegativeUncertainty -= player.speed;
            }

            if (player.uncertaintyHandler.lastStuckNorth > -3) {
                player.uncertaintyHandler.zNegativeUncertainty -= player.speed;
            }

            if (player.uncertaintyHandler.lastStuckSouth > -3) {
                player.uncertaintyHandler.zPositiveUncertainty += player.speed;
            }

            if (isEast || isWest || isSouth || isNorth) {
                player.uncertaintyHandler.stuckOnEdge = 0;
            }
        }

        if (!update.isTeleport()) player.movementPackets++;

        // Tick updates AFTER updating bounding box and actual movement
        player.compensatedWorld.tickPlayerInPistonPushingArea();
        player.compensatedEntities.tick();

        // Update knockback and explosions after getting the vehicle
        player.firstBreadKB = player.checkManager.getKnockbackHandler().calculateFirstBreadKnockback(player.inVehicle ? player.vehicle : player.entityID, player.lastTransactionReceived.get());
        player.likelyKB = player.checkManager.getKnockbackHandler().calculateRequiredKB(player.inVehicle ? player.vehicle : player.entityID, player.lastTransactionReceived.get());

        player.firstBreadExplosion = player.checkManager.getExplosionHandler().getFirstBreadAddedExplosion(player.lastTransactionReceived.get());
        player.likelyExplosions = player.checkManager.getExplosionHandler().getPossibleExplosions(player.lastTransactionReceived.get());

        // The game's movement is glitchy when switching between vehicles
        // This is due to mojang not telling us where the new vehicle's location is
        // meaning the first move gets hidden... beautiful
        //
        // Exiting vehicles does not suffer the same issue
        //
        // It is also glitchy when switching between client vs server vehicle control
        player.vehicleData.lastVehicleSwitch++;
        if (player.vehicleData.wasVehicleSwitch || player.vehicleData.lastDummy) {
            player.vehicleData.lastVehicleSwitch = 0;
        }

        if (player.vehicleData.lastVehicleSwitch < 5) {
            player.checkManager.getExplosionHandler().forceExempt();
            player.checkManager.getKnockbackHandler().forceExempt();
        }

        if (player.vehicleData.lastDummy) {
            player.clientVelocity.multiply(0.98); // This is vanilla, do not touch
        }

        if (player.vehicleData.wasVehicleSwitch || player.vehicleData.lastDummy) {
            update.setTeleport(true);

            if (player.playerVehicle != null) {
                Vector pos = new Vector(player.x, player.y, player.z);
                Vector cutTo = VectorUtils.cutBoxToVector(pos, player.playerVehicle.getPossibleCollisionBoxes());

                // Stop players from teleporting when they enter a vehicle
                // Is this a cheat?  Do we have to lower this threshold?
                // Until I see evidence that this cheat exists, I am keeping this lenient.
                if (cutTo.distanceSquared(pos) > 1) {
                    player.getSetbackTeleportUtil().executeForceResync();
                }
            }

            player.boundingBox = GetBoundingBox.getCollisionBoxForPlayer(player, player.x, player.y, player.z);
            player.isClimbing = Collisions.onClimbable(player, player.x, player.y, player.z);

            player.vehicleData.lastDummy = false;
            player.vehicleData.wasVehicleSwitch = false;

            // Mojang is dumb and combines two movements when starting vehicle movement
            if (player.playerVehicle instanceof PacketEntityRideable) {
                if (((PacketEntityRideable) player.playerVehicle).currentBoostTime < ((PacketEntityRideable) player.playerVehicle).boostTimeMax) {
                    // This is not a value hack, please do not change this.
                    // Any other value will false.
                    ((PacketEntityRideable) player.playerVehicle).currentBoostTime++;
                }
            }

            // The server sets vehicle velocity when entering
            // Grim also does this, although the server
            // overrides Grim due to packet order.
            // This is intentional!  We don't want to modify
            // vanilla behavior if it's not a bug.
            if (player.likelyKB != null) {
                player.clientVelocity = player.likelyKB.vector;
            }

            if (player.firstBreadKB != null) {
                player.clientVelocity = player.firstBreadKB.vector;
            }

            handleTeleport(update);

            if (player.isClimbing) {
                Vector ladder = player.clientVelocity.clone().setY(0.2);
                PredictionEngineNormal.staticVectorEndOfTick(player, ladder);
                player.lastWasClimbing = ladder.getY();
            }

            return;
        }

        if (player.isInBed != player.lastInBed) {
            update.setTeleport(true);
        }
        player.lastInBed = player.isInBed;

        // Teleporting is not a tick, don't run anything that we don't need to, to avoid falses
        player.uncertaintyHandler.lastTeleportTicks--;

        // Don't check sleeping players
        if (player.isInBed) return;

        if (!player.inVehicle) {
            player.speed = player.compensatedEntities.getPlayerMovementSpeed();
            if (player.hasGravity != player.playerEntityHasGravity) {
                player.pointThreeEstimator.updatePlayerGravity();
            }
            player.hasGravity = player.playerEntityHasGravity;
        }

        // Check if the player can control their horse, if they are on a horse
        //
        // Player cannot control entities if other players are doing so, although the server will just
        // ignore these bad packets
        // Players cannot control stacked vehicles
        // Again, the server knows to ignore this
        //
        // Therefore, we just assume that the client and server are modded or whatever.
        if (player.inVehicle) {
            // Players are unable to take explosions in vehicles
            player.checkManager.getExplosionHandler().forceExempt();

            // When in control of the entity, the player sets the entity position to their current position
            player.playerVehicle.setPositionRaw(GetBoundingBox.getPacketEntityBoundingBox(player.x, player.y, player.z, player.playerVehicle));

            if (player.hasGravity != player.playerVehicle.hasGravity) {
                player.pointThreeEstimator.updatePlayerGravity();
            }
            player.hasGravity = player.playerVehicle.hasGravity;

            // For whatever reason the vehicle move packet occurs AFTER the player changes slots...
            if (player.playerVehicle instanceof PacketEntityRideable) {
                EntityControl control = ((EntityControl) player.checkManager.getPostPredictionCheck(EntityControl.class));

                ItemType requiredItem = player.playerVehicle.type == EntityTypes.PIG ? ItemTypes.CARROT_ON_A_STICK : ItemTypes.WARPED_FUNGUS_ON_A_STICK;
                ItemStack mainHand = player.getInventory().getHeldItem();
                ItemStack offHand = player.getInventory().getOffHand();

                boolean correctMainHand = mainHand.getType() == requiredItem;
                boolean correctOffhand = offHand.getType() == requiredItem;

                if (!correctMainHand && !correctOffhand) {
                    // Entity control cheats!  Set the player back
                    control.flag();
                } else {
                    control.rewardPlayer();
                }
            }
        }

        player.uncertaintyHandler.lastFlyingTicks++;
        if (player.isFlying) {
            player.fallDistance = 0;
            player.uncertaintyHandler.lastFlyingTicks = 0;
        }

        player.isClimbing = Collisions.onClimbable(player, player.lastX, player.lastY, player.lastZ);
        player.specialFlying = player.onGround && !player.isFlying && player.wasFlying || player.isFlying;

        player.clientControlledVerticalCollision = Math.abs(player.y % (1 / 64D)) < 0.00001;
        // If you really have nothing better to do, make this support offset blocks like bamboo.  Good luck!
        player.clientControlledHorizontalCollision = Math.min(GrimMath.distanceToHorizontalCollision(player.x), GrimMath.distanceToHorizontalCollision(player.z)) < 1e-6;

        player.uncertaintyHandler.lastSneakingChangeTicks--;
        if (player.isSneaking != player.wasSneaking)
            player.uncertaintyHandler.lastSneakingChangeTicks = 0;

        // This isn't the final velocity of the player in the tick, only the one applied to the player
        player.actualMovement = new Vector(player.x - player.lastX, player.y - player.lastY, player.z - player.lastZ);

        if (player.actualMovement.length() < 50) { // anti-crash
            Vector phase = Collisions.collide(player, player.actualMovement.getX(), player.actualMovement.getY(), player.actualMovement.getZ());
            player.calculatedCollision = new Vector3d(phase.getX(), phase.getY(), phase.getZ());
        }

        // ViaVersion messes up flight speed for 1.7 players
        if (player.getClientVersion().isOlderThanOrEquals(ClientVersion.V_1_7_10) && player.isFlying)
            player.isSprinting = true;


        boolean oldFlying = player.isFlying;
        boolean oldGliding = player.isGliding;
        boolean oldSpecialFlying = player.specialFlying;
        boolean oldSprinting = player.isSprinting;
        boolean oldSneaking = player.isSneaking;

        // Stop stuff like clients using elytra in a vehicle...
        // Interesting, on a pig or strider, a player can climb a ladder
        if (player.inVehicle) {
            // Reset fall distance when riding
            //player.fallDistance = 0;
            player.isFlying = false;
            player.isGliding = false;
            player.specialFlying = false;
            player.isSprinting = false;
            player.isSneaking = false;

            if (player.playerVehicle.type != EntityTypes.PIG && player.playerVehicle.type != EntityTypes.STRIDER) {
                player.isClimbing = false;
            }
        }

        // Multiplying by 1.3 or 1.3f results in precision loss, you must multiply by 0.3
        // The player updates their attribute if it doesn't match the last value
        // This last value can be changed by the server, however.
        //
        // Sprinting status itself does not desync, only the attribute as mojang forgot that the server
        // can change the attribute
        if (player.isSprinting != player.lastSprinting) {
            player.compensatedEntities.hasSprintingAttributeEnabled = player.isSprinting;
        }
        player.speed += player.compensatedEntities.hasSprintingAttributeEnabled ? player.speed * 0.3f : 0;

        player.uncertaintyHandler.lastGlidingChangeTicks--;
        if (player.isGliding != player.wasGliding) player.uncertaintyHandler.lastGlidingChangeTicks = 0;


        SimpleCollisionBox steppingOnBB = GetBoundingBox.getCollisionBoxForPlayer(player, player.x, player.y, player.z).expand(0.03).offset(0, -1, 0);
        player.uncertaintyHandler.isSteppingOnSlime = Collisions.hasSlimeBlock(player);
        player.uncertaintyHandler.wasSteppingOnBouncyBlock = player.uncertaintyHandler.isSteppingOnBouncyBlock;
        player.uncertaintyHandler.isSteppingOnBouncyBlock = Collisions.hasBouncyBlock(player);
        player.uncertaintyHandler.isSteppingOnIce = Collisions.hasMaterial(player, steppingOnBB, type -> BlockTags.ICE.contains(type.getType()));
        player.uncertaintyHandler.isSteppingOnHoney = Collisions.hasMaterial(player, StateTypes.HONEY_BLOCK, -0.03);
        player.uncertaintyHandler.isSteppingNearBubbleColumn = player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_13) && Collisions.hasMaterial(player, StateTypes.BUBBLE_COLUMN, -1);

        player.uncertaintyHandler.thisTickSlimeBlockUncertainty = player.uncertaintyHandler.nextTickSlimeBlockUncertainty;
        player.uncertaintyHandler.nextTickSlimeBlockUncertainty = 0;
        player.couldSkipTick = false;

        // Update firework end/start uncertainty
        player.uncertaintyHandler.lastFireworkStatusChange--;
        boolean hasFirework = (player.isGliding || player.wasGliding) && player.compensatedFireworks.getMaxFireworksAppliedPossible() > 0;
        if (hasFirework != player.uncertaintyHandler.lastUsingFirework)
            player.uncertaintyHandler.lastFireworkStatusChange = 0;
        player.uncertaintyHandler.lastUsingFirework = hasFirework;

        SimpleCollisionBox expandedBB = GetBoundingBox.getBoundingBoxFromPosAndSize(player.lastX, player.lastY, player.lastZ, 0.001f, 0.001f);

        // Don't expand if the player moved more than 50 blocks this tick (stop netty crash exploit)
        if (player.actualMovement.lengthSquared() < 2500)
            expandedBB.expandToAbsoluteCoordinates(player.x, player.y, player.z);

        expandedBB.expand(Pose.STANDING.width / 2, 0, Pose.STANDING.width / 2);
        expandedBB.expandMax(0, Pose.STANDING.height, 0);

        // if the player is using a version with glitched chest and anvil bounding boxes,
        // and they are intersecting with these glitched bounding boxes
        // give them a decent amount of uncertainty and don't ban them for mojang's stupid mistake
        boolean isGlitchy = player.uncertaintyHandler.isNearGlitchyBlock;
        player.uncertaintyHandler.isNearGlitchyBlock = player.getClientVersion().isOlderThan(ClientVersion.V_1_9)
                && Collisions.hasMaterial(player, expandedBB.copy().expand(0.03),
                checkData -> BlockTags.ANVIL.contains(checkData.getType())
                        || checkData.getType() == StateTypes.CHEST || checkData.getType() == StateTypes.TRAPPED_CHEST);
        player.uncertaintyHandler.isOrWasNearGlitchyBlock = isGlitchy || player.uncertaintyHandler.isNearGlitchyBlock;

        player.uncertaintyHandler.scaffoldingOnEdge = player.uncertaintyHandler.nextTickScaffoldingOnEdge;
        player.uncertaintyHandler.checkForHardCollision();

        player.uncertaintyHandler.lastFlyingStatusChange--;
        if (player.isFlying != player.wasFlying) player.uncertaintyHandler.lastFlyingStatusChange = 0;

        player.uncertaintyHandler.lastThirtyMillionHardBorder--;
        if (!player.inVehicle && (Math.abs(player.x) == 2.9999999E7D || Math.abs(player.z) == 2.9999999E7D)) {
            player.uncertaintyHandler.lastThirtyMillionHardBorder = 0;
        }

        player.uncertaintyHandler.lastUnderwaterFlyingHack--;
        if (player.specialFlying && player.getClientVersion().isOlderThan(ClientVersion.V_1_13) && player.compensatedWorld.containsLiquid(player.boundingBox)) {
            player.uncertaintyHandler.lastUnderwaterFlyingHack = 0;
        }

        boolean couldBeStuckSpeed = Collisions.checkStuckSpeed(player, 0.03);
        boolean couldLeaveStuckSpeed = player.isPointThree() && Collisions.checkStuckSpeed(player, -0.03);
        player.uncertaintyHandler.claimingLeftStuckSpeed = !player.inVehicle && player.stuckSpeedMultiplier.getX() < 1 && !couldLeaveStuckSpeed;

        if (couldBeStuckSpeed) {
            player.uncertaintyHandler.lastStuckSpeedMultiplier = 0;
        }

        Vector backOff = Collisions.maybeBackOffFromEdge(player.clientVelocity, player, true);
        player.uncertaintyHandler.nextTickScaffoldingOnEdge = player.clientVelocity.getX() != 0 && player.clientVelocity.getZ() != 0 && backOff.getX() == 0 && backOff.getZ() == 0;
        Vector oldClientVel = player.clientVelocity;

        boolean wasChecked = false;

        // Exempt if the player is offline
        if (player.isDead || (player.playerVehicle != null && player.playerVehicle.isDead)) {
            // Dead players can't cheat, if you find a way how they could, open an issue
            player.predictedVelocity = new VectorData(player.actualMovement, VectorData.VectorType.Dead);
            player.clientVelocity = new Vector();
        } else if (player.disableGrim || (PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_8) && player.gamemode == GameMode.SPECTATOR) || player.specialFlying) {
            // We could technically check spectator but what's the point...
            // Added complexity to analyze a gamemode used mainly by moderators
            //
            // TODO: Re-implement flying support, although LUNAR HAS FLYING CHEATS!!! HOW CAN I CHECK WHEN HALF THE PLAYER BASE IS USING CHEATS???
            player.predictedVelocity = new VectorData(player.actualMovement, VectorData.VectorType.Spectator);
            player.clientVelocity = player.actualMovement.clone();
            player.gravity = 0;
            player.friction = 0.91f;
            PredictionEngineNormal.staticVectorEndOfTick(player, player.clientVelocity);
        } else if (player.playerVehicle == null) {
            wasChecked = true;

            // Depth strider was added in 1.8
            ItemStack boots = player.getInventory().getBoots();
            if (player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_8)) {
                player.depthStriderLevel = boots.getEnchantmentLevel(EnchantmentTypes.DEPTH_STRIDER);
            } else {
                player.depthStriderLevel = 0;
            }

            // This is wrong and the engine was not designed around stuff like this
            player.verticalCollision = false;

            // Riptiding while on the ground moves the hitbox upwards before any movement code runs
            // It's a pain to support and this is my best attempt
            if (player.lastOnGround && player.tryingToRiptide && !player.inVehicle) {
                Vector pushingMovement = Collisions.collide(player, 0, 1.1999999F, 0);
                player.verticalCollision = pushingMovement.getY() != 1.1999999F;
                double currentY = player.clientVelocity.getY();

                player.uncertaintyHandler.thisTickSlimeBlockUncertainty = Math.abs(Riptide.getRiptideVelocity(player).getY()) + (currentY > 0 ? currentY : 0);
                player.uncertaintyHandler.nextTickSlimeBlockUncertainty = Math.abs(Riptide.getRiptideVelocity(player).getY()) + (currentY > 0 ? currentY : 0);

                player.lastOnGround = false;
                player.lastY += pushingMovement.getY();
                new PlayerBaseTick(player).updatePlayerPose();
                player.boundingBox = GetBoundingBox.getPlayerBoundingBox(player, player.lastX, player.lastY, player.lastZ);
                player.actualMovement = new Vector(player.x - player.lastX, player.y - player.lastY, player.z - player.lastZ);

                player.couldSkipTick = true;

                Collisions.handleInsideBlocks(player);
            }

            new PlayerBaseTick(player).doBaseTick();
            new MovementTickerPlayer(player).livingEntityAIStep();
            new PlayerBaseTick(player).updatePowderSnow();
            new PlayerBaseTick(player).updatePlayerPose();

        } else if (PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_9) && player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_9)) {
            wasChecked = true;
            // The player and server are both on a version with client controlled entities
            // If either or both of the client server version has server controlled entities
            // The player can't use entities (or the server just checks the entities)
            if (player.playerVehicle.type == EntityTypes.BOAT) {
                new PlayerBaseTick(player).doBaseTick();
                // Speed doesn't affect anything with boat movement
                new BoatPredictionEngine(player).guessBestMovement(0.1f, player);
            } else if (player.playerVehicle instanceof PacketEntityHorse) {
                new PlayerBaseTick(player).doBaseTick();
                new MovementTickerHorse(player).livingEntityAIStep();
            } else if (player.playerVehicle.type == EntityTypes.PIG) {
                new PlayerBaseTick(player).doBaseTick();
                new MovementTickerPig(player).livingEntityAIStep();
            } else if (player.playerVehicle.type == EntityTypes.STRIDER) {
                new PlayerBaseTick(player).doBaseTick();
                new MovementTickerStrider(player).livingEntityAIStep();
                MovementTickerStrider.floatStrider(player);
                Collisions.handleInsideBlocks(player);
            } else {
                wasChecked = false;
            }
        } // If it isn't any of these cases, the player is on a mob they can't control and therefore is exempt

        // No, don't comment about the sqrt call.  It doesn't matter at all on modern CPU's.
        double offset = player.predictedVelocity.vector.distance(player.actualMovement);
        offset = player.uncertaintyHandler.reduceOffset(offset);

        // Let's hope this doesn't desync :)
        if (player.getSetbackTeleportUtil().blockOffsets)
            offset = 0;

        // Don't check players who are offline
        if (player.bukkitPlayer != null && !player.bukkitPlayer.isOnline()) return;
        // Don't check players who just switched worlds
        if (player.bukkitPlayer != null && player.playerWorld != player.bukkitPlayer.getWorld()) return;

        if (wasChecked) {
            // We shouldn't attempt to send this prediction analysis into checks if we didn't predict anything
            player.checkManager.onPredictionFinish(new PredictionComplete(offset, update));
        } else {
            // The player wasn't checked, explosion and knockback status unknown
            player.checkManager.getExplosionHandler().forceExempt();
            player.checkManager.getKnockbackHandler().forceExempt();
        }

        // If the player is abusing a setback in order to gain the onGround status of true.
        // and the player then jumps from this position in the air.
        // Fixes LiquidBounce Jesus NCP, and theoretically AirJump bypass
        if (player.getSetbackTeleportUtil().safeMovementTicks == 1 && player.predictedVelocity.isJump()
                && Collisions.isEmpty(player, GetBoundingBox.getBoundingBoxFromPosAndSize(player.lastX, player.lastY - 0.03, player.lastZ, 0.66f, 0.06f))) {
            player.getSetbackTeleportUtil().executeForceResync();
        }

        player.lastOnGround = player.onGround;
        player.lastSprinting = player.isSprinting;
        player.lastSprintingForSpeed = player.isSprinting;
        player.wasFlying = player.isFlying;
        player.wasGliding = player.isGliding;
        player.wasSwimming = player.isSwimming;
        player.wasSneaking = player.isSneaking;
        player.tryingToRiptide = false;

        // Don't overwrite packet values
        if (player.inVehicle) {
            player.isFlying = oldFlying;
            player.isGliding = oldGliding;
            player.specialFlying = oldSpecialFlying;
            player.isSprinting = oldSprinting;
            player.isSneaking = oldSneaking;
        }

        player.riptideSpinAttackTicks--;
        if (player.predictedVelocity.isTrident())
            player.riptideSpinAttackTicks = 20;

        player.uncertaintyHandler.lastMovementWasZeroPointZeroThree = !player.inVehicle && player.skippedTickInActualMovement;
        player.uncertaintyHandler.lastMovementWasUnknown003VectorReset = !player.inVehicle && player.couldSkipTick && player.predictedVelocity.isKnockback();
        // Logic is if the player was directly 0.03 and the player could control vertical movement in 0.03
        // Or some state of the player changed, so we can no longer predict this vertical movement
        // Or gravity made the player enter 0.03 movement
        // TODO: This needs to be secured better.  isWasAlwaysCertain() seems like a bit of a hack.
        player.uncertaintyHandler.wasZeroPointThreeVertically = !player.inVehicle &&
                ((player.uncertaintyHandler.lastMovementWasZeroPointZeroThree && player.pointThreeEstimator.controlsVerticalMovement())
                        || !player.pointThreeEstimator.canPredictNextVerticalMovement() || !player.pointThreeEstimator.isWasAlwaysCertain());

        player.uncertaintyHandler.lastLastPacketWasGroundPacket = player.uncertaintyHandler.lastPacketWasGroundPacket;
        player.uncertaintyHandler.lastPacketWasGroundPacket = player.uncertaintyHandler.onGroundUncertain;
        player.uncertaintyHandler.onGroundUncertain = false;

        player.uncertaintyHandler.lastMetadataDesync--;

        player.vehicleData.vehicleForward = (float) Math.min(0.98, Math.max(-0.98, player.vehicleData.nextVehicleForward));
        player.vehicleData.vehicleHorizontal = (float) Math.min(0.98, Math.max(-0.98, player.vehicleData.nextVehicleHorizontal));
        player.vehicleData.horseJump = player.vehicleData.nextHorseJump;
        player.vehicleData.nextHorseJump = 0;

        player.checkManager.getKnockbackHandler().handlePlayerKb(offset);
        player.checkManager.getExplosionHandler().handlePlayerExplosion(offset);
        player.trigHandler.setOffset(oldClientVel, offset);
        player.pointThreeEstimator.endOfTickTick();
    }
}
