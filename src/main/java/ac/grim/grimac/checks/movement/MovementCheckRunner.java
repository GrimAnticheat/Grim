package ac.grim.grimac.checks.movement;

import ac.grim.grimac.GrimPlayer;
import ac.grim.grimac.utils.data.PredictionData;
import ac.grim.grimac.utils.nmsImplementations.GetBoundingBox;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
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
    // List instead of Set for consistency in debug output
    static List<MovementCheck> movementCheckListeners = new ArrayList<>();

    // I actually don't know how many threads is good, more testing is needed!
    static ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(4);

    static ConcurrentHashMap<UUID, ConcurrentLinkedQueue<PredictionData>> queuedPredictions = new ConcurrentHashMap<>();

    public static void addQueuedPrediction(PredictionData data) {
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
            grimPlayer.isSprinting = data.isSprinting;
            grimPlayer.wasSneaking = grimPlayer.isSneaking;
            grimPlayer.isSneaking = data.isSneaking;
            grimPlayer.isFlying = data.isFlying;
            grimPlayer.isSwimming = data.isSwimming;
            grimPlayer.playerWorld = data.playerWorld;
            grimPlayer.movementPacketMilliseconds = System.currentTimeMillis();

            // TODO: Make gliding async safe
            // TODO: Actually get client version
            grimPlayer.boundingBox = GetBoundingBox.getPlayerBoundingBox(grimPlayer.lastX, grimPlayer.lastY, grimPlayer.lastZ, grimPlayer.wasSneaking, grimPlayer.bukkitPlayer.isGliding(), grimPlayer.isSwimming, grimPlayer.bukkitPlayer.isSleeping(), grimPlayer.clientVersion);


            /*for (MovementCheck movementCheck : movementCheckListeners) {
                movementCheck.checkMovement(grimPlayer);
            }*/

            grimPlayer.movementEventMilliseconds = System.currentTimeMillis();

            // This isn't the final velocity of the player in the tick, only the one applied to the player
            grimPlayer.actualMovement = new Vector(grimPlayer.x - grimPlayer.lastX, grimPlayer.y - grimPlayer.lastY, grimPlayer.z - grimPlayer.lastZ);

            // This is not affected by any movement
            new PlayerBaseTick(grimPlayer).doBaseTick();

            // baseTick occurs before this
            new MovementVelocityCheck(grimPlayer).livingEntityAIStep();

            ChatColor color;
            double diff = grimPlayer.predictedVelocity.distance(grimPlayer.actualMovement);

            if (diff < 0.01) {
                color = ChatColor.GREEN;
            } else if (diff < 0.1) {
                color = ChatColor.YELLOW;
            } else {
                color = ChatColor.RED;
            }

            //Bukkit.broadcastMessage("Time since last event " + (grimPlayer.movementEventMilliseconds - grimPlayer.lastMovementEventMilliseconds + "Time taken " + (System.nanoTime() - startTime)));
            Bukkit.broadcastMessage("P: " + color + grimPlayer.predictedVelocity.getX() + " " + grimPlayer.predictedVelocity.getY() + " " + grimPlayer.predictedVelocity.getZ());
            Bukkit.broadcastMessage("A: " + color + grimPlayer.actualMovement.getX() + " " + grimPlayer.actualMovement.getY() + " " + grimPlayer.actualMovement.getZ());
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
        grimPlayer.lastSneaking = grimPlayer.wasSneaking;
        grimPlayer.lastClimbing = grimPlayer.entityPlayer.isClimbing();
        grimPlayer.lastMovementPacketMilliseconds = grimPlayer.movementPacketMilliseconds;
        grimPlayer.lastMovementEventMilliseconds = grimPlayer.movementEventMilliseconds;


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

    @EventHandler
    public void playerJoinEvent(PlayerJoinEvent event) {
        queuedPredictions.put(event.getPlayer().getUniqueId(), new ConcurrentLinkedQueue<>());
    }

    @EventHandler
    public void playerQuitEvent(PlayerQuitEvent event) {
        queuedPredictions.remove(event.getPlayer().getUniqueId());
    }
}
