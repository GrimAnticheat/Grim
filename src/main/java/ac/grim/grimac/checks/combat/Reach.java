// This file was designed and is an original check for GrimAC
// Copyright (C) 2021 DefineOutside
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program.  If not, see <http://www.gnu.org/licenses/>.
package ac.grim.grimac.checks.combat;

import ac.grim.grimac.GrimAC;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.collisions.datatypes.SimpleCollisionBox;
import ac.grim.grimac.utils.data.ReachMovementData;
import ac.grim.grimac.utils.data.packetentity.PlayerReachEntity;
import ac.grim.grimac.utils.nmsImplementations.ReachUtils;
import io.github.retrooper.packetevents.utils.player.ClientVersion;
import io.github.retrooper.packetevents.utils.vector.Vector3d;
import it.unimi.dsi.fastutil.ints.Int2ObjectLinkedOpenHashMap;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

// You may not copy the check unless you are licensed under GPL
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
        if (desyncTrans.contains(player.packetStateData.packetLastTransactionReceived.get()))
            Bukkit.broadcastMessage(ChatColor.GOLD + "Desync detected!");
        playerAttackQueue.add(entityID);
    }

    public void handleMovement(float xRot, float yRot) {
        Integer attackQueue = playerAttackQueue.poll();
        while (attackQueue != null) {
            PlayerReachEntity reachEntity = entityMap.get((int) attackQueue);

            Vector eyePos = new Vector(player.packetStateData.packetPlayerX, player.packetStateData.packetPlayerY + (player.packetStateData.isPacketSneaking ? 1.54 : 1.62), player.packetStateData.packetPlayerZ);

            Vector attackerDirection = ReachUtils.getLook(player, xRot, yRot);
            Vector endReachPos = eyePos.clone().add(new Vector(attackerDirection.getX() * 6, attackerDirection.getY() * 6, attackerDirection.getZ() * 6));

            SimpleCollisionBox targetBox = reachEntity.getPossibleCollisionBoxes().copy();

            // 1.7 and 1.8 players get a bit of extra hitbox (this is why you should use 1.8 on cross version servers)
            if (player.getClientVersion().isOlderThan(ClientVersion.v_1_9)) {
                targetBox.expand(0.1);
            }

            // This is better than adding to the reach, as 0.03 can cause a player to miss their target
            // Adds some more than 0.03 uncertainty in some cases, but a good trade off for simplicity
            if (!player.packetStateData.didLastMovementIncludePosition)
                targetBox.expand(0.03);

            Vector intercept = ReachUtils.calculateIntercept(targetBox, eyePos, endReachPos);

            //Bukkit.broadcastMessage(ChatColor.AQUA + "Checked x pos " + (targetBox.maxX + targetBox.minX) / 2 + " With size " + (targetBox.maxX - targetBox.minX) + " 0.03? " + (!player.packetStateData.didLastMovementIncludePosition));
            if (reachEntity.oldPacketLocation != null)
                GrimAC.staticGetLogger().info(ChatColor.AQUA + "Old position is " + (reachEntity.oldPacketLocation.targetLocation.maxX + reachEntity.oldPacketLocation.targetLocation.minX) / 2);
            GrimAC.staticGetLogger().info(ChatColor.AQUA + "New position is " + (reachEntity.newPacketLocation.targetLocation.maxX + reachEntity.newPacketLocation.targetLocation.minX) / 2);

            GrimAC.staticGetLogger().info(ChatColor.AQUA + "Checking entity " + reachEntity);

            if (ReachUtils.isVecInside(targetBox, eyePos)) {
                Bukkit.broadcastMessage(ChatColor.GREEN + "Intercepted! (Player inside other entity!)");
            } else if (intercept == null) {
                Bukkit.broadcastMessage(ChatColor.RED + "Player missed hitbox!");
            } else {
                double reach = eyePos.distance(intercept);

                if (reach < 3 && !player.packetStateData.didLastMovementIncludePosition) {
                    Bukkit.broadcastMessage(ChatColor.GREEN + "Intersected!  Reach was " + reach + " (0.03 = true)");
                } else if (reach < 3) {
                    Bukkit.broadcastMessage(ChatColor.GREEN + "Intersected!  Reach was " + reach);
                } else {
                    Bukkit.broadcastMessage(ChatColor.RED + "Intersected!  Reach was " + reach);
                }
            }

            attackQueue = playerAttackQueue.poll();
        }

        for (PlayerReachEntity entity : entityMap.values()) {
            entity.onMovement();
        }
    }

    public void handleTransaction(int transactionID) {
        ReachMovementData nextTrans = transactionReachQueue.peek();

        //GrimAC.staticGetLogger().info("Got packet " + transactionID);

        if (nextTrans != null) {
            if (transactionID == nextTrans.transactionID) {
                // Create a bounding box taking the minimums and maximums of the previous packet target and the new target,
                // meaning that the bounding box will become larger than the player’s actual bounding box.
                PlayerReachEntity entity = entityMap.get(nextTrans.entityID);
                //GrimAC.staticGetLogger().info("Handling first bread with pos " + entity.relativeMoveLocation);

                entity.onFirstTransaction(nextTrans.newPos.getX(), nextTrans.newPos.getY(), nextTrans.newPos.getZ());

            } else if (transactionID - 1 == nextTrans.transactionID) {
                PlayerReachEntity entity = entityMap.get(nextTrans.entityID);

                //GrimAC.staticGetLogger().info("Handling second bread with pos " + entity.relativeMoveLocation);

                entity.onSecondTransaction();
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

            transactionReachQueue.add(new ReachMovementData(lastTrans, entityId, reachEntity.serverPos));
        }
    }
}