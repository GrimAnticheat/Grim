package ac.grim.grimac.checks.movement;

import ac.grim.grimac.GrimPlayer;
import ac.grim.grimac.events.anticheat.PlayerBaseTick;
import ac.grim.grimac.utils.data.PredictionData;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
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

    // In testing 4 threads seemed to have the best throughput, although this is hardware dependent
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

        grimPlayer.x = data.playerX;
        grimPlayer.y = data.playerY;
        grimPlayer.z = data.playerZ;
        grimPlayer.xRot = data.xRot;
        grimPlayer.yRot = data.yRot;
        grimPlayer.onGround = data.onGround;
        grimPlayer.isSprinting = data.isSprinting;
        grimPlayer.isSneaking = data.isSneaking;
        grimPlayer.isFlying = data.isFlying;
        grimPlayer.isSwimming = data.isSwimming;
        grimPlayer.boundingBox = data.boundingBox;
        grimPlayer.playerWorld = data.playerWorld;
        grimPlayer.movementPacketMilliseconds = System.currentTimeMillis();


        /*for (MovementCheck movementCheck : movementCheckListeners) {
            movementCheck.checkMovement(grimPlayer);
        }*/

        grimPlayer.movementEventMilliseconds = System.currentTimeMillis();

        Location from = new Location(grimPlayer.playerWorld, grimPlayer.lastX, grimPlayer.lastY, grimPlayer.lastZ);
        Location to = new Location(grimPlayer.playerWorld, grimPlayer.x, grimPlayer.y, grimPlayer.z);

        // This isn't the final velocity of the player in the tick, only the one applied to the player
        grimPlayer.actualMovement = new Vector(to.getX() - from.getX(), to.getY() - from.getY(), to.getZ() - from.getZ());

        // This is not affected by any movement
        new PlayerBaseTick(grimPlayer).doBaseTick();

        // baseTick occurs before this
        new MovementVelocityCheck(grimPlayer).livingEntityAIStep();

        ChatColor color;
        double diff = grimPlayer.predictedVelocity.distance(grimPlayer.actualMovement);

        if (diff < 0.05) {
            color = ChatColor.GREEN;
        } else if (diff < 0.15) {
            color = ChatColor.YELLOW;
        } else {
            color = ChatColor.RED;
        }

        //Bukkit.broadcastMessage("Time since last event " + (grimPlayer.movementEventMilliseconds - grimPlayer.lastMovementEventMilliseconds + "Time taken " + (System.nanoTime() - startTime)));
        Bukkit.broadcastMessage("P: " + color + grimPlayer.predictedVelocity.getX() + " " + grimPlayer.predictedVelocity.getY() + " " + grimPlayer.predictedVelocity.getZ());
        Bukkit.broadcastMessage("A: " + color + grimPlayer.actualMovement.getX() + " " + grimPlayer.actualMovement.getY() + " " + grimPlayer.actualMovement.getZ());

        grimPlayer.lastX = grimPlayer.x;
        grimPlayer.lastY = grimPlayer.y;
        grimPlayer.lastZ = grimPlayer.z;
        grimPlayer.lastXRot = grimPlayer.xRot;
        grimPlayer.lastYRot = grimPlayer.yRot;
        grimPlayer.lastOnGround = grimPlayer.onGround;
        grimPlayer.lastSneaking = grimPlayer.isSneaking;
        grimPlayer.lastClimbing = grimPlayer.entityPlayer.isClimbing();
        grimPlayer.lastMovementPacketMilliseconds = grimPlayer.movementPacketMilliseconds;
        grimPlayer.lastMovementEventMilliseconds = grimPlayer.movementEventMilliseconds;

        if (grimPlayer.tasksNotFinished.getAndDecrement() > 0) {
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
