package ac.grim.grimac.predictionengine;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.impl.movement.EntityControl;
import ac.grim.grimac.checks.impl.prediction.Phase;
import ac.grim.grimac.checks.type.PositionCheck;
import ac.grim.grimac.manager.SetbackTeleportUtil;
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
import ac.grim.grimac.utils.data.packetentity.PacketEntityTrackXRot;
import ac.grim.grimac.utils.enums.Pose;
import ac.grim.grimac.utils.latency.CompensatedWorld;
import ac.grim.grimac.utils.math.GrimMath;
import ac.grim.grimac.utils.math.VectorUtils;
import ac.grim.grimac.utils.nmsutil.*;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.item.enchantment.type.EnchantmentTypes;
import com.github.retrooper.packetevents.protocol.item.type.ItemType;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.protocol.world.states.defaulttags.BlockTags;
import com.github.retrooper.packetevents.protocol.world.states.type.StateTypes;
import org.bukkit.util.Vector;

public class MovementCheckRunner extends Check implements PositionCheck {
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
            player.lastOnGround = player.clientClaimsLastOnGround; // Stop a false on join

            // The player doesn't control this vehicle, we don't care
            if (player.compensatedEntities.getSelf().inVehicle() &&
                    (PacketEvents.getAPI().getServerManager().getVersion().isOlderThan(ServerVersion.V_1_9) ||
                    player.getClientVersion().isOlderThan(ClientVersion.V_1_9))) {
                return;
            }

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

        // Reset velocities
        // Teleporting a vehicle does not reset its velocity
        if (!player.compensatedEntities.getSelf().inVehicle()) {
            if (update.getTeleportData() == null || !update.getTeleportData().isRelativeX()) {
                player.clientVelocity.setX(0);
            }
            if (update.getTeleportData() == null || !update.getTeleportData().isRelativeY()) {
                player.clientVelocity.setY(0);
                player.lastWasClimbing = 0; // Vertical movement reset
                player.canSwimHop = false; // Vertical movement reset
            }
            if (update.getTeleportData() == null || !update.getTeleportData().isRelativeZ()) {
                player.clientVelocity.setZ(0);
            }
        }

        player.uncertaintyHandler.lastTeleportTicks.reset();

        // Teleports OVERRIDE explosions and knockback
        player.checkManager.getExplosionHandler().forceExempt();
        player.checkManager.getKnockbackHandler().forceExempt();

        player.boundingBox = GetBoundingBox.getCollisionBoxForPlayer(player, player.x, player.y, player.z);

        // Manually call prediction complete to handle teleport
        PredictionComplete predictionComplete = new PredictionComplete(0, update, true);
        player.getSetbackTeleportUtil().onPredictionComplete(predictionComplete);
        player.checkManager.getPostPredictionCheck(Phase.class).onPredictionComplete(predictionComplete);

