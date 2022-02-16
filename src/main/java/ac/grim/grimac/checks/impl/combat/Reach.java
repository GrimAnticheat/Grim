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

import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.collisions.datatypes.SimpleCollisionBox;
import ac.grim.grimac.utils.data.packetentity.PacketEntity;
import ac.grim.grimac.utils.math.VectorUtils;
import ac.grim.grimac.utils.nmsutil.ReachUtils;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;
import org.bukkit.GameMode;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

// You may not copy the check unless you are licensed under GPL
public class Reach extends PacketCheck {
    // Concurrent to support weird entity trackers
    private final ConcurrentLinkedQueue<Integer> playerAttackQueue = new ConcurrentLinkedQueue<>();

    private boolean cancelImpossibleHits = true;
    private double threshold = 0.0005;

    public Reach(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(final PacketReceiveEvent event) {
        if (player.disableGrim) return;
        if (event.getPacketType() == PacketType.Play.Client.INTERACT_ENTITY) {
            WrapperPlayClientInteractEntity action = new WrapperPlayClientInteractEntity(event);

            if (player.gamemode == GameMode.CREATIVE) return;
            if (player.vehicle != null) return;

            checkReach(action.getEntityId());

            if (cancelImpossibleHits && isKnownInvalid(action.getEntityId())) {
                event.setCancelled(true);
            }
        }

        if (WrapperPlayClientPlayerFlying.isFlying(event.getPacketType())) {
            // Teleports don't interpolate, duplicate 1.17 packets don't interpolate
            if (player.packetStateData.lastPacketWasTeleport || player.packetStateData.lastPacketWasOnePointSeventeenDuplicate)
                return;
            tickFlying();
        }
    }

    public void checkReach(int entityID) {
        if (player.compensatedEntities.entityMap.containsKey(entityID))
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
        PacketEntity reachEntity = player.compensatedEntities.entityMap.get(entityID);
        boolean zeroThree = player.packetStateData.didLastMovementIncludePosition || player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_9);

        if (reachEntity != null) {
            double lowest = 6;
            for (double eyes : player.getPossibleEyeHeights()) {
                SimpleCollisionBox targetBox = reachEntity.getPossibleCollisionBoxes();
                Vector from = new Vector(player.x, player.y + eyes, player.z);
                Vector closestPoint = VectorUtils.cutBoxToVector(from, targetBox);
                lowest = Math.min(lowest, closestPoint.distance(from));
            }

            return lowest > 3 + (zeroThree ? 0.03 : 0);
        }

        return false;
    }

    private void tickFlying() {
        double maxReach = 3;

        Integer attackQueue = playerAttackQueue.poll();
        while (attackQueue != null) {
            PacketEntity reachEntity = player.compensatedEntities.entityMap.get(attackQueue);

            if (reachEntity == null) return;

            SimpleCollisionBox targetBox = reachEntity.getPossibleCollisionBoxes();

            // 1.9 -> 1.8 precision loss in packets
            // (ViaVersion is doing some stuff that makes this code difficult)
            //
            // This will likely be fixed with PacketEvents 2.0, where our listener is before ViaVersion
            // Don't attempt to fix it with this version of PacketEvents, it's not worth our time when 2.0 will fix it.
            if (PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_9) && player.getClientVersion().isOlderThan(ClientVersion.V_1_9)) {
                targetBox.expand(0.03125);
            }

            // 1.7 and 1.8 players get a bit of extra hitbox (this is why you should use 1.8 on cross version servers)
            // Yes, this is vanilla and not uncertainty.  All reach checks have this or they are wrong.
            if (player.getClientVersion().isOlderThan(ClientVersion.V_1_9)) {
                targetBox.expand(0.1f);
            }

            targetBox.expand(threshold);

            // This is better than adding to the reach, as 0.03 can cause a player to miss their target
            // Adds some more than 0.03 uncertainty in some cases, but a good trade off for simplicity
            //
            // Just give the uncertainty on 1.9+ clients as we have no way of knowing whether they had 0.03 movement
            if (!player.packetStateData.didLastLastMovementIncludePosition || player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_9))
                targetBox.expand(0.03);

            Vector3d from = new Vector3d(player.lastX, player.lastY, player.lastZ);

            double minDistance = Double.MAX_VALUE;

            // https://bugs.mojang.com/browse/MC-67665
            List<Vector> possibleLookDirs = new ArrayList<>(Arrays.asList(
                    ReachUtils.getLook(player, player.lastXRot, player.yRot),
                    ReachUtils.getLook(player, player.xRot, player.yRot)
            ));

            // 1.9+ players could be a tick behind because we don't get skipped ticks
            if (player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_9)) {
                possibleLookDirs.add(ReachUtils.getLook(player, player.lastXRot, player.lastYRot));
            }

            // 1.7 players do not have any of these issues! They are always on the latest look vector
            if (player.getClientVersion().isOlderThan(ClientVersion.V_1_8)) {
                possibleLookDirs = Collections.singletonList(ReachUtils.getLook(player, player.xRot, player.yRot));
            }

            for (Vector lookVec : possibleLookDirs) {
                for (double eye : player.getPossibleEyeHeights()) {
                    Vector eyePos = new Vector(from.getX(), from.getY() + eye, from.getZ());
                    Vector endReachPos = eyePos.clone().add(new Vector(lookVec.getX() * 6, lookVec.getY() * 6, lookVec.getZ() * 6));

                    Vector intercept = ReachUtils.calculateIntercept(targetBox, eyePos, endReachPos).getFirst();

                    if (ReachUtils.isVecInside(targetBox, eyePos)) {
                        minDistance = 0;
                        break;
                    }

                    if (intercept != null) {
                        minDistance = Math.min(eyePos.distance(intercept), minDistance);
                    }
                }
            }

            if (reachEntity.type != EntityTypes.BOAT) { // boats are too glitchy to consider
                if (minDistance == Double.MAX_VALUE) {
                    increaseViolations();
                    alert("Missed hitbox", "Reach", formatViolations());
                } else if (minDistance > maxReach) {
                    increaseViolations();
                    alert(String.format("%.5f", minDistance) + " blocks", "Reach", formatViolations());
                } else {
                    reward();
                }
            }

            attackQueue = playerAttackQueue.poll();
        }
    }

    @Override
    public void reload() {
        super.reload();
        this.cancelImpossibleHits = getConfig().getBoolean("Reach.block-impossible-hits", true);
        this.threshold = getConfig().getDouble("Reach.threshold", 0.0005);
    }
}