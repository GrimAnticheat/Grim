package ac.grim.grimac.checks.predictionengine;

import ac.grim.grimac.GrimAC;
import ac.grim.grimac.checks.movement.TimerCheck;
import ac.grim.grimac.checks.predictionengine.movementTick.*;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.data.PredictionData;
import ac.grim.grimac.utils.enums.Pose;
import ac.grim.grimac.utils.math.Mth;
import ac.grim.grimac.utils.nmsImplementations.GetBoundingBox;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Pig;
import org.bukkit.entity.Strider;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

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
public class MovementCheckRunner implements Listener {
    public static ConcurrentHashMap<UUID, ConcurrentLinkedQueue<PredictionData>> queuedPredictions = new ConcurrentHashMap<>();
    // List instead of Set for consistency in debug output
    static List<MovementCheck> movementCheckListeners = new ArrayList<>();
    // I actually don't know how many threads is good, more testing is needed!
    static ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(8, new ThreadFactoryBuilder().setDaemon(true).build());

    public static void addQueuedPrediction(PredictionData data) {
        // TODO: This is a hack that should be fixed - maybe
        // This allows animal movement packets to also go through this system
        TimerCheck.processMovementPacket(data.player);

        if (data.player.tasksNotFinished.getAndIncrement() == 0) {
            executor.submit(() -> check(data));
        } else {
            queuedPredictions.get(data.player.playerUUID).add(data);
        }
    }
    public static void check(PredictionData data) {
        GrimPlayer player = data.player;

        // If we don't catch it, the exception is silently eaten by ThreadPoolExecutor
        try {
            player.x = data.playerX;
            player.y = data.playerY;
            player.z = data.playerZ;
            player.xRot = data.xRot;
            player.yRot = data.yRot;
            player.onGround = data.onGround;
            player.lastSprinting = player.isSprinting;
            player.wasFlying = player.isFlying;
            player.isSprinting = data.isSprinting;
            player.wasSneaking = player.isSneaking;
            player.isSneaking = data.isSneaking;
            player.specialFlying = player.onGround && !data.isFlying && player.isFlying || data.isFlying;
            player.isFlying = data.isFlying;
            player.isClimbing = data.isClimbing;
            player.isFallFlying = data.isFallFlying;
            player.playerWorld = data.playerWorld;
            player.fallDistance = data.fallDistance;

            player.movementSpeed = data.movementSpeed;
            player.jumpAmplifier = data.jumpAmplifier;
            player.levitationAmplifier = data.levitationAmplifier;
            player.flySpeed = data.flySpeed;
            player.inVehicle = data.inVehicle;
            player.playerVehicle = data.playerVehicle;

            player.firstBreadKB = data.firstBreadKB;
            player.possibleKB = data.possibleKB;

            // This isn't the final velocity of the player in the tick, only the one applied to the player
            player.actualMovement = new Vector(player.x - player.lastX, player.y - player.lastY, player.z - player.lastZ);

            if (!player.inVehicle) {
                player.boundingBox = GetBoundingBox.getPlayerBoundingBox(player, player.lastX, player.lastY, player.lastZ);

                // This is not affected by any movement
                new PlayerBaseTick(player).doBaseTick();

                // baseTick occurs before this
                new MovementTickerPlayer(player).livingEntityAIStep();

                //handleSkippedTicks(player);
            } else if (player.playerVehicle instanceof Boat) {

                // TODO: We will have to handle teleports (occurs multiple times a second due to vanilla glitchyness)
                player.boundingBox = GetBoundingBox.getBoatBoundingBox(player.lastX, player.lastY, player.lastZ);

                BoatMovement.doBoatMovement(player);

            } else if (player.playerVehicle instanceof AbstractHorse) {

                player.boundingBox = GetBoundingBox.getHorseBoundingBox(player.lastX, player.lastY, player.lastZ, (AbstractHorse) player.playerVehicle);

                new PlayerBaseTick(player).doBaseTick();
                new MovementTickerHorse(player).livingEntityTravel();

            } else if (player.playerVehicle instanceof Pig) {

                player.boundingBox = GetBoundingBox.getPigBoundingBox(player.lastX, player.lastY, player.lastZ, (Pig) player.playerVehicle);

                new PlayerBaseTick(player).doBaseTick();
                new MovementTickerPig(player).livingEntityTravel();
            } else if (player.playerVehicle instanceof Strider) {

                player.boundingBox = GetBoundingBox.getStriderBoundingBox(player.lastX, player.lastY, player.lastZ, (Strider) player.playerVehicle);

                new PlayerBaseTick(player).doBaseTick();
                new MovementTickerStrider(player).livingEntityTravel();
            }


            // Teleporting overwrites all movements
            if (player.isJustTeleported) {
                player.baseTickSetX(0);
                player.baseTickSetY(0);
                player.baseTickSetZ(0);
                player.predictedVelocity = new Vector();

                player.actualMovement = new Vector(player.x - player.lastX, player.y - player.lastY, player.z - player.lastZ);
            }


            ChatColor color;
            double diff = player.predictedVelocity.distance(player.actualMovement);

            if (diff < 0.01) {
                color = ChatColor.GREEN;
            } else if (diff < 0.1) {
                color = ChatColor.YELLOW;
            } else {
                color = ChatColor.RED;
            }

            player.bukkitPlayer.sendMessage("P: " + color + player.predictedVelocity.getX() + " " + player.predictedVelocity.getY() + " " + player.predictedVelocity.getZ());
            player.bukkitPlayer.sendMessage("A: " + color + player.actualMovement.getX() + " " + player.actualMovement.getY() + " " + player.actualMovement.getZ());
            player.bukkitPlayer.sendMessage("O:" + color + player.predictedVelocity.distance(player.actualMovement));

            GrimAC.plugin.getLogger().info(player.x + " " + player.y + " " + player.z);
            GrimAC.plugin.getLogger().info(player.lastX + " " + player.lastY + " " + player.lastZ);
            GrimAC.plugin.getLogger().info(player.bukkitPlayer.getName() + "P: " + color + player.predictedVelocity.getX() + " " + player.predictedVelocity.getY() + " " + player.predictedVelocity.getZ());
            GrimAC.plugin.getLogger().info(player.bukkitPlayer.getName() + "A: " + color + player.actualMovement.getX() + " " + player.actualMovement.getY() + " " + player.actualMovement.getZ());


            //Bukkit.broadcastMessage("O: " + color + (player.predictedVelocity.getX() - +player.actualMovement.getX()) + " " + (player.predictedVelocity.getY() - player.actualMovement.getY()) + " " + (player.predictedVelocity.getZ() - player.actualMovement.getZ()));

        } catch (Exception e) {
            e.printStackTrace();

            // Fail open
            player.clientVelocity = player.actualMovement.clone();
        }

        player.lastX = player.x;
        player.lastY = player.y;
        player.lastZ = player.z;
        player.lastXRot = player.xRot;
        player.lastYRot = player.yRot;
        player.lastOnGround = player.onGround;
        player.lastClimbing = player.isClimbing;
        player.isJustTeleported = false;

        if (player.lastTransactionReceived != player.packetLastTransactionReceived) {
            player.lastLastTransactionReceived = player.lastTransactionReceived;
        }

        player.lastTransactionReceived = player.packetLastTransactionReceived;


        player.vehicleForward = (float) Math.min(0.98, Math.max(-0.98, data.vehicleForward));
        player.vehicleHorizontal = (float) Math.min(0.98, Math.max(-0.98, data.vehicleHorizontal));

        if (player.tasksNotFinished.getAndDecrement() > 1) {
            PredictionData nextData;

            // We KNOW that there is data in the queue
            // However the other thread increments this value BEFORE adding it to the LinkedQueue
            // Meaning it could increment the value, we read value, and it hasn't been added yet
            // So we have to loop until it's added
            //
            // In reality this should never occur, and if it does it should only happen once.
            // In theory it's good to design an asynchronous system that can never break
            do {
                nextData = queuedPredictions.get(data.player.playerUUID).poll();
            } while (nextData == null);

            PredictionData finalNextData = nextData;
            executor.submit(() -> check(finalNextData));
        }
    }

