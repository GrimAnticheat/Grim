package ac.grim.grimac.predictionengine;

import ac.grim.grimac.GrimAC;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.predictionengine.movementtick.MovementTickerHorse;
import ac.grim.grimac.predictionengine.movementtick.MovementTickerPig;
import ac.grim.grimac.predictionengine.movementtick.MovementTickerPlayer;
import ac.grim.grimac.predictionengine.movementtick.MovementTickerStrider;
import ac.grim.grimac.predictionengine.predictions.PredictionEngineNormal;
import ac.grim.grimac.predictionengine.predictions.rideable.BoatPredictionEngine;
import ac.grim.grimac.utils.chunks.Column;
import ac.grim.grimac.utils.collisions.datatypes.SimpleCollisionBox;
import ac.grim.grimac.utils.data.AlmostBoolean;
import ac.grim.grimac.utils.data.PredictionData;
import ac.grim.grimac.utils.data.VectorData;
import ac.grim.grimac.utils.data.packetentity.PacketEntityHorse;
import ac.grim.grimac.utils.data.packetentity.PacketEntityRideable;
import ac.grim.grimac.utils.enums.EntityType;
import ac.grim.grimac.utils.math.GrimMathHelper;
import ac.grim.grimac.utils.nmsImplementations.*;
import ac.grim.grimac.utils.threads.CustomThreadPoolExecutor;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.github.retrooper.packetevents.utils.pair.Pair;
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
import java.util.UUID;
import java.util.concurrent.*;

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
public class MovementCheckRunner {
    private static final Material CARROT_ON_A_STICK = XMaterial.CARROT_ON_A_STICK.parseMaterial();
    private static final Material WARPED_FUNGUS_ON_A_STICK = XMaterial.WARPED_FUNGUS_ON_A_STICK.parseMaterial();
    private static final Material BUBBLE_COLUMN = XMaterial.BUBBLE_COLUMN.parseMaterial();

