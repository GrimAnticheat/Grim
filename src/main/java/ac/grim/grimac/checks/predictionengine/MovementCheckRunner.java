package ac.grim.grimac.checks.predictionengine;

import ac.grim.grimac.checks.movement.TimerCheck;
import ac.grim.grimac.checks.predictionengine.movementTick.MovementTickerHorse;
import ac.grim.grimac.checks.predictionengine.movementTick.MovementTickerPig;
import ac.grim.grimac.checks.predictionengine.movementTick.MovementTickerPlayer;
import ac.grim.grimac.checks.predictionengine.movementTick.MovementTickerStrider;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.data.PredictionData;
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
        TimerCheck.processMovementPacket(data.grimPlayer);

        if (data.grimPlayer.tasksNotFinished.getAndIncrement() == 0) {
            executor.submit(() -> check(data));
        } else {
            queuedPredictions.get(data.grimPlayer.playerUUID).add(data);
        }
    }
    public static void check(PredictionData data) {
        GrimPlayer grimPlayer = data.grimPlayer;

        // If we don't catch it, the exception is silently eaten by ThreadPoolExecutor
        try {
            grimPlayer.x = data.playerX;
            grimPlayer.y = data.playerY;
            grimPlayer.z = data.playerZ;
            grimPlayer.xRot = data.xRot;
            grimPlayer.yRot = data.yRot;
            grimPlayer.onGround = data.onGround;
            grimPlayer.lastSprinting = grimPlayer.isSprinting;
            grimPlayer.isSprinting = data.isSprinting;
            grimPlayer.wasSneaking = grimPlayer.isSneaking;
            grimPlayer.isSneaking = data.isSneaking;
            grimPlayer.specialFlying = grimPlayer.onGround && !data.isFlying && grimPlayer.isFlying || data.isFlying;
            grimPlayer.isFlying = data.isFlying;
            grimPlayer.isClimbing = data.isClimbing;
            grimPlayer.isFallFlying = data.isFallFlying;
            grimPlayer.playerWorld = data.playerWorld;
            grimPlayer.fallDistance = data.fallDistance;

            grimPlayer.movementSpeed = data.movementSpeed;
            grimPlayer.jumpAmplifier = data.jumpAmplifier;
            grimPlayer.levitationAmplifier = data.levitationAmplifier;
            grimPlayer.flySpeed = data.flySpeed;
            grimPlayer.inVehicle = data.inVehicle;
            grimPlayer.playerVehicle = data.playerVehicle;


            // This isn't the final velocity of the player in the tick, only the one applied to the player
            grimPlayer.actualMovement = new Vector(grimPlayer.x - grimPlayer.lastX, grimPlayer.y - grimPlayer.lastY, grimPlayer.z - grimPlayer.lastZ);

            if (!grimPlayer.inVehicle) {
                grimPlayer.boundingBox = GetBoundingBox.getPlayerBoundingBox(grimPlayer, grimPlayer.lastX, grimPlayer.lastY, grimPlayer.lastZ);

                // This is not affected by any movement
                new PlayerBaseTick(grimPlayer).doBaseTick();

                // baseTick occurs before this
                new MovementTickerPlayer(grimPlayer).livingEntityAIStep();

                handleSkippedTicks(grimPlayer);
            } else if (grimPlayer.playerVehicle instanceof Boat) {

                // TODO: We will have to handle teleports (occurs multiple times a second due to vanilla glitchyness)
                grimPlayer.boundingBox = GetBoundingBox.getBoatBoundingBox(grimPlayer.lastX, grimPlayer.lastY, grimPlayer.lastZ);

                BoatMovement.doBoatMovement(grimPlayer);

            } else if (grimPlayer.playerVehicle instanceof AbstractHorse) {

                grimPlayer.boundingBox = GetBoundingBox.getHorseBoundingBox(grimPlayer.lastX, grimPlayer.lastY, grimPlayer.lastZ, (AbstractHorse) grimPlayer.playerVehicle);

                new PlayerBaseTick(grimPlayer).doBaseTick();
                new MovementTickerHorse(grimPlayer).livingEntityTravel();

            } else if (grimPlayer.playerVehicle instanceof Pig) {

                grimPlayer.boundingBox = GetBoundingBox.getPigBoundingBox(grimPlayer.lastX, grimPlayer.lastY, grimPlayer.lastZ, (Pig) grimPlayer.playerVehicle);

                new PlayerBaseTick(grimPlayer).doBaseTick();
                new MovementTickerPig(grimPlayer).livingEntityTravel();
            } else if (grimPlayer.playerVehicle instanceof Strider) {

                grimPlayer.boundingBox = GetBoundingBox.getStriderBoundingBox(grimPlayer.lastX, grimPlayer.lastY, grimPlayer.lastZ, (Strider) grimPlayer.playerVehicle);

                new PlayerBaseTick(grimPlayer).doBaseTick();
                new MovementTickerStrider(grimPlayer).livingEntityTravel();
            }


            // Teleporting overwrites all movements
            if (grimPlayer.isJustTeleported) {
                grimPlayer.baseTickSetX(0);
                grimPlayer.baseTickSetY(0);
                grimPlayer.baseTickSetZ(0);
                grimPlayer.predictedVelocity = new Vector();

                grimPlayer.actualMovement = new Vector(grimPlayer.x - grimPlayer.lastX, grimPlayer.y - grimPlayer.lastY, grimPlayer.z - grimPlayer.lastZ);
            }


            ChatColor color;
            double diff = grimPlayer.predictedVelocity.distance(grimPlayer.actualMovement);

            if (diff < 0.01) {
                color = ChatColor.GREEN;
            } else if (diff < 0.1) {
                color = ChatColor.YELLOW;
            } else {
                color = ChatColor.RED;
            }

            /*grimPlayer.bukkitPlayer.sendMessage("P: " + color + grimPlayer.predictedVelocity.getX() + " " + grimPlayer.predictedVelocity.getY() + " " + grimPlayer.predictedVelocity.getZ());
            grimPlayer.bukkitPlayer.sendMessage("A: " + color + grimPlayer.actualMovement.getX() + " " + grimPlayer.actualMovement.getY() + " " + grimPlayer.actualMovement.getZ());
            grimPlayer.bukkitPlayer.sendMessage("O:" + color + grimPlayer.predictedVelocity.distance(grimPlayer.actualMovement));

            GrimAC.plugin.getLogger().info(grimPlayer.x + " " + grimPlayer.y + " " + grimPlayer.z);
            GrimAC.plugin.getLogger().info(grimPlayer.lastX + " " + grimPlayer.lastY + " " + grimPlayer.lastZ);
            GrimAC.plugin.getLogger().info(grimPlayer.bukkitPlayer.getName() + "P: " + color + grimPlayer.predictedVelocity.getX() + " " + grimPlayer.predictedVelocity.getY() + " " + grimPlayer.predictedVelocity.getZ());
            GrimAC.plugin.getLogger().info(grimPlayer.bukkitPlayer.getName() + "A: " + color + grimPlayer.actualMovement.getX() + " " + grimPlayer.actualMovement.getY() + " " + grimPlayer.actualMovement.getZ());
            */

            //Bukkit.broadcastMessage("O: " + color + (grimPlayer.predictedVelocity.getX() - +grimPlayer.actualMovement.getX()) + " " + (grimPlayer.predictedVelocity.getY() - grimPlayer.actualMovement.getY()) + " " + (grimPlayer.predictedVelocity.getZ() - grimPlayer.actualMovement.getZ()));

        } catch (Exception e) {
            e.printStackTrace();

            // Fail open
            grimPlayer.clientVelocity = grimPlayer.actualMovement.clone();
        }

        grimPlayer.lastX = grimPlayer.x;
        grimPlayer.lastY = grimPlayer.y;
        grimPlayer.lastZ = grimPlayer.z;
        grimPlayer.lastXRot = grimPlayer.xRot;
        grimPlayer.lastYRot = grimPlayer.yRot;
        grimPlayer.lastOnGround = grimPlayer.onGround;
        grimPlayer.lastClimbing = grimPlayer.isClimbing;
        grimPlayer.isJustTeleported = false;


        grimPlayer.vehicleForward = (float) Math.min(0.98, Math.max(-0.98, data.vehicleForward));
        grimPlayer.vehicleHorizontal = (float) Math.min(0.98, Math.max(-0.98, data.vehicleHorizontal));

        if (grimPlayer.tasksNotFinished.getAndDecrement() > 1) {
            PredictionData nextData;

            // We KNOW that there is data in the queue
            // However the other thread increments this value BEFORE adding it to the LinkedQueue
            // Meaning it could increment the value, we read value, and it hasn't been added yet
            // So we have to loop until it's added
            //
            // In reality this should never occur, and if it does it should only happen once.
            // In theory it's good to design an asynchronous system that can never break
            do {
                nextData = queuedPredictions.get(data.grimPlayer.playerUUID).poll();
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
    public static void handleSkippedTicks(GrimPlayer grimPlayer) {
        Vector wantedMovement = grimPlayer.actualMovement.clone();
        Vector totalMovement = grimPlayer.predictedVelocity.clone();
        int x = 0;

        // Fuck it, proof of concept. Just use the client velocity plus some math
        // TODO: Double check that the player's velocity would have dipped below 0.03
        if (grimPlayer.couldSkipTick && wantedMovement.lengthSquared() > totalMovement.lengthSquared() * 1.25) {
            for (x = 0; x < 19; x++) {
                // Set to detect 1% speed increase < 0.03 such as in lava
                if (grimPlayer.actualMovement.length() / (x + 1) / grimPlayer.predictedVelocity.length() < 1.01) {
                    break;
                }
            }
        }

        Bukkit.broadcastMessage("Skipped ticks " + x + " last move " + grimPlayer.movementTransaction + " recent " + grimPlayer.lastTransactionReceived);
        Bukkit.broadcastMessage("Predicted velocity " + grimPlayer.predictedVelocity);
        Bukkit.broadcastMessage("Actual velocity " + grimPlayer.actualMovement);
        grimPlayer.movementTransaction += x + 1;

        // This is going to lead to some bypasses
        // For example, noclip would be able to abuse this
        // Oh well, I'll just say it's a "proof of concept" then it's fine
        if (x > 0) {
            grimPlayer.predictedVelocity = grimPlayer.actualMovement.clone();
        }

        if (grimPlayer.movementTransaction > grimPlayer.lastTransactionReceived + 2) {
            Bukkit.broadcastMessage(ChatColor.RED + "Player has speed!");
        }

        grimPlayer.movementTransaction = Math.max(grimPlayer.movementTransaction, grimPlayer.lastTransactionReceived);
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