    // Transaction is from server -> client -> server
    //  Despite the addition of server -> client latency, there is a guarantee:
    //  The needed movement packets should not surpass the ID of latest transaction packet sent
    //
    // For speed checks under 0.03:
    // - We keep track of the transaction ID we just got
    // - We add the number of ticks required to get that movement.
    // This is calculated by looping water/lava tick additions and multipliers found in the player base tick for each tick.
    // We then the wanted movement vector normalized to 1 as the inputs.  If we haven't gotten to the actual movement, keep on ticking.
    //
    // When the player has stopped moving, despite not knowing how long the player has stopped moving, we still have guarantees:
    // - Any amount of movement still increments the transaction ID by one.
    // To stop lag compensation from being too lenient, don’t let movement id fall behind the last transaction ID received
    // - If a delta movement of 0, 0, 0 has been sent, increment movement id by 20
    //
    // What this accomplishes is a perfect lag compensation system:
    // - We will never give more lenience than we have to
    // - We still allow bursts of packets
    //
    // This assumes the following:
    // - Collision will never allow for faster movement, which they shouldn't
    // - Base tick additions and multipliers don't change between client ticks between the two movements.
    //
    // The latter assumption isn't true but with 0.03 movement it isn't enough to break the checks.
    //
    // Here is an example:
    // Let's say the player moved 0.03 blocks in lava
    // Our prediction is that they moved 0.005 blocks in lava
    // A naive programmer may simply divide 0.03 / 0.005 but that doesn't work
    //
    //
    // tl;dr: I made a perfectly lag compensated speed check
    public static void handleSkippedTicks(GrimPlayer player) {
        Vector wantedMovement = player.actualMovement.clone();
        Vector theoreticalOutput = player.predictedVelocity.clone();

        int x = 0;

        // < 0.03 we don't care about checking flying - people flying go fast enough it doesn't matter.
        // As flying players aren't affected by lava speed or cobwebs
        if (player.isFlying) return;

        // Double check that the player didn't toggle fly
        PredictionData nextData = queuedPredictions.get(player.playerUUID).peek();
        if (nextData != null) {
            if (nextData.isFlying) return;
        } else {
            // Update to the latest and check if flying
            // Flight can't be rapidly toggled so we don't need to check off -> on -> off
            player.lastTransactionSent.set(player.packetLastTransactionReceived);
            if (player.packetFlyingDanger && player.compensatedFlying.getCanPlayerFlyLagCompensated()) {
                return;
            }
        }

        // Give the most optimistic scenario for movement speed
        if (player.isPacketSprintingChange) player.isSprinting = true;
        if (player.isPacketSneakingChange) player.isSneaking = false;

        boolean optimisticCrouching = !player.specialFlying && !player.isSwimming && PlayerBaseTick.canEnterPose(player, Pose.CROUCHING, player.x, player.y, player.z)
                && (player.wasSneaking || player.bukkitPlayer.isSleeping() || !PlayerBaseTick.canEnterPose(player, Pose.STANDING, player.x, player.y, player.z));

        Vector optimisticStuckSpeed = player.lastStuckSpeedMultiplier;
        if (player.stuckSpeedMultiplier.lengthSquared() > player.lastStuckSpeedMultiplier.lengthSquared()) {
            optimisticStuckSpeed = player.stuckSpeedMultiplier;
        }

        // TODO: Exempt/fix if speed/potions change between movement ticks

        if (player.couldSkipTick && wantedMovement.lengthSquared() > theoreticalOutput.lengthSquared() * 1.25) {
            for (x = 0; x < 19; x++) {
                // Set to detect 1% speed increase < 0.03 such as in lava

                if (wantedMovement.length() / theoreticalOutput.length() < 1.01) {
                    break;
                }

                new PlayerBaseTick(player).doBaseTick();
                new MovementTickerSlow(player, optimisticCrouching, optimisticStuckSpeed, wantedMovement, theoreticalOutput).playerEntityTravel();
                theoreticalOutput.add(player.predictedVelocity);

                Bukkit.broadcastMessage("Adding " + player.predictedVelocity);
            }
        }

        Bukkit.broadcastMessage("Skipped ticks " + x + " last move " + player.movementTransaction + " recent " + player.lastTransactionReceived);
        Bukkit.broadcastMessage("Predicted velocity " + theoreticalOutput);
        Bukkit.broadcastMessage("Actual velocity " + player.actualMovement);
        player.movementTransaction += x + 1;

        // This is going to lead to some bypasses
        // For example, noclip would be able to abuse this
        // Oh well, I'll just say it's a "proof of concept" then it's fine
        if (x > 0) {
            player.predictedVelocity = player.actualMovement.clone();
        }

        if (player.movementTransaction > player.lastTransactionSent.get()) {
            Bukkit.broadcastMessage(ChatColor.RED + "Player has speed!");
        }

        player.movementTransaction = Math.max(player.movementTransaction, player.lastTransactionReceived);
    }