    public static ConcurrentHashMap<UUID, ConcurrentLinkedQueue<PredictionData>> queuedPredictions = new ConcurrentHashMap<>();
    public static CustomThreadPoolExecutor executor =
            new CustomThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS,
                    new LinkedBlockingQueue<>(), new ThreadFactoryBuilder().setDaemon(true).build());
    public static ConcurrentLinkedQueue<PredictionData> waitingOnServerQueue = new ConcurrentLinkedQueue<>();

    public static boolean checkTeleportQueue(PredictionData data, double x, double y, double z) {
        // Support teleports without teleport confirmations
        // If the player is in a vehicle when teleported, they will exit their vehicle
        while (true) {
            Pair<Integer, Vector3d> teleportPos = data.player.teleports.peek();
            if (teleportPos == null) break;

            Vector3d position = teleportPos.getSecond();

            if (data.lastTransaction < teleportPos.getFirst()) {
                break;
            }

            // Don't use prediction data because it doesn't allow positions past 29,999,999 blocks
            if (position.getX() == x && position.getY() == y && position.getZ() == z) {
                data.player.teleports.poll();
                data.isJustTeleported = true;

                Bukkit.broadcastMessage(ChatColor.AQUA + data.player.bukkitPlayer.getName() + " just teleported! to " + position);

                // Exempt for the next tick for all teleports
                data.player.timerCheck.exempt++;

                // Long distance teleport
                if (position.distanceSquared(new Vector3d(x, y, z)) > 32 * 32)
                    data.player.timerCheck.exempt = Math.max(data.player.timerCheck.exempt, 150); // Exempt for 7.5 seconds on teleport

                // Teleports remove the player from their vehicle
                data.player.vehicle = null;

                return true;
            } else if (data.lastTransaction > teleportPos.getFirst() + 2) {
                data.player.teleports.poll();
                Bukkit.broadcastMessage(ChatColor.RED + data.player.bukkitPlayer.getName() + " ignored teleport! " + position);
                continue;
            }

            break;
        }

        return false;
    }

    public static void checkVehicleTeleportQueue(PredictionData data) {
        // Handle similar teleports for players in vehicles
        while (true) {
            Pair<Integer, Vector3d> teleportPos = data.player.vehicleTeleports.peek();
            if (teleportPos == null) break;
            if (data.lastTransaction < teleportPos.getFirst()) {
                break;
            }

            Vector3d position = teleportPos.getSecond();
            if (position.getX() == data.playerX && position.getY() == data.playerY && position.getZ() == data.playerZ) {
                data.player.vehicleTeleports.poll();
                data.isJustTeleported = true;

                // Exempt for the next tick for all teleports
                data.player.timerCheck.exempt++;

                // Long distance teleport
                if (position.distanceSquared(new Vector3d(data.playerX, data.playerY, data.playerZ)) > 32 * 32)
                    data.player.timerCheck.exempt = Math.max(data.player.timerCheck.exempt, 150); // Exempt for 7.5 seconds on long teleport

                continue;
            } else if (data.lastTransaction > teleportPos.getFirst() + 2) {
                data.player.vehicleTeleports.poll();
                continue;
            }

            break;
        }
    }

    public static boolean processAndCheckMovementPacket(PredictionData data) {
        // Client sends junk onGround data when they teleport
        if (data.isJustTeleported)
            data.onGround = data.player.packetStateData.packetPlayerOnGround;

        Column column = data.player.compensatedWorld.getChunk(GrimMathHelper.floor(data.playerX) >> 4, GrimMathHelper.floor(data.playerZ) >> 4);

        data.player.packetStateData.packetPlayerXRot = data.xRot;
        data.player.packetStateData.packetPlayerYRot = data.yRot;
        data.player.packetStateData.packetPlayerOnGround = data.onGround;

        data.player.packetStateData.packetPlayerX = data.playerX;
        data.player.packetStateData.packetPlayerY = data.playerY;
        data.player.packetStateData.packetPlayerZ = data.playerZ;

        // The player is in an unloaded chunk
        if (!data.isJustTeleported && column == null) {
            data.player.nextTaskToRun = null;
            return false;
        }
        // The player has not loaded this chunk yet
        if (!data.isJustTeleported && column.transaction > data.player.packetStateData.packetLastTransactionReceived.get()) {
            data.player.nextTaskToRun = null;
            return false;
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
            if (nextTask.playerX == data.playerX && nextTask.playerY == data.playerY && nextTask.playerZ == data.playerZ) {
                return false;
            } else {
                data.player.nextTaskToRun = data;
                addData(nextTask);
            }
        } else {
            data.player.nextTaskToRun = data;
        }

        // Was this mojang sending duplicate packets because 1.17?  If so, then don't pass into timer check.
        // (This can happen multiple times a tick, the player can send us infinite movement packets a second!)
        return true;
    }

    private static void addData(PredictionData data) {
        if (data.player.tasksNotFinished.getAndIncrement() == 0) {
            executor.runCheck(data);
        } else {
            queuedPredictions.get(data.player.playerUUID).add(data);
        }
    }

    public static void runTransactionQueue(GrimPlayer player) {
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
                player.compensatedEntities.tickUpdates(lastTransaction, false);
                player.compensatedFlying.canFlyLagCompensated(lastTransaction);
                player.compensatedFireworks.getMaxFireworksAppliedPossible();
                player.compensatedRiptide.getCanRiptide();
                player.compensatedElytra.isGlidingLagCompensated(lastTransaction);
                player.compensatedPotions.handleTransactionPacket(lastTransaction);

                // As we incremented the tasks, we must now execute the next task, if there is one
                executor.queueNext(player);
            }
        }, executor);
    }

    public static void check(PredictionData data) {
        GrimPlayer player = data.player;

        data.isCheckNotReady = data.minimumTickRequiredToContinue > GrimAC.getCurrentTick();
        if (data.isCheckNotReady) {
            return;
        }

        // Tick updates AFTER updating bounding box and actual movement
        player.compensatedWorld.tickUpdates(data.lastTransaction);
        player.compensatedWorld.tickPlayerInPistonPushingArea();

        player.lastTransactionReceived = data.lastTransaction;

        // Update entities to get current vehicle
        player.compensatedEntities.tickUpdates(data.lastTransaction, false);

        // If the check was for players moving in a vehicle, but after we just updated vehicles
        // the player isn't in a vehicle, don't check.
        if (data.inVehicle && player.vehicle == null)
            return;

        // Player was teleported, so therefore they left their vehicle
        if (!data.inVehicle && data.isJustTeleported)
            player.playerVehicle = null;

        // The game's movement is glitchy when switching between vehicles
        player.lastVehicleSwitch++;
        if (player.lastVehicle != player.playerVehicle) {
            player.lastVehicleSwitch = 0;
        }
        // It is also glitchy when switching between client vs server vehicle control
        if (player.lastDummy) {
            player.lastVehicleSwitch = 0;
        }
        player.lastDummy = false;

        // Tick player vehicle after we update the packet entity state
        player.lastVehicle = player.playerVehicle;
        player.playerVehicle = player.vehicle == null ? null : player.compensatedEntities.getEntity(player.vehicle);
        player.inVehicle = player.playerVehicle != null;

        if (player.playerVehicle != player.lastVehicle) {
            data.isJustTeleported = true;
        }

        if (!player.inVehicle)
            player.speed = player.compensatedEntities.playerEntityMovementSpeed;

        player.firstBreadKB = player.knockbackHandler.getFirstBreadOnlyKnockback(player.inVehicle ? player.vehicle : player.entityID, data.lastTransaction);
        player.likelyKB = player.knockbackHandler.getRequiredKB(player.inVehicle ? player.vehicle : player.entityID, data.lastTransaction);

        player.firstBreadExplosion = player.explosionHandler.getFirstBreadAddedExplosion(data.lastTransaction);
        player.likelyExplosions = player.explosionHandler.getPossibleExplosions(data.lastTransaction);

        // Check if the player can control their horse, if they are on a horse
        if (player.inVehicle) {
            // Players are unable to take explosions in vehicles
            player.explosionHandler.handlePlayerExplosion(0);

            // When in control of the entity, the player sets the entity position to their current position
            player.playerVehicle.lastTickPosition = player.playerVehicle.position;
            player.playerVehicle.position = new Vector3d(player.x, player.y, player.z);

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

        player.isFlying = player.compensatedFlying.canFlyLagCompensated(data.lastTransaction);
        player.isClimbing = Collisions.onClimbable(player);
        player.isGliding = player.compensatedElytra.isGlidingLagCompensated(data.lastTransaction) && !player.isFlying;
        player.specialFlying = player.onGround && !player.isFlying && player.wasFlying || player.isFlying;
        player.isRiptidePose = player.compensatedRiptide.getPose(data.lastTransaction);

        player.lastHand = data.usingHand;
        player.lastSlotSelected = data.itemHeld;
        player.tryingToRiptide = data.isTryingToRiptide;

        player.minPlayerAttackSlow = data.minPlayerAttackSlow;
        player.maxPlayerAttackSlow = data.maxPlayerAttackSlow;
        player.playerWorld = data.playerWorld;

        player.clientControlledVerticalCollision = Math.abs(player.y % (1 / 64D)) < 0.00001;
        // If you really have nothing better to do, make this support offset blocks like bamboo.  Good luck!
        player.clientControlledHorizontalCollision = Math.min(GrimMathHelper.distanceToHorizontalCollision(player.x), GrimMathHelper.distanceToHorizontalCollision(player.z)) < 1e-6;

        player.uncertaintyHandler.lastTeleportTicks--;
        if (data.isJustTeleported) {
            player.lastX = player.x;
            player.lastY = player.y;
            player.lastZ = player.z;
            player.uncertaintyHandler.lastTeleportTicks = 0;

            // Teleports mess with explosions and knockback
            player.explosionHandler.handlePlayerExplosion(0);
            player.knockbackHandler.handlePlayerKb(0);
        }

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

            player.uncertaintyHandler.lastTickWasNearGroundZeroPointZeroThree = !Collisions.isEmpty(player, player.boundingBox.copy().expand(0.03, 0, 0.03).offset(0, -0.03, 0));
        }

        // Multiplying by 1.3 or 1.3f results in precision loss, you must multiply by 0.3
        player.speed += player.isSprinting ? player.speed * 0.3f : 0;
        player.jumpAmplifier = data.jumpAmplifier;
        player.levitationAmplifier = data.levitationAmplifier;
        player.slowFallingAmplifier = data.slowFallingAmplifier;
        player.dolphinsGraceAmplifier = data.dolphinsGraceAmplifier;
        player.flySpeed = data.flySpeed;

        player.uncertaintyHandler.wasLastOnGroundUncertain = false;

        player.uncertaintyHandler.isSteppingOnSlime = Collisions.hasSlimeBlock(player);
        player.uncertaintyHandler.isSteppingOnBouncyBlock = Collisions.hasBouncyBlock(player);
        player.uncertaintyHandler.isSteppingOnIce = Materials.checkFlag(BlockProperties.getOnBlock(player, player.lastX, player.lastY, player.lastZ), Materials.ICE);
        player.uncertaintyHandler.isSteppingNearBubbleColumn = player.getClientVersion().isNewerThanOrEquals(ClientVersion.v_1_13) && Collisions.onMaterial(player, BUBBLE_COLUMN, -0.5);
        player.uncertaintyHandler.scaffoldingOnEdge = player.uncertaintyHandler.nextTickScaffoldingOnEdge;
        player.uncertaintyHandler.checkForHardCollision();
        player.uncertaintyHandler.thirtyMillionHardBorder.add(!player.inVehicle && (Math.abs(player.x) == 2.9999999E7D || Math.abs(player.z) == 2.9999999E7D));

        player.uncertaintyHandler.nextTickScaffoldingOnEdge = false;
        player.canGroundRiptide = player.lastOnGround && player.tryingToRiptide && !player.inVehicle;

        // Exempt if the player is offline
        if (data.isJustTeleported || !player.bukkitPlayer.isOnline()) {
            // Don't let the player move if they just teleported
            player.predictedVelocity = new VectorData(new Vector(), VectorData.VectorType.Teleport);
            player.clientVelocity = new Vector();
        } else if (player.bukkitPlayer.isDead() || (player.playerVehicle != null && player.playerVehicle.isDead)) {
            // Dead players can't cheat, if you find a way how they could, open an issue
            player.predictedVelocity = new VectorData(player.actualMovement, VectorData.VectorType.Dead);
            player.clientVelocity = new Vector();
        } else if (ServerVersion.getVersion().isNewerThanOrEquals(ServerVersion.v_1_8) && player.bukkitPlayer.getGameMode() == GameMode.SPECTATOR) {
            // We could technically check spectator but what's the point...
            // Added complexity to analyze a gamemode used mainly by moderators
            // ViaVersion plays with 1.7 player flying speed, don't bother checking them
            // We don't know what ViaVersion is doing as their packet listener is in front of ours
            player.predictedVelocity = new VectorData(player.actualMovement, VectorData.VectorType.Spectator);
            player.clientVelocity = player.actualMovement.clone();
            player.gravity = 0;
            player.friction = 0.91f;
            PredictionEngineNormal.staticVectorEndOfTick(player, player.clientVelocity);
        } else if (player.playerVehicle == null) {
            // Depth strider was added in 1.8
            ItemStack boots = player.bukkitPlayer.getInventory().getBoots();
            if (boots != null && XMaterial.supports(8) && player.getClientVersion().isNewerThanOrEquals(ClientVersion.v_1_8)) {
                player.depthStriderLevel = boots.getEnchantmentLevel(Enchantment.DEPTH_STRIDER);
            } else {
                player.depthStriderLevel = 0;
            }

            if (player.canGroundRiptide) {
                double addedY = Math.min(player.actualMovement.getY(), 1.1999999F);
                player.lastOnGround = false;
                player.lastY += addedY;

                player.boundingBox.offset(0, addedY, 0);
            }

            new PlayerBaseTick(player).doBaseTick();

            SimpleCollisionBox updatedBox = GetBoundingBox.getPlayerBoundingBox(player, player.x, player.y, player.z);

            if (player.isSneaking || player.wasSneaking) {
                // Before we do player block placements, determine if the shifting glitch occurred
                // It's a glitch on 1.14+ and on earlier versions, the 0.03 is just brutal.
                boolean east = player.actualMovement.angle(new Vector(1, 0, 0)) < 60 && Collisions.isEmpty(player, updatedBox.copy().offset(0.1, -0.6, 0));
                boolean west = player.actualMovement.angle(new Vector(-1, 0, 1)) < 60 && Collisions.isEmpty(player, updatedBox.copy().offset(-0.1, -0.6, 0));
                boolean south = player.actualMovement.angle(new Vector(0, 0, 1)) < 60 && Collisions.isEmpty(player, updatedBox.copy().offset(0, -0.6, 0.1));
                boolean north = player.actualMovement.angle(new Vector(0, 0, -1)) < 60 && Collisions.isEmpty(player, updatedBox.copy().offset(0, -0.6, -0.1));

                player.uncertaintyHandler.stuckOnEdge = (east || west || south || north);
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
                    && (player.uncertaintyHandler.lastTickWasNearGroundZeroPointZeroThree
                    || !Collisions.isEmpty(player, player.boundingBox.copy().offset(0, -0.03, 0)))) {
                player.lastOnGround = true;
                player.uncertaintyHandler.wasLastOnGroundUncertain = true;
                player.uncertaintyHandler.lastTickWasNearGroundZeroPointZeroThree = true;
            }

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

        // Exempt 1.7 players from piston checks by giving them 1 block of lenience for any piston pushing
        // ViaVersion is modifying their movement which messes us up
        //
        // This does NOT apply for 1.8 and above players
        // Anyways, 1.7 clients are more used on arena PvP servers or other gamemodes without pistons
        if (player.getClientVersion().isOlderThanOrEquals(ClientVersion.v_1_7_10) && Collections.max(player.uncertaintyHandler.pistonPushing) > 0) {
            offset -= 1;
        }

        // Boats are too glitchy to check.
        // Yes, they have caused an insane amount of uncertainty!
        // Even 1 block offset reduction isn't enough... damn it mojang
        if (Collections.max(player.uncertaintyHandler.hardCollidingLerpingEntity)) {
            offset -= 1.2;
        }

        // Checking slime is too complicated
        if (player.uncertaintyHandler.isSteppingOnBouncyBlock) {
            offset -= 0.03;
        }

        // Sneaking near edge cases a ton of issues
        // Don't give this bonus if the Y axis is wrong though.
        // Another temporary permanent hack.
        if (player.uncertaintyHandler.stuckOnEdge && player.clientVelocity.getY() > 0 && Math.abs(player.clientVelocity.getY() - player.actualMovement.getY()) < 1e-6)
            offset -= 0.1;

        offset = Math.max(0, offset);

        ChatColor color;

        if (offset <= 0) {
            color = ChatColor.GRAY;
        } else if (offset < 0.0001) {
            color = ChatColor.GREEN;
        } else if (offset < 0.01) {
            color = ChatColor.YELLOW;
        } else {
            color = ChatColor.RED;
        }

        // Vanilla can desync with riptide status
        // This happens because of the < 0.03 thing
        // It also happens at random, especially when close to exiting water (because minecraft netcode sucks)
        //
        // We can recover from the near water desync, but we cannot recover from the rain desync and must set the player back
        if (player.tryingToRiptide != player.compensatedRiptide.getCanRiptide() && player.predictedVelocity.hasVectorType(VectorData.VectorType.Trident) && !player.compensatedWorld.containsWater(GetBoundingBox.getPlayerBoundingBox(player, player.lastX, player.lastY, player.lastZ).expand(0.3, 0.3, 0.3)))
            Bukkit.broadcastMessage(ChatColor.LIGHT_PURPLE + "DESYNC IN RIPTIDE! // todo: setback and exempt player until setback");

        player.riptideSpinAttackTicks--;
        if (player.predictedVelocity.hasVectorType(VectorData.VectorType.Trident))
            player.riptideSpinAttackTicks = 20;

        player.uncertaintyHandler.wasLastGravityUncertain = player.uncertaintyHandler.gravityUncertainty != 0;
        player.uncertaintyHandler.lastLastMovementWasZeroPointZeroThree = player.uncertaintyHandler.lastMovementWasZeroPointZeroThree;
        player.uncertaintyHandler.lastMovementWasZeroPointZeroThree = player.uncertaintyHandler.countsAsZeroPointZeroThree(player.predictedVelocity);
        player.uncertaintyHandler.lastLastPacketWasGroundPacket = player.uncertaintyHandler.lastPacketWasGroundPacket;
        player.uncertaintyHandler.lastPacketWasGroundPacket = player.uncertaintyHandler.wasLastOnGroundUncertain;

        player.isFirstTick = false;

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
        player.lastClimbing = player.isClimbing;

        player.vehicleForward = (float) Math.min(0.98, Math.max(-0.98, data.vehicleForward));
        player.vehicleHorizontal = (float) Math.min(0.98, Math.max(-0.98, data.vehicleHorizontal));
        player.horseJump = data.horseJump;

        player.knockbackHandler.handlePlayerKb(offset);
        player.explosionHandler.handlePlayerExplosion(offset);
        player.trigHandler.setOffset(offset);
        player.compensatedRiptide.handleRemoveRiptide();

        if (color == ChatColor.YELLOW || color == ChatColor.RED) {
            player.bukkitPlayer.sendMessage("P: " + color + player.predictedVelocity.vector.getX() + " " + player.predictedVelocity.vector.getY() + " " + player.predictedVelocity.vector.getZ());
            player.bukkitPlayer.sendMessage("A: " + color + player.actualMovement.getX() + " " + player.actualMovement.getY() + " " + player.actualMovement.getZ());
            player.bukkitPlayer.sendMessage("O: " + color + offset + " " + player.inVehicle + " " + Collections.max(player.uncertaintyHandler.hardCollidingLerpingEntity));

            if (player.lastVehicleSwitch < 5) {
                player.bukkitPlayer.sendMessage("Note that the player would be setback and not punished");
            }

            if (!player.uncertaintyHandler.countsAsZeroPointZeroThree(player.predictedVelocity) && !player.horizontalCollision && player.clientControlledHorizontalCollision) {
                player.bukkitPlayer.sendMessage("Horizontal collision desync!");
            }
            if (!player.uncertaintyHandler.countsAsZeroPointZeroThree(player.predictedVelocity) && !player.uncertaintyHandler.isStepMovement && !player.verticalCollision && player.clientControlledVerticalCollision) {
                player.bukkitPlayer.sendMessage("Vertical collision desync!");
            }
        }

        GrimAC.staticGetLogger().info(player.bukkitPlayer.getName() + " P: " + color + player.predictedVelocity.vector.getX() + " " + player.predictedVelocity.vector.getY() + " " + player.predictedVelocity.vector.getZ());
        GrimAC.staticGetLogger().info(player.bukkitPlayer.getName() + " A: " + color + player.actualMovement.getX() + " " + player.actualMovement.getY() + " " + player.actualMovement.getZ());
        GrimAC.staticGetLogger().info(player.bukkitPlayer.getName() + " O: " + color + offset + " " + player.uncertaintyHandler.stuckOnEdge);
    }
}
