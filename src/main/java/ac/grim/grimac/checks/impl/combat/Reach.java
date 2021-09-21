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
package ac.grim.grimac.checks.impl.combat;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.collisions.datatypes.SimpleCollisionBox;
import ac.grim.grimac.utils.data.packetentity.PlayerReachEntity;
import ac.grim.grimac.utils.math.VectorUtils;
import ac.grim.grimac.utils.nmsImplementations.ReachUtils;
import io.github.retrooper.packetevents.event.impl.PacketPlayReceiveEvent;
import io.github.retrooper.packetevents.event.impl.PacketPlaySendEvent;
import io.github.retrooper.packetevents.packettype.PacketType;
import io.github.retrooper.packetevents.packetwrappers.play.in.useentity.WrappedPacketInUseEntity;
import io.github.retrooper.packetevents.packetwrappers.play.out.entity.WrappedPacketOutEntity;
import io.github.retrooper.packetevents.packetwrappers.play.out.entityteleport.WrappedPacketOutEntityTeleport;
import io.github.retrooper.packetevents.packetwrappers.play.out.namedentityspawn.WrappedPacketOutNamedEntitySpawn;
import io.github.retrooper.packetevents.utils.player.ClientVersion;
import io.github.retrooper.packetevents.utils.server.ServerVersion;
import io.github.retrooper.packetevents.utils.vector.Vector3d;
import org.bukkit.GameMode;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.util.Vector;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

// You may not copy the check unless you are licensed under GPL
public class Reach extends PacketCheck {

    // Concurrent to support weird entity trackers
    public final ConcurrentHashMap<Integer, PlayerReachEntity> entityMap = new ConcurrentHashMap<>();
    private final ConcurrentLinkedQueue<Integer> playerAttackQueue = new ConcurrentLinkedQueue<>();
    private final GrimPlayer player;

    private boolean hasSentPreWavePacket = false; // Not required to be atomic - sync'd to one thread

    private boolean cancelImpossibleHits = true;
    private double threshold = 0.0005;

    public Reach(GrimPlayer player) {
        super(player);
        this.player = player;
    }

    @Override
    public void onPacketReceive(final PacketPlayReceiveEvent event) {
        if (event.getPacketId() == PacketType.Play.Client.USE_ENTITY) {
            WrappedPacketInUseEntity action = new WrappedPacketInUseEntity(event.getNMSPacket());
            GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getPlayer());

            if (player == null) return;
            if (player.packetStateData.gameMode == GameMode.CREATIVE) return;
            if (player.vehicle != null) return;

            if (action.getAction() == WrappedPacketInUseEntity.EntityUseAction.ATTACK) {
                checkReach(action.getEntityId());

                if (cancelImpossibleHits && isKnownInvalid(action.getEntityId())) {
                    event.setCancelled(true);
                }
            }
        }

