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
            // Teleport the player back to avoid players being able to simply ignore transactions
            player.getSetbackTeleportUtil().executeForceResync();
            return;
        }

        long start = System.nanoTime();
        check(data);
        long length = System.nanoTime() - start;

        predictionNanos = (predictionNanos * 499 / 500d) + (length / 500d);
        longPredictionNanos = (longPredictionNanos * 19999 / 20000d) + (length / 20000d);
    }

    public void runTransactionQueue(GrimPlayer player) {
        // Stop OOM
        int lastTransaction = player.lastTransactionReceived.get();
        player.compensatedFlying.canFlyLagCompensated(lastTransaction);
        player.compensatedFireworks.getMaxFireworksAppliedPossible();
        player.compensatedRiptide.getCanRiptide();
    }

    private void check(PositionUpdate update) {
        player.uncertaintyHandler.stuckOnEdge--;
        player.uncertaintyHandler.lastStuckEast--;
        player.uncertaintyHandler.lastStuckWest--;
        player.uncertaintyHandler.lastStuckSouth--;
        player.uncertaintyHandler.lastStuckNorth--;

        if (update.isTeleport()) {
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

            return;
        }

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
        player.firstBreadKB = player.checkManager.getKnockbackHandler().getFirstBreadOnlyKnockback(player.inVehicle ? player.vehicle : player.entityID, player.lastTransactionReceived.get());
        player.likelyKB = player.checkManager.getKnockbackHandler().getRequiredKB(player.inVehicle ? player.vehicle : player.entityID, player.lastTransactionReceived.get());

        player.firstBreadExplosion = player.checkManager.getExplosionHandler().getFirstBreadAddedExplosion(player.lastTransactionReceived.get());
        player.likelyExplosions = player.checkManager.getExplosionHandler().getPossibleExplosions(player.lastTransactionReceived.get());

        // The game's movement is glitchy when switching between vehicles
        // This is due to mojang not telling us where the new vehicle's location is
        // meaning the first move gets hidden... fucking beautiful
        //
        // Exiting vehicles does not suffer the same issue
        // GOD DAMN IT MOJANG WHY DID YOU MAKE VEHICLES CLIENT SIDED IN 1.9?
        // THIS IS MODERN CODE WHY IS IT SO BUGGY
        player.vehicleData.lastVehicleSwitch++;
        if (player.lastVehicle != player.playerVehicle && player.playerVehicle != null) {
            player.vehicleData.lastVehicleSwitch = 0;
        }
        // It is also glitchy when switching between client vs server vehicle control
        if (player.vehicleData.lastDummy) {
            player.vehicleData.lastVehicleSwitch = 0;
        }

        if (player.vehicleData.lastVehicleSwitch < 5) {
            player.checkManager.getExplosionHandler().forceExempt();
            player.checkManager.getKnockbackHandler().forceExempt();
        }

        if (player.lastVehicle != player.playerVehicle || player.vehicleData.lastDummy) {
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

            player.lastX = player.x;
            player.lastY = player.y;
            player.lastZ = player.z;
            player.clientVelocity = new Vector();
        }
        player.vehicleData.lastDummy = false;

        player.lastVehicle = player.playerVehicle;

        if (player.isInBed != player.lastInBed) {
            update.setTeleport(true);
        }
        player.lastInBed = player.isInBed;

        // Teleporting is not a tick, don't run anything that we don't need to, to avoid falses
        player.uncertaintyHandler.lastTeleportTicks--;

        // Don't check sleeping players
        if (player.isInBed) return;

        if (!player.inVehicle) {
            player.speed = player.compensatedEntities.playerEntityMovementSpeed;
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
            player.isSprinting = false;

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
                    if (control.flag()) {
                        player.getSetbackTeleportUtil().executeSetback();
                    }
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

        player.boundingBox = GetBoundingBox.getCollisionBoxForPlayer(player, player.lastX, player.lastY, player.lastZ);
        player.isClimbing = Collisions.onClimbable(player, player.lastX, player.lastY, player.lastZ);
        player.isFlying = player.compensatedFlying.canFlyLagCompensated(player.lastTransactionReceived.get());
        player.specialFlying = player.onGround && !player.isFlying && player.wasFlying || player.isFlying;
        player.isRiptidePose = player.compensatedRiptide.getPose(player.lastTransactionReceived.get());

        player.clientControlledVerticalCollision = Math.abs(player.y % (1 / 64D)) < 0.00001;
        // If you really have nothing better to do, make this support offset blocks like bamboo.  Good luck!
        player.clientControlledHorizontalCollision = Math.min(GrimMath.distanceToHorizontalCollision(player.x), GrimMath.distanceToHorizontalCollision(player.z)) < 1e-6;

        player.uncertaintyHandler.lastSneakingChangeTicks--;
        if (player.isSneaking != player.wasSneaking)
            player.uncertaintyHandler.lastSneakingChangeTicks = 0;

        // This isn't the final velocity of the player in the tick, only the one applied to the player
        player.actualMovement = new Vector(player.x - player.lastX, player.y - player.lastY, player.z - player.lastZ);

        // ViaVersion messes up flight speed for 1.7 players
        if (player.getClientVersion().isOlderThanOrEquals(ClientVersion.V_1_7_10) && player.isFlying)
            player.isSprinting = true;

        // Stop stuff like clients using elytra in a vehicle...
        // Interesting, on a pig or strider, a player can climb a ladder
        if (player.inVehicle) {
            // Reset fall distance when riding
            player.fallDistance = 0;
            player.isFlying = false;
            player.isGliding = false;
            player.specialFlying = false;

            if (player.playerVehicle.type != EntityTypes.PIG && player.playerVehicle.type != EntityTypes.STRIDER) {
                player.isClimbing = false;
            }
        }

        // Multiplying by 1.3 or 1.3f results in precision loss, you must multiply by 0.3
        player.speed += player.isSprinting ? player.speed * 0.3f : 0;

        player.uncertaintyHandler.lastGlidingChangeTicks--;
        if (player.isGliding != player.wasGliding) player.uncertaintyHandler.lastGlidingChangeTicks = 0;


        SimpleCollisionBox steppingOnBB = GetBoundingBox.getCollisionBoxForPlayer(player, player.x, player.y, player.z).expand(0.03).offset(0, -1, 0);
        player.uncertaintyHandler.isSteppingOnSlime = Collisions.hasSlimeBlock(player);
        player.uncertaintyHandler.wasSteppingOnBouncyBlock = player.uncertaintyHandler.isSteppingOnBouncyBlock;
        player.uncertaintyHandler.isSteppingOnBouncyBlock = Collisions.hasBouncyBlock(player);
        player.uncertaintyHandler.isSteppingOnIce = Collisions.hasMaterial(player, steppingOnBB, type -> BlockTags.ICE.contains(type.getType()));
        player.uncertaintyHandler.isSteppingOnHoney = Collisions.hasMaterial(player, StateTypes.HONEY_BLOCK, -0.03);
        player.uncertaintyHandler.isSteppingNearBubbleColumn = player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_13) && Collisions.hasMaterial(player, StateTypes.BUBBLE_COLUMN, -1);

        // Update firework end/start uncertainty
        player.uncertaintyHandler.lastFireworkStatusChange--;
        boolean hasFirework = (player.isGliding || player.wasGliding) && player.compensatedFireworks.getMaxFireworksAppliedPossible() > 0;
        if (hasFirework != player.uncertaintyHandler.lastUsingFirework)
            player.uncertaintyHandler.lastFireworkStatusChange = 0;
        player.uncertaintyHandler.lastUsingFirework = hasFirework;

        SimpleCollisionBox expandedBB = GetBoundingBox.getBoundingBoxFromPosAndSize(player.lastX, player.lastY, player.lastZ, 0.001, 0.001);

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
        boolean couldLeaveStuckSpeed = Collisions.checkStuckSpeed(player, -0.03);
        player.uncertaintyHandler.claimingLeftStuckSpeed = player.stuckSpeedMultiplier.getX() < 1 && !couldLeaveStuckSpeed;

        if (couldBeStuckSpeed) {
            player.uncertaintyHandler.lastStuckSpeedMultiplier = 0;
        }

        Vector backOff = Collisions.maybeBackOffFromEdge(player.clientVelocity, player, true);
        player.uncertaintyHandler.nextTickScaffoldingOnEdge = player.clientVelocity.getX() != 0 && player.clientVelocity.getZ() != 0 && backOff.getX() == 0 && backOff.getZ() == 0;
        player.canGroundRiptide = false;
        Vector oldClientVel = player.clientVelocity;

        boolean wasChecked = false;

        // Exempt if the player is offline
        if (player.isDead || (player.playerVehicle != null && player.playerVehicle.isDead)) {
            // Dead players can't cheat, if you find a way how they could, open an issue
            player.predictedVelocity = new VectorData(player.actualMovement, VectorData.VectorType.Dead);
            player.clientVelocity = new Vector();
        } else if ((PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_8) && player.gamemode == GameMode.SPECTATOR) || player.specialFlying) {
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

            // Now that we have all the world updates, recalculate if the player is near the ground
            player.uncertaintyHandler.lastTickWasNearGroundZeroPointZeroThree = !Collisions.isEmpty(player, player.boundingBox.copy().expand(0.03, 0, 0.03).offset(0, -0.03, 0));

            // This is wrong and the engine was not designed around stuff like this
            player.canGroundRiptide = ((player.clientClaimsLastOnGround && player.uncertaintyHandler.lastTickWasNearGroundZeroPointZeroThree)
                    || (player.uncertaintyHandler.isSteppingOnSlime && player.uncertaintyHandler.lastTickWasNearGroundZeroPointZeroThree))
                    && player.tryingToRiptide && player.compensatedRiptide.getCanRiptide() && !player.inVehicle;
            player.verticalCollision = false;

            // Riptiding while on the ground moves the hitbox upwards before any movement code runs
            // It's a pain to support and this is my best attempt
            if (player.canGroundRiptide) {
                Vector pushingMovement = Collisions.collide(player, 0, 1.1999999F, 0);
                player.verticalCollision = pushingMovement.getY() != 1.1999999F;
                double currentY = player.clientVelocity.getY();
                player.uncertaintyHandler.slimeBlockUpwardsUncertainty.add(Math.abs(Riptide.getRiptideVelocity(player).getY()) + (currentY > 0 ? currentY : 0));

                // If the player was very likely to have used riptide on the ground
                // (Patches issues with slime and other desync's)
                if (likelyGroundRiptide(pushingMovement)) {
                    player.lastOnGround = false;
                    player.boundingBox.offset(0, pushingMovement.getY(), 0);
                    player.lastY += pushingMovement.getY();
                    player.actualMovement = new Vector(player.x - player.lastX, player.y - player.lastY, player.z - player.lastZ);

                    Collisions.handleInsideBlocks(player);
                }
            } else {
                if (player.uncertaintyHandler.influencedByBouncyBlock()) { // Slime
                    player.uncertaintyHandler.slimeBlockUpwardsUncertainty.add(player.clientVelocity.getY());
                } else {
                    player.uncertaintyHandler.slimeBlockUpwardsUncertainty.add(0d);
                }
            }

            new PlayerBaseTick(player).doBaseTick();
            new MovementTickerPlayer(player).livingEntityAIStep();
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

        // If the player is trying to riptide
        // But the server has rejected this movement
        // And there isn't water nearby (tries to solve most vanilla issues with this desync)
        //
        // Set back the player to disallow them to use riptide anywhere, even outside rain or water
        if (player.tryingToRiptide != player.compensatedRiptide.getCanRiptide() &&
                player.predictedVelocity.isTrident() &&
                // Don't let player do this too often as otherwise it could allow players to spam riptide
                (player.riptideSpinAttackTicks < 0 && !player.compensatedWorld.containsWater(GetBoundingBox.getCollisionBoxForPlayer(player, player.lastX, player.lastY, player.lastZ).expand(0.3, 0.3, 0.3)))) {
            player.getSetbackTeleportUtil().executeForceResync();
        }

        // Let's hope this doesn't desync :)
        if (player.getSetbackTeleportUtil().blockOffsets)
            offset = 0;

        // Don't check players who are offline
        if (!player.bukkitPlayer.isOnline()) return;
        // Don't check players who just switched worlds
        if (player.playerWorld != player.bukkitPlayer.getWorld()) return;

        if (wasChecked) {
            // We shouldn't attempt to send this prediction analysis into checks if we didn't predict anything
            player.checkManager.onPredictionFinish(new PredictionComplete(offset, update));
        } else {
            // The player wasn't checked, explosion and knockback status unknown
            player.checkManager.getExplosionHandler().forceExempt();
            player.checkManager.getKnockbackHandler().forceExempt();
        }

        player.lastOnGround = player.onGround;
        player.lastSprinting = player.isSprinting;
        player.lastSprintingForSpeed = player.isSprinting;
        player.wasFlying = player.isFlying;
        player.wasGliding = player.isGliding;
        player.wasSwimming = player.isSwimming;
        player.wasSneaking = player.isSneaking;

        player.riptideSpinAttackTicks--;
        if (player.predictedVelocity.isTrident())
            player.riptideSpinAttackTicks = 20;

        player.uncertaintyHandler.lastMovementWasZeroPointZeroThree = player.skippedTickInActualMovement;
        player.uncertaintyHandler.lastMovementWasUnknown003VectorReset = player.couldSkipTick && player.predictedVelocity.isKnockback();
        // Logic is if the player was directly 0.03 and the player could control vertical movement in 0.03
        // Or some state of the player changed, so we can no longer predict this vertical movement
        // Or gravity made the player enter 0.03 movement
        player.uncertaintyHandler.wasZeroPointThreeVertically = (player.uncertaintyHandler.lastMovementWasZeroPointZeroThree && player.pointThreeEstimator.controlsVerticalMovement())
                || !player.pointThreeEstimator.canPredictNextVerticalMovement() || !player.pointThreeEstimator.isWasAlwaysCertain();

        player.uncertaintyHandler.lastLastPacketWasGroundPacket = player.uncertaintyHandler.lastPacketWasGroundPacket;
        player.uncertaintyHandler.lastPacketWasGroundPacket = player.uncertaintyHandler.onGroundUncertain;
        player.uncertaintyHandler.onGroundUncertain = false;

        player.uncertaintyHandler.lastMetadataDesync--;

        player.vehicleData.vehicleForward = (float) Math.min(0.98, Math.max(-0.98, player.vehicleData.nextVehicleForward));
        player.vehicleData.vehicleHorizontal = (float) Math.min(0.98, Math.max(-0.98, player.vehicleData.nextVehicleHorizontal));
        player.vehicleData.horseJump = player.vehicleData.nextHorseJump;

        player.checkManager.getKnockbackHandler().handlePlayerKb(offset);
        player.checkManager.getExplosionHandler().handlePlayerExplosion(offset);
        player.trigHandler.setOffset(oldClientVel, offset);
        player.compensatedRiptide.handleRemoveRiptide();
        player.pointThreeEstimator.endOfTickTick();
    }

    /**
     * Computes the movement from the riptide, and then uses it to determine whether the player
     * was more likely to be on or off of the ground when they started to riptide
     * <p>
     * A player on ground when riptiding will move upwards by 1.2f
     * We don't know whether the player was on the ground, however, which is why
     * we must attempt to guess here
     * <p>
     * Very reliable.
     *
     * @param pushingMovement The collision result when trying to move the player upwards by 1.2f
     * @return Whether it is more likely that this player was on the ground the tick they riptided
     */
    private boolean likelyGroundRiptide(Vector pushingMovement) {
        // Y velocity gets reset if the player collides vertically
        double riptideYResult = Riptide.getRiptideVelocity(player).getY();

        double riptideDiffToBase = Math.abs(player.actualMovement.getY() - riptideYResult);
        double riptideDiffToGround = Math.abs(player.actualMovement.getY() - riptideYResult - pushingMovement.getY());

        // If the player was very likely to have used riptide on the ground
        // (Patches issues with slime and other desync's)
        return riptideDiffToGround < riptideDiffToBase;
    }
}