        player.uncertaintyHandler.lastHorizontalOffset = 0;
        player.uncertaintyHandler.lastVerticalOffset = 0;
    }

    private void check(PositionUpdate update) {
        if (update.isTeleport()) {
            handleTeleport(update);
            return;
        }

        player.movementPackets++;

        player.onGround = update.isOnGround();

        // This is here to prevent abuse of sneaking
        // Without this, players could sneak on a flat plane to avoid velocity
        // That would be bad so this prevents it
        if (!player.isFlying && player.isSneaking && Collisions.isAboveGround(player)) {
            // 16 - Magic number to stop people from crashing the server
            // 0.05 - Mojang's magic value that they use to calculate precision of sneaking
            // They move the position back by 0.05 blocks repeatedly until they are above ground
            // So by going forwards 0.05 blocks, we can determine if the player was influenced by this
            double posX = Math.max(0.05, GrimMath.clamp(player.actualMovement.getX(), -16, 16) + 0.05);
            double posZ = Math.max(0.05, GrimMath.clamp(player.actualMovement.getZ(), -16, 16) + 0.05);
            double negX = Math.min(-0.05, GrimMath.clamp(player.actualMovement.getX(), -16, 16) - 0.05);
            double negZ = Math.min(-0.05, GrimMath.clamp(player.actualMovement.getZ(), -16, 16) - 0.05);

            Vector NE = Collisions.maybeBackOffFromEdge(new Vector(posX, 0, negZ), player, true);
            Vector NW = Collisions.maybeBackOffFromEdge(new Vector(negX, 0, negZ), player, true);
            Vector SE = Collisions.maybeBackOffFromEdge(new Vector(posX, 0, posZ), player, true);
            Vector SW = Collisions.maybeBackOffFromEdge(new Vector(negX, 0, posZ), player, true);

            boolean isEast = NE.getX() != posX || SE.getX() != posX;
            boolean isWest = NW.getX() != negX || SW.getX() != negX;
            boolean isNorth = NE.getZ() != negZ || NW.getZ() != negZ;
            boolean isSouth = SE.getZ() != posZ || SW.getZ() != posZ;

            if (isEast) player.uncertaintyHandler.lastStuckEast.reset();
            if (isWest) player.uncertaintyHandler.lastStuckWest.reset();
            if (isNorth) player.uncertaintyHandler.lastStuckNorth.reset();
            if (isSouth) player.uncertaintyHandler.lastStuckSouth.reset();

            if (isEast || isWest || isSouth || isNorth) {
                player.uncertaintyHandler.stuckOnEdge.reset();
            }
        }

        player.compensatedWorld.tickPlayerInPistonPushingArea();
        player.compensatedEntities.tick();

        // The game's movement is glitchy when switching between vehicles
        // This is due to mojang not telling us where the new vehicle's location is
        // meaning the first move gets hidden... beautiful
        //
        // Exiting vehicles does not suffer the same issue
        //
        // It is also glitchy when switching between client vs server vehicle control
        if (player.vehicleData.wasVehicleSwitch || player.vehicleData.lastDummy) {
            player.uncertaintyHandler.lastVehicleSwitch.reset();
        }

        if (player.vehicleData.lastDummy) {
            player.clientVelocity.multiply(0.98); // This is vanilla, do not touch
        }

        if (player.vehicleData.wasVehicleSwitch || player.vehicleData.lastDummy) {
            update.setTeleport(true);

            player.vehicleData.lastDummy = false;
            player.vehicleData.wasVehicleSwitch = false;

            if (player.compensatedEntities.getSelf().getRiding() != null) {
                Vector pos = new Vector(player.x, player.y, player.z);
                SimpleCollisionBox interTruePositions = player.compensatedEntities.getSelf().getRiding().getPossibleCollisionBoxes();

                // We shrink the expanded bounding box to what the packet positions can be, for a smaller box
                float width = BoundingBoxSize.getWidth(player, player.compensatedEntities.getSelf().getRiding());
                float height = BoundingBoxSize.getHeight(player, player.compensatedEntities.getSelf().getRiding());
                interTruePositions.expand(-width, 0, -width);
                interTruePositions.expandMax(0, -height, 0);

                Vector cutTo = VectorUtils.cutBoxToVector(pos, interTruePositions);

                // Now we need to simulate a tick starting at the most optimal position
                // The start position is never sent, so we assume the most optimal start position
                //
                // Value patching this is not allowed.
                // NoCheatPlus suffers from this type of exploit, so attacks against
                // their similar code may also work on grim.
                //
                // This is the best I can do, but I think it might just work.
                player.lastX = cutTo.getX();
                player.lastY = cutTo.getY();
                player.lastZ = cutTo.getZ();

                player.boundingBox = GetBoundingBox.getCollisionBoxForPlayer(player, player.lastX, player.lastY, player.lastZ);
            } else {
                // Server always teleports the player when they eject anyways,
                // so just let the player control where they eject within reason, they get set back anyways
                if (new Vector(player.lastX, player.lastY, player.lastZ).distance(new Vector(player.x, player.y, player.z)) > 3) {
                    player.getSetbackTeleportUtil().executeForceResync(); // Too far! (I think this value is sane)
                }

                handleTeleport(update);

                if (player.isClimbing) {
                    Vector ladder = player.clientVelocity.clone().setY(0.2);
                    PredictionEngineNormal.staticVectorEndOfTick(player, ladder);
                    player.lastWasClimbing = ladder.getY();
                }
                return;
            }
        }

        if (player.isInBed != player.lastInBed) {
            update.setTeleport(true);
        }
        player.lastInBed = player.isInBed;

        // Don't check sleeping players
        if (player.isInBed) return;

        if (!player.compensatedEntities.getSelf().inVehicle()) {
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
        if (player.compensatedEntities.getSelf().inVehicle()) {
            // Players are unable to take explosions in vehicles
            player.checkManager.getExplosionHandler().forceExempt();

            // When in control of the entity, the player sets the entity position to their current position
            player.compensatedEntities.getSelf().getRiding().setPositionRaw(GetBoundingBox.getPacketEntityBoundingBox(player, player.x, player.y, player.z, player.compensatedEntities.getSelf().getRiding()));

            if (player.compensatedEntities.getSelf().getRiding() instanceof PacketEntityTrackXRot) {
                PacketEntityTrackXRot boat = (PacketEntityTrackXRot) player.compensatedEntities.getSelf().getRiding();
                boat.packetYaw = player.xRot;
                boat.interpYaw = player.xRot;
                boat.steps = 0;
            }

            if (player.hasGravity != player.compensatedEntities.getSelf().getRiding().hasGravity) {
                player.pointThreeEstimator.updatePlayerGravity();
            }
            player.hasGravity = player.compensatedEntities.getSelf().getRiding().hasGravity;

            // For whatever reason the vehicle move packet occurs AFTER the player changes slots...
            if (player.compensatedEntities.getSelf().getRiding() instanceof PacketEntityRideable) {
                EntityControl control = player.checkManager.getPostPredictionCheck(EntityControl.class);

                ItemType requiredItem = player.compensatedEntities.getSelf().getRiding().type == EntityTypes.PIG ? ItemTypes.CARROT_ON_A_STICK : ItemTypes.WARPED_FUNGUS_ON_A_STICK;
                ItemStack mainHand = player.getInventory().getHeldItem();
                ItemStack offHand = player.getInventory().getOffHand();

                boolean correctMainHand = mainHand.getType() == requiredItem;
                boolean correctOffhand = offHand.getType() == requiredItem;

                if (!correctMainHand && !correctOffhand) {
                    // Entity control cheats!  Set the player back
                    control.flagAndAlert();
                } else {
                    control.rewardPlayer();
                }
            }
        }

        if (player.isFlying) {
            player.fallDistance = 0;
            player.uncertaintyHandler.lastFlyingTicks.reset();
        }

        player.isClimbing = Collisions.onClimbable(player, player.lastX, player.lastY, player.lastZ);

        player.clientControlledVerticalCollision = Math.abs(player.y % (1 / 64D)) < 0.00001;

        // This isn't the final velocity of the player in the tick, only the one applied to the player
        player.actualMovement = new Vector(player.x - player.lastX, player.y - player.lastY, player.z - player.lastZ);

        if (player.isSprinting != player.lastSprinting) {
            player.compensatedEntities.hasSprintingAttributeEnabled = player.isSprinting;
        }

        boolean oldFlying = player.isFlying;
        boolean oldGliding = player.isGliding;
        boolean oldSprinting = player.isSprinting;
        boolean oldSneaking = player.isSneaking;

        // Stop stuff like clients using elytra in a vehicle...
        // Interesting, on a pig or strider, a player can climb a ladder
        if (player.compensatedEntities.getSelf().inVehicle()) {
            // Reset fall distance when riding
            //player.fallDistance = 0;
            player.isFlying = false;
            player.isGliding = false;
            player.isSprinting = false;
            player.isSneaking = false;

            if (player.compensatedEntities.getSelf().getRiding().type != EntityTypes.PIG && player.compensatedEntities.getSelf().getRiding().type != EntityTypes.STRIDER) {
                player.isClimbing = false;
            }
        }

        // Multiplying by 1.3 or 1.3f results in precision loss, you must multiply by 0.3
        // The player updates their attribute if it doesn't match the last value
        // This last value can be changed by the server, however.
        //
        // Sprinting status itself does not desync, only the attribute as mojang forgot that the server
        // can change the attribute
        if (!player.compensatedEntities.getSelf().inVehicle()) {
            player.speed += player.compensatedEntities.hasSprintingAttributeEnabled ? player.speed * 0.3f : 0;
        }

        boolean clientClaimsRiptide = player.packetStateData.tryingToRiptide;
        if (player.packetStateData.tryingToRiptide) {
            long currentTime = System.currentTimeMillis();
            boolean isInWater = player.compensatedWorld.isRaining || Collisions.hasMaterial(player, player.boundingBox.copy().expand(0.1f), (block) -> Materials.isWater(CompensatedWorld.blockVersion, block.getFirst()));

            if (currentTime - player.packetStateData.lastRiptide < 450 || !isInWater) {
                player.packetStateData.tryingToRiptide = false;
            }

            player.packetStateData.lastRiptide = currentTime;
        }

        SimpleCollisionBox steppingOnBB = GetBoundingBox.getCollisionBoxForPlayer(player, player.x, player.y, player.z).expand(0.03).offset(0, -1, 0);
        Collisions.hasMaterial(player, steppingOnBB, (pair) -> {
            WrappedBlockState data = pair.getFirst();
            if (data.getType() == StateTypes.SLIME_BLOCK && player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_8)) {
                player.uncertaintyHandler.isSteppingOnSlime = true;
                player.uncertaintyHandler.isSteppingOnBouncyBlock = true;
            }
            if (data.getType() == StateTypes.HONEY_BLOCK) {
                if (player.getClientVersion().isOlderThanOrEquals(ClientVersion.V_1_14)
                        && player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_8)) {
                    player.uncertaintyHandler.isSteppingOnBouncyBlock = true;
                }
                player.uncertaintyHandler.isSteppingOnHoney = true;
            }
            if (BlockTags.BEDS.contains(data.getType()) && player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_8)) {
                player.uncertaintyHandler.isSteppingOnBouncyBlock = true;
            }
            if (BlockTags.ICE.contains(data.getType())) {
                player.uncertaintyHandler.isSteppingOnIce = true;
            }
            if (data.getType() == StateTypes.BUBBLE_COLUMN) {
                player.uncertaintyHandler.isSteppingNearBubbleColumn = true;
            }
            if (data.getType() == StateTypes.SCAFFOLDING) {
                player.uncertaintyHandler.isSteppingNearScaffolding = true;
            }
            return false;
        });

        player.uncertaintyHandler.thisTickSlimeBlockUncertainty = player.uncertaintyHandler.nextTickSlimeBlockUncertainty;
        player.uncertaintyHandler.nextTickSlimeBlockUncertainty = 0;

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
                && Collisions.hasMaterial(player, expandedBB.copy().expand(0.2),
                checkData -> BlockTags.ANVIL.contains(checkData.getFirst().getType())
                        || checkData.getFirst().getType() == StateTypes.CHEST || checkData.getFirst().getType() == StateTypes.TRAPPED_CHEST);

        player.uncertaintyHandler.isOrWasNearGlitchyBlock = isGlitchy || player.uncertaintyHandler.isNearGlitchyBlock;
        player.uncertaintyHandler.checkForHardCollision();

        if (player.isFlying != player.wasFlying) player.uncertaintyHandler.lastFlyingStatusChange.reset();

        if (!player.compensatedEntities.getSelf().inVehicle() && (Math.abs(player.x) == 2.9999999E7D || Math.abs(player.z) == 2.9999999E7D)) {
            player.uncertaintyHandler.lastThirtyMillionHardBorder.reset();
        }

        if (player.isFlying && player.getClientVersion().isOlderThan(ClientVersion.V_1_13) && player.compensatedWorld.containsLiquid(player.boundingBox)) {
            player.uncertaintyHandler.lastUnderwaterFlyingHack.reset();
        }

        boolean couldBeStuckSpeed = Collisions.checkStuckSpeed(player, 0.03);
        boolean couldLeaveStuckSpeed = player.isPointThree() && Collisions.checkStuckSpeed(player, -0.03);
        player.uncertaintyHandler.claimingLeftStuckSpeed = !player.compensatedEntities.getSelf().inVehicle() && player.stuckSpeedMultiplier.getX() < 1 && !couldLeaveStuckSpeed;

        if (couldBeStuckSpeed) {
            player.uncertaintyHandler.lastStuckSpeedMultiplier.reset();
        }

        player.startTickClientVel = player.clientVelocity;

        boolean wasChecked = false;

        // Exempt if the player is dead or is riding a dead entity
        if (player.compensatedEntities.getSelf().isDead || (player.compensatedEntities.getSelf().getRiding() != null && player.compensatedEntities.getSelf().getRiding().isDead)) {
            // Dead players can't cheat, if you find a way how they could, open an issue
            player.predictedVelocity = new VectorData(new Vector(), VectorData.VectorType.Dead);
            player.clientVelocity = new Vector();
        } else if (player.disableGrim || (PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_8) && player.gamemode == GameMode.SPECTATOR) || player.isFlying) {
            // We could technically check spectator but what's the point...
            // Added complexity to analyze a gamemode used mainly by moderators
            //
            // TODO: Re-implement flying support, although LUNAR HAS FLYING CHEATS!!! HOW CAN I CHECK WHEN HALF THE PLAYER BASE IS USING CHEATS???
            player.predictedVelocity = new VectorData(player.actualMovement, VectorData.VectorType.Spectator);
            player.clientVelocity = player.actualMovement.clone();
            player.gravity = 0;
            player.friction = 0.91f;
            PredictionEngineNormal.staticVectorEndOfTick(player, player.clientVelocity);
        } else if (player.compensatedEntities.getSelf().getRiding() == null) {
            wasChecked = true;

            // Depth strider was added in 1.8
            ItemStack boots = player.getInventory().getBoots();
            if (player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_8)) {
                player.depthStriderLevel = boots.getEnchantmentLevel(EnchantmentTypes.DEPTH_STRIDER, PacketEvents.getAPI().getServerManager().getVersion().toClientVersion());
            } else {
                player.depthStriderLevel = 0;
            }

            if (player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_19)) {
                ItemStack leggings = player.getInventory().getLeggings();
                player.sneakingSpeedMultiplier = GrimMath.clampFloat(0.3F + (leggings.getEnchantmentLevel(EnchantmentTypes.SWIFT_SNEAK, player.getClientVersion()) * 0.15F), 0f, 1f);
            } else {
                player.sneakingSpeedMultiplier = 0.3F;
            }

            // This is wrong and the engine was not designed around stuff like this
            player.verticalCollision = false;

            // Riptiding while on the ground moves the hitbox upwards before any movement code runs
            // It's a pain to support and this is my best attempt
            if (player.lastOnGround && player.packetStateData.tryingToRiptide && !player.compensatedEntities.getSelf().inVehicle()) {
                Vector pushingMovement = Collisions.collide(player, 0, 1.1999999F, 0);
                player.verticalCollision = pushingMovement.getY() != 1.1999999F;
                double currentY = player.clientVelocity.getY();

                if (likelyGroundRiptide(pushingMovement)) {
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
            if (EntityTypes.isTypeInstanceOf(player.compensatedEntities.getSelf().getRiding().type, EntityTypes.BOAT)) {
                new PlayerBaseTick(player).doBaseTick();
                // Speed doesn't affect anything with boat movement
                new BoatPredictionEngine(player).guessBestMovement(0.1f, player);
            } else if (player.compensatedEntities.getSelf().getRiding() instanceof PacketEntityHorse) {
                new PlayerBaseTick(player).doBaseTick();
                new MovementTickerHorse(player).livingEntityAIStep();
            } else if (player.compensatedEntities.getSelf().getRiding().type == EntityTypes.PIG) {
                new PlayerBaseTick(player).doBaseTick();
                new MovementTickerPig(player).livingEntityAIStep();
            } else if (player.compensatedEntities.getSelf().getRiding().type == EntityTypes.STRIDER) {
                new PlayerBaseTick(player).doBaseTick();
                new MovementTickerStrider(player).livingEntityAIStep();
                MovementTickerStrider.floatStrider(player);
                Collisions.handleInsideBlocks(player);
            } else {
                wasChecked = false;
            }
        } // If it isn't any of these cases, the player is on a mob they can't control and therefore is exempt

        // No, don't comment about the sqrt call.  It doesn't matter unless you run sqrt thousands of times a second.
        double offset = player.predictedVelocity.vector.distance(player.actualMovement);
        offset = player.uncertaintyHandler.reduceOffset(offset);

        if (player.packetStateData.tryingToRiptide != clientClaimsRiptide) {
            player.getSetbackTeleportUtil().executeForceResync(); // Could technically be lag due to packet timings.
        }

        // If the player is abusing a setback in order to gain the onGround status of true.
        // and the player then jumps from this position in the air.
        // Fixes LiquidBounce Jesus NCP, and theoretically AirJump bypass
        //
        // Checking for oldClientVel being too high fixes BleachHack vertical scaffold
        if (player.getSetbackTeleportUtil().getRequiredSetBack() != null && player.getSetbackTeleportUtil().getRequiredSetBack().getTicksComplete() == 1) {
            Vector setbackVel = player.getSetbackTeleportUtil().getRequiredSetBack().getVelocity();
            // A player must have velocity going INTO the ground to be able to jump
            // Otherwise they could ignore upwards velocity that isn't useful into more useful upwards velocity (towering)
            // So if they are supposed to be going upwards, or are supposed to be off the ground, resync
            if (player.predictedVelocity.isJump() && ((setbackVel != null && setbackVel.getY() >= 0) || !Collisions.slowCouldPointThreeHitGround(player, player.lastX, player.lastY, player.lastZ))) {
                player.getSetbackTeleportUtil().executeForceResync();
            }
            // Player ignored the knockback or is delaying it a tick... bad!
            if (!player.predictedVelocity.isKnockback() && player.getSetbackTeleportUtil().getRequiredSetBack().getVelocity() != null) {
                // And then send it again!
                player.getSetbackTeleportUtil().executeForceResync();
            }
        }

        // Let's hope this doesn't desync :)
        if (player.getSetbackTeleportUtil().blockOffsets) offset = 0;

        if (player.skippedTickInActualMovement || !wasChecked) player.uncertaintyHandler.lastPointThree.reset();

        // We shouldn't attempt to send this prediction analysis into checks if we didn't predict anything
        player.checkManager.onPredictionFinish(new PredictionComplete(offset, update, wasChecked));

        // Patch sprint jumping with elytra exploit
        if (player.bukkitPlayer != null && player.isGliding && player.predictedVelocity.isJump() && player.isSprinting
                && !GrimAPI.INSTANCE.getConfigManager().getConfig().getBooleanElse("exploit.allow-sprint-jumping-when-using-elytra", true)) {
            SetbackTeleportUtil.SetbackPosWithVector lastKnownGoodPosition = player.getSetbackTeleportUtil().lastKnownGoodPosition;
            lastKnownGoodPosition.setVector(lastKnownGoodPosition.getVector().multiply(new Vector(0.6 * 0.91, 1, 0.6 * 0.91)));
            player.getSetbackTeleportUtil().executeNonSimulatingSetback();
        }

        if (!wasChecked) {
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
        player.packetStateData.tryingToRiptide = false;

        // Don't overwrite packet values
        if (player.compensatedEntities.getSelf().inVehicle()) {
            player.isFlying = oldFlying;
            player.isGliding = oldGliding;
            player.isSprinting = oldSprinting;
            player.isSneaking = oldSneaking;
        }

        player.riptideSpinAttackTicks--;
        if (player.predictedVelocity.isTrident())
            player.riptideSpinAttackTicks = 20;

        player.uncertaintyHandler.lastMovementWasZeroPointZeroThree = !player.compensatedEntities.getSelf().inVehicle() && player.skippedTickInActualMovement;
        player.uncertaintyHandler.lastMovementWasUnknown003VectorReset = !player.compensatedEntities.getSelf().inVehicle() && player.couldSkipTick && player.predictedVelocity.isKnockback();
        player.couldSkipTick = false;

        // Logic is if the player was directly 0.03 and the player could control vertical movement in 0.03
        // Or some state of the player changed, so we can no longer predict this vertical movement
        // Or gravity made the player enter 0.03 movement
        // TODO: This needs to be secured better.  isWasAlwaysCertain() seems like a bit of a hack.
        player.uncertaintyHandler.wasZeroPointThreeVertically = !player.compensatedEntities.getSelf().inVehicle() &&
                ((player.uncertaintyHandler.lastMovementWasZeroPointZeroThree && player.pointThreeEstimator.controlsVerticalMovement())
                        || !player.pointThreeEstimator.canPredictNextVerticalMovement() || !player.pointThreeEstimator.isWasAlwaysCertain());

        player.uncertaintyHandler.lastPacketWasGroundPacket = player.uncertaintyHandler.onGroundUncertain;
        player.uncertaintyHandler.onGroundUncertain = false;

        player.vehicleData.vehicleForward = (float) Math.min(0.98, Math.max(-0.98, player.vehicleData.nextVehicleForward));
        player.vehicleData.vehicleHorizontal = (float) Math.min(0.98, Math.max(-0.98, player.vehicleData.nextVehicleHorizontal));
        player.vehicleData.horseJump = player.vehicleData.nextHorseJump;
        player.vehicleData.nextHorseJump = 0;
        player.minPlayerAttackSlow = 0;
        player.maxPlayerAttackSlow = 0;

        player.likelyKB = null;
        player.firstBreadKB = null;
        player.firstBreadExplosion = null;
        player.likelyExplosions = null;

        player.trigHandler.setOffset(offset);
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