    public static Vector getBestContinuousInput(boolean isCrouching, Vector theoreticalInput) {
        double bestPossibleX;
        double bestPossibleZ;

        if (isCrouching) {
            bestPossibleX = Math.min(Math.max(-0.294, theoreticalInput.getX()), 0.294);
            bestPossibleZ = Math.min(Math.max(-0.294, theoreticalInput.getZ()), 0.294);
        } else {
            bestPossibleX = Math.min(Math.max(-0.98, theoreticalInput.getX()), 0.98);
            bestPossibleZ = Math.min(Math.max(-0.98, theoreticalInput.getZ()), 0.98);
        }

        Vector inputVector = new Vector(bestPossibleX, 0, bestPossibleZ);

        if (inputVector.lengthSquared() > 1) inputVector.normalize();

        return inputVector;
    }

    // These math equations are based off of the vanilla equations, made impossible to divide by 0
    public static Vector getBestTheoreticalPlayerInput(Vector wantedMovement, float f, float f2) {
        float f3 = Mth.sin(f2 * 0.017453292f);
        float f4 = Mth.cos(f2 * 0.017453292f);

        float bestTheoreticalX = (float) (f3 * wantedMovement.getZ() + f4 * wantedMovement.getX()) / (f3 * f3 + f4 * f4) / f;
        float bestTheoreticalZ = (float) (-f3 * wantedMovement.getX() + f4 * wantedMovement.getZ()) / (f3 * f3 + f4 * f4) / f;

        return new Vector(bestTheoreticalX, 0, bestTheoreticalZ);
    }

    @EventHandler
    public void playerJoinEvent(PlayerJoinEvent event) {
        queuedPredictions.put(event.getPlayer().getUniqueId(), new ConcurrentLinkedQueue<>());
    }

    @EventHandler
    public void playerQuitEvent(PlayerQuitEvent event) {
        queuedPredictions.remove(event.getPlayer().getUniqueId());
    }
}
