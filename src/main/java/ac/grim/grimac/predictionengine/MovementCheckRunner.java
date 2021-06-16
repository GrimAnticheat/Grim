package ac.grim.grimac.predictionengine;

import ac.grim.grimac.GrimAC;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.predictionengine.movementTick.MovementTickerHorse;
import ac.grim.grimac.predictionengine.movementTick.MovementTickerPig;
import ac.grim.grimac.predictionengine.movementTick.MovementTickerPlayer;
import ac.grim.grimac.predictionengine.movementTick.MovementTickerStrider;
import ac.grim.grimac.predictionengine.predictions.PredictionEngineNormal;
import ac.grim.grimac.utils.data.PredictionData;
import ac.grim.grimac.utils.data.VectorData;
import ac.grim.grimac.utils.data.packetentity.PacketEntityHorse;
import ac.grim.grimac.utils.enums.EntityType;
import ac.grim.grimac.utils.nmsImplementations.Collisions;
import ac.grim.grimac.utils.nmsImplementations.GetBoundingBox;
import ac.grim.grimac.utils.nmsImplementations.XMaterial;
import ac.grim.grimac.utils.threads.CustomThreadPoolExecutor;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.github.retrooper.packetevents.utils.player.ClientVersion;
import io.github.retrooper.packetevents.utils.vector.Vector3d;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Pig;
import org.bukkit.entity.Strider;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
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
public class MovementCheckRunner {
    public static ConcurrentHashMap<UUID, ConcurrentLinkedQueue<PredictionData>> queuedPredictions = new ConcurrentHashMap<>();
    public static CustomThreadPoolExecutor executor =
            new CustomThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS,
                    new LinkedBlockingQueue<>(), new ThreadFactoryBuilder().setDaemon(true).build());
    public static ConcurrentLinkedQueue<PredictionData> waitingOnServerQueue = new ConcurrentLinkedQueue<>();
    // List instead of Set for consistency in debug output
    static List<MovementCheck> movementCheckListeners = new ArrayList<>();

    static int temp = 0;

    public static void processAndCheckMovementPacket(PredictionData data) {
        data.player.packetStateData.packetPlayerX = data.playerX;
        data.player.packetStateData.packetPlayerY = data.playerY;
        data.player.packetStateData.packetPlayerZ = data.playerZ;
        data.player.packetStateData.packetPlayerXRot = data.xRot;
        data.player.packetStateData.packetPlayerYRot = data.yRot;

        // Support teleports without teleport confirmations
        Vector3d teleportPos = data.player.teleports.peek();
        if (teleportPos != null && teleportPos.getX() == data.playerX && teleportPos.getY() == data.playerY && teleportPos.getZ() == data.playerZ) {
            data.player.teleports.poll();
            data.isJustTeleported = true;
            data.player.timerCheck.exempt = 60; // Exempt for 3 seconds on teleport
        }

        data.player.timerCheck.processMovementPacket(data.playerX, data.playerY, data.playerZ, data.xRot, data.yRot);

        if (data.player.tasksNotFinished.getAndIncrement() == 0) {
            executor.submit(() -> check(data));
        } else {
            queuedPredictions.get(data.player.playerUUID).add(data);
        }
    }

    public static void check(PredictionData data) {
        GrimPlayer player = data.player;

        if (data.minimumTickRequiredToContinue > GrimAC.getCurrentTick()) {
            waitingOnServerQueue.add(data);
            return;
        }

        player.compensatedWorld.tickUpdates(data.lastTransaction);
        player.compensatedEntities.tickUpdates(data.lastTransaction);
        player.compensatedWorld.tickPlayerInPistonPushingArea();

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

            player.isFlying = player.compensatedFlying.canFlyLagCompensated(data.lastTransaction);
            player.isClimbing = Collisions.onClimbable(player);
            player.isGliding = player.compensatedElytra.isGlidingLagCompensated(data.lastTransaction) && !player.isFlying;
            player.specialFlying = player.onGround && !player.isFlying && player.wasFlying || player.isFlying;

            temp = data.lastTransaction;

            // Stop stuff like clients using elytra in a vehicle...
            if (player.inVehicle) {
                player.isFlying = false;
                player.isClimbing = false;
                player.isGliding = false;
                player.specialFlying = false;
            }

            player.playerWorld = data.playerWorld;
            player.fallDistance = data.fallDistance;

            if (data.isJustTeleported) {
                player.lastX = player.x;
                player.lastY = player.y;
                player.lastZ = player.z;
            }

            player.movementSpeed = data.movementSpeed;
            player.jumpAmplifier = data.jumpAmplifier;
            player.levitationAmplifier = data.levitationAmplifier;
            player.slowFallingAmplifier = data.slowFallingAmplifier;
            player.dolphinsGraceAmplifier = data.dolphinsGraceAmplifier;
            player.flySpeed = data.flySpeed;
            player.playerVehicle = data.playerVehicle == null ? null : player.compensatedEntities.getEntity(data.playerVehicle);
            player.inVehicle = player.playerVehicle != null;

            player.firstBreadKB = data.firstBreadKB;
            player.possibleKB = data.requiredKB;

            player.firstBreadExplosion = data.firstBreadExplosion;
            player.knownExplosion = data.possibleExplosion;

            // This isn't the final velocity of the player in the tick, only the one applied to the player
            player.actualMovement = new Vector(player.x - player.lastX, player.y - player.lastY, player.z - player.lastZ);
            player.boundingBox = GetBoundingBox.getCollisionBoxForPlayer(player, player.lastX, player.lastY, player.lastZ);

            if (data.isJustTeleported || player.isFirstTick) {
                // Don't let the player move if they just teleported
                player.predictedVelocity = new VectorData(new Vector(), VectorData.VectorType.Teleport);
                player.clientVelocity = new Vector();
            } else if (player.bukkitPlayer.isDead()) {
                // Dead players can't cheat, if you find a way how they could, open an issue
                player.predictedVelocity = new VectorData(player.actualMovement, VectorData.VectorType.Dead);
                player.clientVelocity = new Vector();
            } else if (player.getClientVersion().isOlderThanOrEquals(ClientVersion.v_1_7_10) && player.isFlying ||
                    (XMaterial.getVersion() >= 8 && player.bukkitPlayer.getGameMode() == GameMode.SPECTATOR)) {
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

                if (player.canGroundRiptide = (player.lastOnGround && player.compensatedRiptide.getCanRiptide())) {
                    double addedY = Math.min(player.actualMovement.getY(), 1.1999999F);
                    player.lastOnGround = false;

                    player.boundingBox.offset(0, addedY, 0);
                }

                new PlayerBaseTick(player).doBaseTick();
                new MovementTickerPlayer(player).livingEntityAIStep();

            } else if (player.playerVehicle.type == EntityType.BOAT) {
                BoatMovement.doBoatMovement(player);
            } else if (player.playerVehicle instanceof PacketEntityHorse) {
                new PlayerBaseTick(player).doBaseTick();
                new MovementTickerHorse(player).livingEntityTravel();
            } else if (player.playerVehicle.type == EntityType.PIG) {
                new PlayerBaseTick(player).doBaseTick();
                new MovementTickerPig(player).livingEntityTravel();
            } else if (player.playerVehicle.type == EntityType.STRIDER) {
                new PlayerBaseTick(player).doBaseTick();
                new MovementTickerStrider(player).livingEntityTravel();
            } // If it isn't any of these cases, the player is on a mob they can't control and therefore is exempt

            player.isFirstTick = false;

            Vector offsetVector = player.predictedVelocity.vector.clone().subtract(player.actualMovement);
            double offset = offsetVector.length();

            ChatColor color;

            if (offset < 0.01) {
                color = ChatColor.GREEN;
            } else if (offset < 0.1) {
                color = ChatColor.YELLOW;
            } else {
                color = ChatColor.RED;
            }

            player.knockbackHandler.handlePlayerKb(offset);
            player.explosionHandler.handlePlayerExplosion(offset);
            player.trigHandler.setOffset(offset);
            player.compensatedRiptide.handleRemoveRiptide();

            player.bukkitPlayer.sendMessage("P: " + color + player.predictedVelocity.vector.getX() + " " + player.predictedVelocity.vector.getY() + " " + player.predictedVelocity.vector.getZ());
            player.bukkitPlayer.sendMessage("A: " + color + player.actualMovement.getX() + " " + player.actualMovement.getY() + " " + player.actualMovement.getZ());
            player.bukkitPlayer.sendMessage("O:" + color + offset);

            VectorData last = player.predictedVelocity;
            StringBuilder traceback = new StringBuilder("Traceback: ");

            List<Vector> velocities = new ArrayList<>();
            List<VectorData.VectorType> types = new ArrayList<>();

            // Find the very last vector
            while (last.lastVector != null) {
                velocities.add(last.vector);
                types.add(last.vectorType);
                last = last.lastVector;
            }

            Vector lastAppendedVector = null;
            for (int i = velocities.size(); i-- > 0; ) {
                Vector currentVector = velocities.get(i);
                VectorData.VectorType type = types.get(i);

                if (currentVector.equals(lastAppendedVector)) {
                    continue;
                }

                traceback.append(type).append(": ");
                traceback.append(currentVector).append(" > ");

                lastAppendedVector = last.vector;
            }

            GrimAC.staticGetLogger().info(traceback.toString());
            GrimAC.staticGetLogger().info(player.x + " " + player.y + " " + player.z);
            GrimAC.staticGetLogger().info(player.lastX + " " + player.lastY + " " + player.lastZ);
            GrimAC.staticGetLogger().info(player.bukkitPlayer.getName() + "P: " + color + player.predictedVelocity.vector.getX() + " " + player.predictedVelocity.vector.getY() + " " + player.predictedVelocity.vector.getZ());
            GrimAC.staticGetLogger().info(player.bukkitPlayer.getName() + "A: " + color + player.actualMovement.getX() + " " + player.actualMovement.getY() + " " + player.actualMovement.getZ());
            GrimAC.staticGetLogger().info(player.bukkitPlayer.getName() + "O: " + color + offset);
            GrimAC.staticGetLogger().info("Gliding " + player.isGliding + " trans " + temp);

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

        player.lastTransactionBeforeLastMovement = player.packetStateData.packetLastTransactionReceived;


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
}
