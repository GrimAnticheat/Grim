package ac.grim.grimac.predictionengine;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.checks.type.PositionCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.predictionengine.movementtick.MovementTickerHorse;
import ac.grim.grimac.predictionengine.movementtick.MovementTickerPig;
import ac.grim.grimac.predictionengine.movementtick.MovementTickerPlayer;
import ac.grim.grimac.predictionengine.movementtick.MovementTickerStrider;
import ac.grim.grimac.predictionengine.predictions.PredictionEngineNormal;
import ac.grim.grimac.predictionengine.predictions.rideable.BoatPredictionEngine;
import ac.grim.grimac.utils.anticheat.LogUtil;
import ac.grim.grimac.utils.anticheat.update.PredictionComplete;
import ac.grim.grimac.utils.chunks.Column;
import ac.grim.grimac.utils.collisions.datatypes.SimpleCollisionBox;
import ac.grim.grimac.utils.data.AlmostBoolean;
import ac.grim.grimac.utils.data.PredictionData;
import ac.grim.grimac.utils.data.VectorData;
import ac.grim.grimac.utils.data.packetentity.PacketEntityHorse;
import ac.grim.grimac.utils.data.packetentity.PacketEntityRideable;
import ac.grim.grimac.utils.enums.EntityType;
import ac.grim.grimac.utils.math.GrimMath;
import ac.grim.grimac.utils.nmsImplementations.*;
import ac.grim.grimac.utils.threads.CustomThreadPoolExecutor;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.github.retrooper.packetevents.utils.player.ClientVersion;
import io.github.retrooper.packetevents.utils.server.ServerVersion;
import io.github.retrooper.packetevents.utils.vector.Vector3d;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

// This class is how we manage to safely do everything async
// AtomicInteger allows us to make decisions safely - we can get and set values in one processor instruction
// This is the meaning of GrimPlayer.tasksNotFinished
// Stage 0 - All work is done
// Stage 1 - There is more work, number = number of jobs in the queue and running
//
// After finishing doing the predictions:
// If stage 0 - Do nothing
// If stage 1 - Subtract by 1, and add another to the queue
//
// When the player sends a packet and we have to add him to the queue:
// If stage 0 - Add one and add the data to the workers
// If stage 1 - Add the data to the queue and add one
public class MovementCheckRunner extends PositionCheck {
    private static final Material CARROT_ON_A_STICK = XMaterial.CARROT_ON_A_STICK.parseMaterial();
    private static final Material WARPED_FUNGUS_ON_A_STICK = XMaterial.WARPED_FUNGUS_ON_A_STICK.parseMaterial();
    private static final Material BUBBLE_COLUMN = XMaterial.BUBBLE_COLUMN.parseMaterial();

    private static final Material TRIDENT = XMaterial.TRIDENT.parseMaterial();