        if (PacketType.Play.Client.Util.isInstanceOfFlying(event.getPacketId())) {
            // Teleports don't interpolate, duplicate 1.17 packets don't interpolate
            if (player.packetStateData.lastPacketWasTeleport || player.packetStateData.lastPacketWasOnePointSeventeenDuplicate)
                return;
            tickFlying();
        }
    }

    private void tickFlying() {
        double maxReach = 3;

        Integer attackQueue = playerAttackQueue.poll();
        while (attackQueue != null) {
            PlayerReachEntity reachEntity = entityMap.get(attackQueue);
            SimpleCollisionBox targetBox = reachEntity.getPossibleCollisionBoxes();

            // 1.9 -> 1.8 precision loss in packets
            // (ViaVersion is doing some stuff that makes this code difficult)
            //
            // This will likely be fixed with PacketEvents 2.0, where our listener is before ViaVersion
            // Don't attempt to fix it with this version of PacketEvents, it's not worth our time when 2.0 will fix it.
            if (ServerVersion.getVersion().isNewerThanOrEquals(ServerVersion.v_1_9) && player.getClientVersion().isOlderThan(ClientVersion.v_1_9)) {
                targetBox.expand(0.03125);
            }

            // 1.7 and 1.8 players get a bit of extra hitbox (this is why you should use 1.8 on cross version servers)
            // Yes, this is vanilla and not uncertainty.  All reach checks have this or they are wrong.
            if (player.getClientVersion().isOlderThan(ClientVersion.v_1_9)) {
                targetBox.expand(0.1);
            }

            targetBox.expand(threshold);

            // This is better than adding to the reach, as 0.03 can cause a player to miss their target
            // Adds some more than 0.03 uncertainty in some cases, but a good trade off for simplicity
            //
            // Just give the uncertainty on 1.9+ clients as we have no way of knowing whether they had 0.03 movement
            if (!player.packetStateData.didLastLastMovementIncludePosition || player.getClientVersion().isNewerThanOrEquals(ClientVersion.v_1_9))
                targetBox.expand(0.03);

            Vector3d from = player.packetStateData.lastPacketPosition;
            Vector attackerDirection = ReachUtils.getLook(player, player.packetStateData.packetPlayerXRot, player.packetStateData.packetPlayerYRot);

            double minDistance = Double.MAX_VALUE;

            for (double eye : player.getPossibleEyeHeights()) {
                Vector eyePos = new Vector(from.getX(), from.getY() + eye, from.getZ());
                Vector endReachPos = eyePos.clone().add(new Vector(attackerDirection.getX() * 6, attackerDirection.getY() * 6, attackerDirection.getZ() * 6));

                Vector intercept = ReachUtils.calculateIntercept(targetBox, eyePos, endReachPos);
                Vector vanillaIntercept = null;

                if (ReachUtils.isVecInside(targetBox, eyePos)) {
                    minDistance = 0;
                    break;
                }

                // This is how vanilla handles look vectors on 1.8 - it's a tick behind.
                // 1.9+ you have no guarantees of which look vector it is due to 0.03
                //
                // The only safe version is 1.7
                if (player.getClientVersion().isNewerThanOrEquals(ClientVersion.v_1_8)) {
                    Vector vanillaDir = ReachUtils.getLook(player, player.packetStateData.lastPacketPlayerXRot, player.packetStateData.lastPacketPlayerYRot);
                    Vector vanillaEndPos = eyePos.clone().add(new Vector(vanillaDir.getX() * 6, vanillaDir.getY() * 6, vanillaDir.getZ() * 6));

                    vanillaIntercept = ReachUtils.calculateIntercept(targetBox, eyePos, vanillaEndPos);
                }

                if (intercept != null) {
                    minDistance = Math.min(eyePos.distance(intercept), minDistance);
                }
                if (vanillaIntercept != null) {
                    minDistance = Math.min(eyePos.distance(vanillaIntercept), minDistance);
                }
            }

            if (minDistance == Double.MAX_VALUE) {
                increaseViolations();
                alert("Missed hitbox", "Reach", formatViolations());
            } else if (minDistance > maxReach) {
                increaseViolations();
                alert(String.format("%.5f", minDistance) + " blocks", "Reach", formatViolations());
            }

            attackQueue = playerAttackQueue.poll();
        }

        for (PlayerReachEntity entity : entityMap.values()) {
            entity.onMovement(player.getClientVersion().isNewerThan(ClientVersion.v_1_8));
        }
    }

    public void checkReach(int entityID) {
        if (entityMap.containsKey(entityID))
            playerAttackQueue.add(entityID);
    }

    // This method finds the most optimal point at which the user should be aiming at
    // and then measures the distance between the player's eyes and this target point
    //
    // It will not cancel every invalid attack but should cancel 3.05+ or so in real-time
    // Let the post look check measure the distance, as it will always return equal or higher
    // than this method.  If this method flags, the other method WILL flag.
    //
    // Meaning that the other check should be the only one that flags.
    private boolean isKnownInvalid(int entityID) {
        PlayerReachEntity reachEntity = entityMap.get(entityID);
        boolean zeroThree = player.packetStateData.didLastMovementIncludePosition || player.getClientVersion().isNewerThanOrEquals(ClientVersion.v_1_9);

        if (reachEntity != null) {
            double lowest = 6;
            for (double eyes : player.getPossibleEyeHeights()) {
                SimpleCollisionBox targetBox = reachEntity.getPossibleCollisionBoxes();
                Vector from = VectorUtils.fromVec3d(player.packetStateData.packetPosition).add(new Vector(0, eyes, 0));
                Vector closestPoint = VectorUtils.cutBoxToVector(from, targetBox);
                lowest = Math.min(lowest, closestPoint.distance(from));
            }

            return lowest > 3 + (zeroThree ? 0.03 : 0);
        }

        return false;
    }

    @Override
    public void reload() {
        super.reload();
        this.cancelImpossibleHits = getConfig().getBoolean("Reach.block-impossible-hits", true);
        this.threshold = getConfig().getDouble("Reach.threshold", 0.0005);
    }

    @Override
    public void onPacketSend(final PacketPlaySendEvent event) {
        byte packetID = event.getPacketId();

        if (packetID == PacketType.Play.Server.NAMED_ENTITY_SPAWN) {
            WrappedPacketOutNamedEntitySpawn spawn = new WrappedPacketOutNamedEntitySpawn(event.getNMSPacket());
            Entity entity = spawn.getEntity();

            if (entity != null && entity.getType() == EntityType.PLAYER) {
                handleSpawnPlayer(spawn.getEntityId(), spawn.getPosition());
            }
        }

        if (packetID == PacketType.Play.Server.REL_ENTITY_MOVE || packetID == PacketType.Play.Server.REL_ENTITY_MOVE_LOOK || packetID == PacketType.Play.Server.ENTITY_LOOK) {
            WrappedPacketOutEntity.WrappedPacketOutRelEntityMove move = new WrappedPacketOutEntity.WrappedPacketOutRelEntityMove(event.getNMSPacket());

            if (entityMap.containsKey(move.getEntityId())) {
                handleMoveEntity(move.getEntityId(), move.getDeltaX(), move.getDeltaY(), move.getDeltaZ(), true);
            }
        }

        if (packetID == PacketType.Play.Server.ENTITY_TELEPORT) {
            WrappedPacketOutEntityTeleport teleport = new WrappedPacketOutEntityTeleport(event.getNMSPacket());

            if (entityMap.containsKey(teleport.getEntityId())) {
                Vector3d pos = teleport.getPosition();
                handleMoveEntity(teleport.getEntityId(), pos.getX(), pos.getY(), pos.getZ(), false);
            }
        }
    }

    private void handleSpawnPlayer(int playerID, Vector3d spawnPosition) {
        entityMap.put(playerID, new PlayerReachEntity(spawnPosition.getX(), spawnPosition.getY(), spawnPosition.getZ()));
    }

    private void handleMoveEntity(int entityId, double deltaX, double deltaY, double deltaZ, boolean isRelative) {
        PlayerReachEntity reachEntity = entityMap.get(entityId);

        if (reachEntity != null) {
            // Only send one transaction before each wave, without flushing
            if (!hasSentPreWavePacket) player.sendTransaction();
            hasSentPreWavePacket = true; // Also functions to mark we need a post wave transaction

            // Update the tracked server's entity position
            if (isRelative)
                reachEntity.serverPos = reachEntity.serverPos.add(new Vector3d(deltaX, deltaY, deltaZ));
            else
                reachEntity.serverPos = new Vector3d(deltaX, deltaY, deltaZ);

            int lastTrans = player.lastTransactionSent.get();
            Vector3d newPos = reachEntity.serverPos;

            player.latencyUtils.addRealTimeTask(lastTrans, () -> reachEntity.onFirstTransaction(newPos.getX(), newPos.getY(), newPos.getZ()));
            player.latencyUtils.addRealTimeTask(lastTrans + 1, reachEntity::onSecondTransaction);
        }
    }

    public void onEndOfTickEvent() {
        // Only send a transaction at the end of the tick if we are tracking players
        player.sendAndFlushTransaction(); // Vanilla already flushed packet at this point
        hasSentPreWavePacket = false;
    }

    public void removeEntity(int entityID) {
        entityMap.remove(entityID);
    }
}