package ac.grim.grimac.checks.combat;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.collisions.datatypes.SimpleCollisionBox;
import ac.grim.grimac.utils.data.ReachMovementData;
import ac.grim.grimac.utils.data.packetentity.PlayerReachEntity;
import ac.grim.grimac.utils.nmsImplementations.GetBoundingBox;
import ac.grim.grimac.utils.nmsImplementations.Ray;
import ac.grim.grimac.utils.nmsImplementations.RayTrace;
import io.github.retrooper.packetevents.utils.vector.Vector3d;
import it.unimi.dsi.fastutil.ints.Int2ObjectLinkedOpenHashMap;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Reach {

    public final Int2ObjectLinkedOpenHashMap<PlayerReachEntity> entityMap = new Int2ObjectLinkedOpenHashMap<>();
    private final GrimPlayer player;
    private final ConcurrentLinkedQueue<ReachMovementData> transactionReachQueue = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<Integer> playerAttackQueue = new ConcurrentLinkedQueue<>();

    // This is a memory leak to detect a desync
    private final List<Integer> desyncTrans = new ArrayList<>();

    public Reach(GrimPlayer player) {
        this.player = player;
    }

    public void checkReach(int entityID) {
        Bukkit.broadcastMessage("Packet last trans before attack " + player.packetStateData.packetLastTransactionReceived.get());
        playerAttackQueue.add(entityID);

        if (desyncTrans.contains(player.packetStateData.packetLastTransactionReceived.get()))
            Bukkit.broadcastMessage(ChatColor.RED + "A DESYNC HAS OCCURED!  PANIC");
    }

    public void handleMovement(float xRot, float yRot) {
        Integer attackQueue = playerAttackQueue.poll();
        while (attackQueue != null) {
            PlayerReachEntity reachEntity = entityMap.get((int) attackQueue);

            Vector attackerDirection = RayTrace.getDirection(player, xRot, yRot);
            Vector direction = new Vector(attackerDirection.getX(), attackerDirection.getY(), attackerDirection.getZ());

            Ray attackerRay = new Ray(new Vector(player.packetStateData.packetPlayerX, player.packetStateData.packetPlayerY + 1.62, player.packetStateData.packetPlayerZ), direction);

            attackerRay.highlight(player, 3, 0.01);

            Vector intersection = reachEntity.currentLocation.copy().expand(0.1).intersectsRay(attackerRay, 0, Float.MAX_VALUE);

            Bukkit.broadcastMessage(ChatColor.AQUA + "Checked hitbox size " + (reachEntity.currentLocation.maxY - reachEntity.currentLocation.minY));

            if (Math.abs((reachEntity.currentLocation.maxX - reachEntity.currentLocation.minX) - 0.6) > 0.01) {
                Bukkit.broadcastMessage(ChatColor.GOLD + "We recovered from a desync!");
            }

            if (intersection == null) {
                Bukkit.broadcastMessage(ChatColor.RED + "Player failed hitbox check!");
            } else {
                double reach = new Vector(player.packetStateData.packetPlayerX, player.packetStateData.packetPlayerY + 1.62, player.packetStateData.packetPlayerZ).distance(intersection);
                if (reach < 3) {
                    Bukkit.broadcastMessage(ChatColor.GREEN + "Intersected!  Reach was " + reach);
                } else {
                    Bukkit.broadcastMessage(ChatColor.RED + "Intersected!  Reach was " + reach);
                }
            }

            attackQueue = playerAttackQueue.poll();
        }

        // Move the current bounding box a third towards the target bounding box.
        for (PlayerReachEntity entity : entityMap.values()) {
            double minX = entity.currentLocation.minX + ((entity.targetLocation.minX - entity.currentLocation.minX) / 3);
            double maxX = entity.currentLocation.maxX + ((entity.targetLocation.maxX - entity.currentLocation.maxX) / 3);
            double minY = entity.currentLocation.minY + ((entity.targetLocation.minY - entity.currentLocation.minY) / 3);
            double maxY = entity.currentLocation.maxY + ((entity.targetLocation.maxY - entity.currentLocation.maxY) / 3);
            double minZ = entity.currentLocation.minZ + ((entity.targetLocation.minZ - entity.currentLocation.minZ) / 3);
            double maxZ = entity.currentLocation.maxZ + ((entity.targetLocation.maxZ - entity.currentLocation.maxZ) / 3);

            entity.currentLocation = new SimpleCollisionBox(minX, minY, minZ, maxX, maxY, maxZ);
        }
    }

    public void handleTransaction(int transactionID) {
        ReachMovementData nextTrans = transactionReachQueue.peek();

        if (nextTrans != null) {
            if (transactionID == nextTrans.transactionID) {
                // Create a bounding box taking the minimums and maximums of the previous packet target and the new target,
                // meaning that the bounding box will become larger than the player’s actual bounding box.
                PlayerReachEntity entity = entityMap.get(nextTrans.entityID);
                entity.relativeMoveLocation = nextTrans.newPos;

                double nextX = entity.relativeMoveLocation.getX();
                double nextY = entity.relativeMoveLocation.getY();
                double nextZ = entity.relativeMoveLocation.getZ();

                SimpleCollisionBox newLoc = GetBoundingBox.getBoundingBoxFromPosAndSize(nextX, nextY, nextZ, 0.6, 1.8);
                double minX = Math.min(entity.targetLocation.minX, newLoc.minX);
                double maxX = Math.max(entity.targetLocation.maxX, newLoc.maxX);
                double minY = Math.min(entity.targetLocation.minY, newLoc.minY);
                double maxY = Math.max(entity.targetLocation.maxY, newLoc.maxY);
                double minZ = Math.min(entity.targetLocation.minZ, newLoc.minZ);
                double maxZ = Math.max(entity.targetLocation.maxZ, newLoc.maxZ);

                entity.targetLocation = new SimpleCollisionBox(minX, minY, minZ, maxX, maxY, maxZ);

                if (player.bukkitPlayer.getName().equalsIgnoreCase("DefineOutside"))
                    Bukkit.broadcastMessage(ChatColor.AQUA + "Set uncertain hitbox size " + (entity.targetLocation.maxY - entity.targetLocation.minY) + " " + System.currentTimeMillis() + " " + player.packetStateData.packetLastTransactionReceived.get());

            } else if (transactionID - 1 == nextTrans.transactionID) {
                PlayerReachEntity entity = entityMap.get(nextTrans.entityID);

                // We have already added the move last transaction
                double nextX = entity.relativeMoveLocation.getX();
                double nextY = entity.relativeMoveLocation.getY();
                double nextZ = entity.relativeMoveLocation.getZ();

                entity.targetLocation = GetBoundingBox.getBoundingBoxFromPosAndSize(nextX, nextY, nextZ, 0.6, 1.8);

                if (player.bukkitPlayer.getName().equalsIgnoreCase("DefineOutside"))
                    Bukkit.broadcastMessage(ChatColor.GOLD + "Set certain hitbox size " + (entity.targetLocation.maxY - entity.targetLocation.minY) + " " + System.currentTimeMillis() + " " + player.packetStateData.packetLastTransactionReceived.get());

                transactionReachQueue.poll();
            }
        }
    }

    public void handleSpawnPlayer(int playerID, Vector3d spawnPosition) {
        entityMap.put(playerID, new PlayerReachEntity(spawnPosition.getX(), spawnPosition.getY(), spawnPosition.getZ()));
    }

    public void handleMoveEntity(int entityId, double deltaX, double deltaY, double deltaZ, boolean isRelative) {
        PlayerReachEntity reachEntity = entityMap.get(entityId);

        if (reachEntity != null) {
            // Update the tracked server's entity position
            if (isRelative)
                reachEntity.serverPos = reachEntity.serverPos.add(new Vector3d(deltaX, deltaY, deltaZ));
            else
                reachEntity.serverPos = new Vector3d(deltaX, deltaY, deltaZ);

            int lastTrans = player.lastTransactionSent.get();

            desyncTrans.add(lastTrans);

            if (player.bukkitPlayer.getName().equalsIgnoreCase("DefineOutside"))
                Bukkit.broadcastMessage("Trans before " + lastTrans);

            transactionReachQueue.add(new ReachMovementData(lastTrans, entityId, reachEntity.serverPos));
        }
    }
}