    public static CustomThreadPoolExecutor executor =
            new CustomThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS,
                    new LinkedBlockingQueue<>(), new ThreadFactoryBuilder().setDaemon(true).build());
    public static ConcurrentLinkedQueue<PredictionData> waitingOnServerQueue = new ConcurrentLinkedQueue<>();

    public MovementCheckRunner(GrimPlayer player) {
        super(player);
    }

    public void processAndCheckMovementPacket(PredictionData data) {
        Column column = data.player.compensatedWorld.getChunk(GrimMath.floor(data.playerX) >> 4, GrimMath.floor(data.playerZ) >> 4);

        // The player is in an unloaded chunk
        if (!data.isJustTeleported && column == null) {
            data.player.nextTaskToRun = null;
            return;
        }
        // The player has not loaded this chunk yet
        if (!data.isJustTeleported && column.transaction > data.player.packetStateData.packetLastTransactionReceived.get()) {
            data.player.nextTaskToRun = null;
            return;
        }

        boolean forceAddThisTask = data.inVehicle || data.isJustTeleported;

        PredictionData nextTask = data.player.nextTaskToRun;

        if (forceAddThisTask) { // Run the check now
            data.player.nextTaskToRun = null;
            if (nextTask != null)
                addData(nextTask);
            addData(data);
        } else if (nextTask != null) {
            // Mojang fucked up packet order so we need to fix the current item held
            //
            // Why would you send the item held AFTER you send their movement??? Anyways. fixed. you're welcome
            nextTask.itemHeld = data.itemHeld;
            // This packet was a duplicate to the current one, ignore it.
            // Thank you 1.17 for sending duplicate positions!
            if (nextTask.playerX != data.playerX || nextTask.playerY != data.playerY || nextTask.playerZ != data.playerZ) {
                data.player.nextTaskToRun = data;
                addData(nextTask);
            }
        } else {
            data.player.nextTaskToRun = data;
        }
    }

    private void addData(PredictionData data) {
        if (data.player.tasksNotFinished.getAndIncrement() == 0) {
            executor.runCheck(data);
        } else {
            data.player.queuedPredictions.add(data);
        }
    }

    public void runTransactionQueue(GrimPlayer player) {
        // This takes < 0.01 ms to run world and entity updates
        // It stops a memory leak from all the lag compensation queue'ing and never ticking
        CompletableFuture.runAsync(() -> {
            // It is unsafe to modify the transaction world async if another check is running
            // Adding 1 to the tasks blocks another check from running
            //
            // If there are no tasks queue'd, it is safe to modify these variables
            //
            // Additionally, we don't want to, and it isn't needed, to update the world
            if (player.tasksNotFinished.compareAndSet(0, 1)) {
                int lastTransaction = player.packetStateData.packetLastTransactionReceived.get();
                player.compensatedWorld.tickUpdates(lastTransaction);
                player.latencyUtils.handleAnticheatSyncTransaction(lastTransaction);
                player.compensatedEntities.tickUpdates(lastTransaction);
                player.compensatedFlying.canFlyLagCompensated(lastTransaction);
                player.compensatedFireworks.getMaxFireworksAppliedPossible();
                player.compensatedRiptide.getCanRiptide();
                player.compensatedElytra.isGlidingLagCompensated(lastTransaction);

                // As we incremented the tasks, we must now execute the next task, if there is one
                executor.queueNext(player);
            }
        }, executor);
    }

    public void check(PredictionData data) {
        GrimPlayer player = data.player;

        data.isCheckNotReady = data.minimumTickRequiredToContinue > GrimAPI.INSTANCE.getTickManager().getTick();
        if (data.isCheckNotReady) {
            return;
        }

        // Note this before any updates
        boolean byGround = !Collisions.isEmpty(player, player.boundingBox.copy().expand(0.03, 0, 0.03).offset(0, -0.03, 0));

        player.uncertaintyHandler.stuckOnEdge--;
        player.uncertaintyHandler.lastStuckEast--;
        player.uncertaintyHandler.lastStuckWest--;
        player.uncertaintyHandler.lastStuckSouth--;
        player.uncertaintyHandler.lastStuckNorth--;

        // This must be done before updating the world to support bridging and sneaking at the edge of it
        if ((player.isSneaking || player.wasSneaking) && player.uncertaintyHandler.lastTickWasNearGroundZeroPointZeroThree) {
            // Before we do player block placements, determine if the shifting glitch occurred
            // The 0.03 and maintaining velocity is just brutal
            boolean isEast = Collisions.maybeBackOffFromEdge(new Vector(0.1, 0, 0), player, true).getX() != 0.1;
            boolean isWest = Collisions.maybeBackOffFromEdge(new Vector(-0.1, 0, 0), player, true).getX() != -0.1;
            boolean isSouth = Collisions.maybeBackOffFromEdge(new Vector(0, 0, 0.1), player, true).getZ() != 0.1;
            boolean isNorth = Collisions.maybeBackOffFromEdge(new Vector(0, 0, -0.1), player, true).getZ() != -0.1;

            if (isEast) player.uncertaintyHandler.lastStuckEast = 0;
            if (isWest) player.uncertaintyHandler.lastStuckWest = 0;
            if (isSouth) player.uncertaintyHandler.lastStuckSouth = 0;
            if (isNorth) player.uncertaintyHandler.lastStuckNorth = 0;

            if (player.uncertaintyHandler.lastStuckEast > -3)
                player.uncertaintyHandler.xPositiveUncertainty += player.speed;

            if (player.uncertaintyHandler.lastStuckWest > -3)
                player.uncertaintyHandler.xNegativeUncertainty -= player.speed;

            if (player.uncertaintyHandler.lastStuckNorth > -3)
                player.uncertaintyHandler.zNegativeUncertainty -= player.speed;

            if (player.uncertaintyHandler.lastStuckSouth > -3)
                player.uncertaintyHandler.zPositiveUncertainty += player.speed;

            if (isEast || isWest || isSouth || isNorth) {
                player.uncertaintyHandler.stuckOnEdge = 0;
            }
        }

        player.lastTransactionReceived = data.lastTransaction;

        // Tick updates AFTER updating bounding box and actual movement
        player.compensatedWorld.tickUpdates(data.lastTransaction);
        player.compensatedWorld.tickPlayerInPistonPushingArea();
        player.latencyUtils.handleAnticheatSyncTransaction(data.lastTransaction);

        // Update entities to get current vehicle
        player.compensatedEntities.tickUpdates(data.lastTransaction);

        // Player was teleported, so they left their vehicle
        if (!data.inVehicle && data.isJustTeleported)
            player.playerVehicle = null;

        // The game's movement is glitchy when switching between vehicles
        player.vehicleData.lastVehicleSwitch++;
        if (player.lastVehicle != player.playerVehicle) {
            player.vehicleData.lastVehicleSwitch = 0;
        }
        // It is also glitchy when switching between client vs server vehicle control
        if (player.vehicleData.lastDummy) {
            player.vehicleData.lastVehicleSwitch = 0;
        }
        player.vehicleData.lastDummy = false;

        // Tick player vehicle after we update the packet entity state
        player.lastVehicle = player.playerVehicle;
        player.playerVehicle = player.vehicle == null ? null : player.compensatedEntities.getEntity(player.vehicle);
        player.inVehicle = player.playerVehicle != null;

        // If the check was for players moving in a vehicle, but after we just updated vehicles
        // the player isn't in a vehicle, don't check.
        if (data.inVehicle && player.vehicle == null)
            return;

        // If the check was for a player out of a vehicle but the player is in a vehicle
        if (!data.inVehicle && player.vehicle != null)
            return;

        // TODO: Sanity check vehicle position to stop a theoretical teleport bypass
        if (player.playerVehicle != player.lastVehicle) {
            data.isJustTeleported = true;
        }

        if (player.isInBed != player.lastInBed) {
            data.isJustTeleported = true;
        }
        player.lastInBed = player.isInBed;

        // Teleporting is not a tick, don't run anything that we don't need to, to avoid falses
        player.uncertaintyHandler.lastTeleportTicks--;
        if (data.isJustTeleported) {
            player.x = data.playerX;
            player.y = data.playerY;
            player.z = data.playerZ;
            player.lastX = player.x;
            player.lastY = player.y;
            player.lastZ = player.z;
            player.uncertaintyHandler.lastTeleportTicks = 0;

            // Reset velocities
            player.clientVelocity = new Vector();
            player.lastWasClimbing = 0;
            player.canSwimHop = false;

            // Teleports mess with explosions and knockback
            player.checkManager.getExplosionHandler().handlePlayerExplosion(0, true);
            player.checkManager.getKnockbackHandler().handlePlayerKb(0, true);

            LogUtil.info(ChatColor.AQUA + "Player teleported!");

            return;
        }

        // Don't check sleeping players
        if (player.isInBed) return;

        if (!player.inVehicle) {
            player.speed = player.compensatedEntities.playerEntityMovementSpeed;
            player.hasGravity = player.playerEntityHasGravity;
        }

        player.firstBreadKB = player.checkManager.getKnockbackHandler().getFirstBreadOnlyKnockback(player.inVehicle ? player.vehicle : player.entityID, data.lastTransaction);
        player.likelyKB = player.checkManager.getKnockbackHandler().getRequiredKB(player.inVehicle ? player.vehicle : player.entityID, data.lastTransaction);

        player.firstBreadExplosion = player.checkManager.getExplosionHandler().getFirstBreadAddedExplosion(data.lastTransaction);
        player.likelyExplosions = player.checkManager.getExplosionHandler().getPossibleExplosions(data.lastTransaction);

        // Check if the player can control their horse, if they are on a horse
        if (player.inVehicle) {
            // Players are unable to take explosions in vehicles
            player.checkManager.getExplosionHandler().handlePlayerExplosion(0, true);

            // When in control of the entity, the player sets the entity position to their current position
            player.playerVehicle.lastTickPosition = player.playerVehicle.position;
            player.playerVehicle.position = new Vector3d(player.x, player.y, player.z);

            player.hasGravity = player.playerVehicle.hasGravity;

            ItemStack mainHand = player.bukkitPlayer.getInventory().getItem(data.itemHeld);
            // For whatever reason the vehicle move packet occurs AFTER the player changes slots...
            ItemStack newMainHand = player.bukkitPlayer.getInventory().getItem(player.packetStateData.lastSlotSelected);
            if (player.playerVehicle instanceof PacketEntityRideable) {
                Material requiredItem = player.playerVehicle.type == EntityType.PIG ? CARROT_ON_A_STICK : WARPED_FUNGUS_ON_A_STICK;
                if ((mainHand == null || mainHand.getType() != requiredItem) &&
                        (ServerVersion.getVersion().isNewerThanOrEquals(ServerVersion.v_1_9)
                                && player.bukkitPlayer.getInventory().getItemInOffHand().getType() != requiredItem) &&
                        (newMainHand == null || newMainHand.getType() != requiredItem)) {
                    // TODO: Setback
                    Bukkit.broadcastMessage(ChatColor.RED + "Player cannot control this entity!");
                } else if (player.playerVehicle != player.lastVehicle) {
                    // Hack with boostable ticking without us (why does it do this?)
                    ((PacketEntityRideable) player.playerVehicle).currentBoostTime += 4;
                }
            }

            // Player cannot control entities if other players are doing so, although the server will just
            // ignore these bad packets
            if (player.playerVehicle.passengers.length > 0 && player.playerVehicle.passengers[0] != player.entityID) {
                Bukkit.broadcastMessage(ChatColor.RED + "Player cannot control this entity! (second passenger)");
            }

            // Players cannot control stacked vehicles
            // Again, the server knows to ignore this
            if (player.playerVehicle.riding != null) {
                Bukkit.broadcastMessage(ChatColor.RED + "Player cannot control this entity! (stacked)");
            }
        }

        // Determine whether the player is being slowed by using an item
        // Handle the player dropping food to stop eating
        // We are sync'd to roughly the bukkit thread here
        // Although we don't have inventory lag compensation so we can't fully sync
        // Works unless the player spams their offhand button
        ItemStack mainHand = player.bukkitPlayer.getInventory().getItem(data.itemHeld);
        ItemStack offHand = XMaterial.supports(9) ? player.bukkitPlayer.getInventory().getItemInOffHand() : null;
        if (data.isUsingItem == AlmostBoolean.TRUE && (mainHand == null || !Materials.isUsable(mainHand.getType())) &&
                (offHand == null || !Materials.isUsable(offHand.getType()))) {
            data.isUsingItem = AlmostBoolean.MAYBE;
            //Bukkit.broadcastMessage(ChatColor.RED + "Player is no longer using an item!");
        }

        player.ticksSinceLastSlotSwitch++;
        // Switching items results in the player no longer using an item
        if (data.itemHeld != player.lastSlotSelected || data.usingHand != player.lastHand) {
            player.ticksSinceLastSlotSwitch = 0;
        }

        // See shields without this, there's a bit of a delay before the slow applies.  Not sure why.  I blame Mojang.
        if (player.ticksSinceLastSlotSwitch < 3)
            data.isUsingItem = AlmostBoolean.MAYBE;

        player.isUsingItem = data.isUsingItem;

        player.uncertaintyHandler.lastFlyingTicks++;
        if (player.isFlying) {
            player.fallDistance = 0;
            player.uncertaintyHandler.lastFlyingTicks = 0;
        }

        player.boundingBox = GetBoundingBox.getCollisionBoxForPlayer(player, player.lastX, player.lastY, player.lastZ);

        player.x = data.playerX;
        player.y = data.playerY;
        player.z = data.playerZ;
        player.xRot = data.xRot;
        player.yRot = data.yRot;

        player.onGround = data.onGround;

        player.lastSprinting = player.isSprinting;
        player.wasFlying = player.isFlying;
        player.wasGliding = player.isGliding;
        player.isSprinting = data.isSprinting;
        player.wasSneaking = player.isSneaking;
        player.isSneaking = data.isSneaking;
        player.isClimbing = Collisions.onClimbable(player, player.lastX, player.lastY, player.lastZ);

        player.isFlying = player.compensatedFlying.canFlyLagCompensated(data.lastTransaction);
        player.isGliding = player.compensatedElytra.isGlidingLagCompensated(data.lastTransaction) && !player.isFlying;
        player.specialFlying = player.onGround && !player.isFlying && player.wasFlying || player.isFlying;
        player.isRiptidePose = player.compensatedRiptide.getPose(data.lastTransaction);

        player.lastHand = data.usingHand;
        player.lastSlotSelected = data.itemHeld;
        player.tryingToRiptide = data.isTryingToRiptide;

        player.minPlayerAttackSlow = data.minPlayerAttackSlow;
        player.maxPlayerAttackSlow = data.maxPlayerAttackSlow;

        player.clientControlledVerticalCollision = Math.abs(player.y % (1 / 64D)) < 0.00001;
        // If you really have nothing better to do, make this support offset blocks like bamboo.  Good luck!
        player.clientControlledHorizontalCollision = Math.min(GrimMath.distanceToHorizontalCollision(player.x), GrimMath.distanceToHorizontalCollision(player.z)) < 1e-6;

        player.uncertaintyHandler.lastSneakingChangeTicks--;
        if (player.isSneaking != player.wasSneaking)
            player.uncertaintyHandler.lastSneakingChangeTicks = 0;

        // This isn't the final velocity of the player in the tick, only the one applied to the player
        player.actualMovement = new Vector(player.x - player.lastX, player.y - player.lastY, player.z - player.lastZ);

        // ViaVersion messes up flight speed for 1.7 players
        if (player.getClientVersion().isOlderThanOrEquals(ClientVersion.v_1_7_10) && player.isFlying)
            player.isSprinting = true;

        // Stop stuff like clients using elytra in a vehicle...
        // Interesting, on a pig or strider, a player can climb a ladder
        if (player.inVehicle) {
            // Reset fall distance when riding
            player.fallDistance = 0;
            player.isFlying = false;
            player.isGliding = false;
            player.specialFlying = false;

            if (player.playerVehicle.type != EntityType.PIG && player.playerVehicle.type != EntityType.STRIDER) {
                player.isClimbing = false;
            }
        }

        // Multiplying by 1.3 or 1.3f results in precision loss, you must multiply by 0.3
        player.speed += player.isSprinting ? player.speed * 0.3f : 0;

        player.jumpAmplifier = player.compensatedPotions.getPotionLevel("JUMP");
        player.levitationAmplifier = player.compensatedPotions.getPotionLevel("LEVITATION");
        player.slowFallingAmplifier = player.compensatedPotions.getPotionLevel("SLOW_FALLING");
        player.dolphinsGraceAmplifier = player.compensatedPotions.getPotionLevel("DOLPHINS_GRACE");

        player.flySpeed = data.flySpeed;

        player.uncertaintyHandler.wasLastOnGroundUncertain = false;

        player.uncertaintyHandler.lastGlidingChangeTicks--;
        if (player.isGliding != player.wasGliding) player.uncertaintyHandler.lastGlidingChangeTicks = 0;

        player.uncertaintyHandler.isSteppingOnSlime = Collisions.hasSlimeBlock(player);
        player.uncertaintyHandler.wasSteppingOnBouncyBlock = player.uncertaintyHandler.isSteppingOnBouncyBlock;
        player.uncertaintyHandler.isSteppingOnBouncyBlock = Collisions.hasBouncyBlock(player);
        player.uncertaintyHandler.isSteppingOnIce = Materials.checkFlag(BlockProperties.getOnBlock(player, player.lastX, player.lastY, player.lastZ), Materials.ICE);
        player.uncertaintyHandler.isSteppingNearBubbleColumn = player.getClientVersion().isNewerThanOrEquals(ClientVersion.v_1_13) && Collisions.onMaterial(player, BUBBLE_COLUMN, -1);
        // TODO: Make this work for chests, anvils, and client interacted blocks (door, trapdoor, etc.)
        player.uncertaintyHandler.isNearGlitchyBlock = false;
        player.uncertaintyHandler.scaffoldingOnEdge = player.uncertaintyHandler.nextTickScaffoldingOnEdge;
        player.uncertaintyHandler.checkForHardCollision();
        player.uncertaintyHandler.thirtyMillionHardBorder.add(!player.inVehicle && (Math.abs(player.x) == 2.9999999E7D || Math.abs(player.z) == 2.9999999E7D));
        player.uncertaintyHandler.claimingLeftStuckSpeed = player.stuckSpeedMultiplier.getX() < 1 && !Collisions.checkStuckSpeed(player);

        Vector backOff = Collisions.maybeBackOffFromEdge(player.clientVelocity, player, true);
        player.uncertaintyHandler.nextTickScaffoldingOnEdge = player.clientVelocity.getX() != 0 && player.clientVelocity.getZ() != 0 && backOff.getX() == 0 && backOff.getZ() == 0;
        player.canGroundRiptide = false;

        // Exempt if the player is offline
        if (player.isDead || (player.playerVehicle != null && player.playerVehicle.isDead)) {
            // Dead players can't cheat, if you find a way how they could, open an issue
            player.predictedVelocity = new VectorData(player.actualMovement, VectorData.VectorType.Dead);
            player.clientVelocity = new Vector();

            // Dead players don't take explosions or knockback
            player.checkManager.getExplosionHandler().handlePlayerExplosion(0, true);
            player.checkManager.getKnockbackHandler().handlePlayerKb(0, true);
        } else if (ServerVersion.getVersion().isNewerThanOrEquals(ServerVersion.v_1_8) && data.gameMode == GameMode.SPECTATOR || player.specialFlying) {
            // We could technically check spectator but what's the point...
            // Added complexity to analyze a gamemode used mainly by moderators
            //
            // TODO: Re-implement flying support
            player.predictedVelocity = new VectorData(player.actualMovement, VectorData.VectorType.Spectator);
            player.clientVelocity = player.actualMovement.clone();
            player.gravity = 0;
            player.friction = 0.91f;
            PredictionEngineNormal.staticVectorEndOfTick(player, player.clientVelocity);

            player.checkManager.getExplosionHandler().handlePlayerExplosion(0, true);
            player.checkManager.getKnockbackHandler().handlePlayerKb(0, true);
        } else if (player.playerVehicle == null) {
            // Depth strider was added in 1.8
            ItemStack boots = player.bukkitPlayer.getInventory().getBoots();
            if (boots != null && XMaterial.supports(8) && player.getClientVersion().isNewerThanOrEquals(ClientVersion.v_1_8)) {
                player.depthStriderLevel = boots.getEnchantmentLevel(Enchantment.DEPTH_STRIDER);
            } else {
                player.depthStriderLevel = 0;
            }

            // Now that we have all the world updates, recalculate if the player is near the ground
            player.uncertaintyHandler.lastTickWasNearGroundZeroPointZeroThree = !Collisions.isEmpty(player, player.boundingBox.copy().expand(0.03, 0, 0.03).offset(0, -0.03, 0));
            player.uncertaintyHandler.didGroundStatusChangeWithoutPositionPacket = data.didGroundStatusChangeWithoutPositionPacket;

            // Vehicles don't have jumping or that stupid < 0.03 thing
            // If the player isn't on the ground, a packet in between < 0.03 said they did
            // And the player is reasonably touching the ground
            //
            // And the player isn't now near the ground due to a new block placed by the player
            //
            // Give some lenience and update the onGround status
            if (player.uncertaintyHandler.didGroundStatusChangeWithoutPositionPacket && !player.lastOnGround
                    && (player.uncertaintyHandler.lastTickWasNearGroundZeroPointZeroThree || byGround)) {
                player.lastOnGround = true;
                player.uncertaintyHandler.wasLastOnGroundUncertain = true;
                player.uncertaintyHandler.lastTickWasNearGroundZeroPointZeroThree = true;
                player.clientClaimsLastOnGround = true;
            }

            // This is wrong and the engine was not designed around stuff like this
            player.canGroundRiptide = ((player.clientClaimsLastOnGround && player.uncertaintyHandler.lastTickWasNearGroundZeroPointZeroThree)
                    || (player.uncertaintyHandler.isSteppingOnSlime && player.uncertaintyHandler.lastTickWasNearGroundZeroPointZeroThree))
                    && player.tryingToRiptide && player.compensatedRiptide.getCanRiptide() && !player.inVehicle;

            // If the player's last tick was on the ground and the player is near the ground
            if (player.canGroundRiptide) {
                Vector pushingMovement = Collisions.collide(player, 0, 1.1999999F, 0);
                player.verticalCollision = pushingMovement.getY() != 1.1999999F;

                player.lastOnGround = false;
                player.boundingBox.offset(0, pushingMovement.getY(), 0);
                player.lastY += pushingMovement.getY();
                player.actualMovement = new Vector(player.x - player.lastX, player.y - player.lastY, player.z - player.lastZ);

                Collisions.handleInsideBlocks(player);
            }

            new PlayerBaseTick(player).doBaseTick();
            new MovementTickerPlayer(player).livingEntityAIStep();
        } else if (ServerVersion.getVersion().isNewerThanOrEquals(ServerVersion.v_1_9) && player.getClientVersion().isNewerThanOrEquals(ClientVersion.v_1_9)) {
            // The player and server are both on a version with client controlled entities
            // If either or both of the client server version has server controlled entities
            // The player can't use entities (or the server just checks the entities)
            if (player.playerVehicle.type == EntityType.BOAT) {
                new PlayerBaseTick(player).doBaseTick();
                // Speed doesn't affect anything with boat movement
                new BoatPredictionEngine(player).guessBestMovement(0, player);
            } else if (player.playerVehicle instanceof PacketEntityHorse) {
                new PlayerBaseTick(player).doBaseTick();
                new MovementTickerHorse(player).livingEntityAIStep();
            } else if (player.playerVehicle.type == EntityType.PIG) {
                new PlayerBaseTick(player).doBaseTick();
                new MovementTickerPig(player).livingEntityAIStep();
            } else if (player.playerVehicle.type == EntityType.STRIDER) {
                new PlayerBaseTick(player).doBaseTick();
                new MovementTickerStrider(player).livingEntityAIStep();
                MovementTickerStrider.floatStrider(player);
                Collisions.handleInsideBlocks(player);
            }
        } // If it isn't any of these cases, the player is on a mob they can't control and therefore is exempt

        Vector offsetVector = player.predictedVelocity.vector.clone().subtract(player.actualMovement);
        double offset = offsetVector.length();

        // Exempt players from piston checks by giving them 1 block of lenience for any piston pushing
        if (Collections.max(player.uncertaintyHandler.pistonPushing) > 0) {
            offset -= 1;
        }

        // Boats are too glitchy to check.
        // Yes, they have caused an insane amount of uncertainty!
        // Even 1 block offset reduction isn't enough... damn it mojang
        if (Collections.max(player.uncertaintyHandler.hardCollidingLerpingEntity)) {
            offset -= 1.2;
        }

        if (player.uncertaintyHandler.lastGlidingChangeTicks > -6)
            offset -= 0.25;

        if (Collections.max(player.uncertaintyHandler.flyingStatusSwitchHack)) {
            offset -= 0.25;
        }

        if (player.uncertaintyHandler.isNearGlitchyBlock) {
            offset -= 0.15;
        }

        // Checking slime is too complicated
        if (player.uncertaintyHandler.influencedByBouncyBlock() && Math.abs(player.actualMovement.getY()) < 0.418) {
            offset -= 0.1;
        }

        if (player.uncertaintyHandler.isSteppingNearBubbleColumn) {
            offset -= 0.09;
        }

        if (player.uncertaintyHandler.stuckOnEdge > -3) {
            offset -= 0.05;
        }

        // Errors are caused by a combination of client/server desync while climbing
        // desync caused by 0.03 and the lack of an idle packet
        //
        // I can't solve this.  This is on Mojang to fix.
        //
        // Don't even attempt to fix the poses code... garbage in garbage out - I did the best I could
        // you can likely look at timings of packets to extrapolate better... but I refuse to use packet timings for stuff like this
        // Does anyone at mojang understand netcode??? (the answer is no)
        //
        // Don't give me the excuse that it was originally a singleplayer game so the netcode is terrible...
        // the desync's and netcode has progressively gotten worse starting with 1.9!
        if (!Collisions.isEmpty(player, GetBoundingBox.getBoundingBoxFromPosAndSize(player.x, player.y, player.z, 0.6f, 1.8f).expand(-SimpleCollisionBox.COLLISION_EPSILON).offset(0, 0.03, 0)) && player.isClimbing) {
            offset -= 0.12;
        }

        // I can't figure out how the client exactly tracks boost time
        if (player.playerVehicle instanceof PacketEntityRideable) {
            PacketEntityRideable vehicle = (PacketEntityRideable) player.playerVehicle;
            if (vehicle.currentBoostTime < vehicle.boostTimeMax + 20)
                offset -= 0.01;
        }

        // Sneaking near edge cases a ton of issues
        // Don't give this bonus if the Y axis is wrong though.
        // Another temporary permanent hack.
        if (player.uncertaintyHandler.stuckOnEdge == -2 && player.clientVelocity.getY() > 0 && Math.abs(player.clientVelocity.getY() - player.actualMovement.getY()) < 1e-6)
            offset -= 0.1;

        offset = Math.max(0, offset);

        // Don't check players who are offline
        if (!player.bukkitPlayer.isOnline()) return;
        // Don't check players who just switched worlds
        if (player.playerWorld != player.bukkitPlayer.getWorld()) return;

        player.checkManager.onPredictionFinish(new PredictionComplete(offset));

        player.riptideSpinAttackTicks--;
        if (player.predictedVelocity.hasVectorType(VectorData.VectorType.Trident))
            player.riptideSpinAttackTicks = 20;

        player.uncertaintyHandler.wasLastGravityUncertain = player.uncertaintyHandler.gravityUncertainty != 0;
        player.uncertaintyHandler.lastLastMovementWasZeroPointZeroThree = player.uncertaintyHandler.lastMovementWasZeroPointZeroThree;
        player.uncertaintyHandler.lastMovementWasZeroPointZeroThree = player.uncertaintyHandler.countsAsZeroPointZeroThree(player.predictedVelocity);
        player.uncertaintyHandler.lastLastPacketWasGroundPacket = player.uncertaintyHandler.lastPacketWasGroundPacket;
        player.uncertaintyHandler.lastPacketWasGroundPacket = player.uncertaintyHandler.wasLastOnGroundUncertain;
        player.uncertaintyHandler.lastMetadataDesync--;

        if (player.playerVehicle instanceof PacketEntityRideable) {
            PacketEntityRideable rideable = (PacketEntityRideable) player.playerVehicle;
            rideable.entityPositions.clear();
            rideable.entityPositions.add(rideable.position);
        }

        player.lastX = player.x;
        player.lastY = player.y;
        player.lastZ = player.z;
        player.lastXRot = player.xRot;
        player.lastYRot = player.yRot;
        player.lastOnGround = player.onGround;

        player.vehicleData.vehicleForward = (float) Math.min(0.98, Math.max(-0.98, data.vehicleForward));
        player.vehicleData.vehicleHorizontal = (float) Math.min(0.98, Math.max(-0.98, data.vehicleHorizontal));
        player.vehicleData.horseJump = data.horseJump;

        player.checkManager.getKnockbackHandler().handlePlayerKb(offset, false);
        player.checkManager.getExplosionHandler().handlePlayerExplosion(offset, false);
        player.trigHandler.setOffset(offset);
        player.compensatedRiptide.handleRemoveRiptide();
    }
}
