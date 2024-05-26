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
package ac.grim.grimac.utils.data.packetentity;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.collisions.datatypes.SimpleCollisionBox;
import ac.grim.grimac.utils.data.ReachInterpolationData;
import ac.grim.grimac.utils.data.TrackedPosition;
import ac.grim.grimac.utils.data.TrackedPosition;
import ac.grim.grimac.utils.nmsutil.GetBoundingBox;
import com.github.retrooper.packetevents.protocol.entity.type.EntityType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.potion.PotionType;
import com.github.retrooper.packetevents.util.Vector3d;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

// You may not copy this check unless your anticheat is licensed under GPL
public class PacketEntity extends TypedPacketEntity {
    
    public final TrackedPosition trackedServerPosition;

    public PacketEntity riding;
    public List<PacketEntity> passengers = new ArrayList<>(0);
    public boolean isDead = false;
    public boolean isBaby = false;
    public boolean hasGravity = true;
    private ReachInterpolationData oldPacketLocation;
    private ReachInterpolationData newPacketLocation;

    public HashMap<PotionType, Integer> potionsMap = null;
    public float scale = 1f; // 1.20.5+
    public float stepHeight = 0.6f; // 1.20.5+
    public double gravityAttribute = 0.08; // 1.20.5+

    public PacketEntity(EntityType type) {
        super(type);
        this.trackedServerPosition = new TrackedPosition();
    }

    public PacketEntity(GrimPlayer player, EntityType type, double x, double y, double z) {
        super(type);
        this.trackedServerPosition = new TrackedPosition();
        this.trackedServerPosition.setPos(new Vector3d(x, y, z));
        if (player.getClientVersion().isOlderThan(ClientVersion.V_1_9)) { // Thanks ViaVersion
            trackedServerPosition.setPos(new Vector3d(((int) (x * 32)) / 32d, ((int) (y * 32)) / 32d, ((int) (z * 32)) / 32d));
        }
        this.newPacketLocation = new ReachInterpolationData(player, GetBoundingBox.getPacketEntityBoundingBox(player, x, y, z, this), trackedServerPosition, this);
    }

    // Set the old packet location to the new one
    // Set the new packet location to the updated packet location
    public void onFirstTransaction(boolean relative, boolean hasPos, double relX, double relY, double relZ, GrimPlayer player) {
        if (hasPos) {
            if (relative) {
                // This only matters for 1.9+ clients, but it won't hurt 1.8 clients either... align for imprecision
                final double scale = trackedServerPosition.getScale();
                Vector3d vec3d;
                if (player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_16)) {
                    vec3d = trackedServerPosition.withDelta(TrackedPosition.pack(relX, scale), TrackedPosition.pack(relY, scale), TrackedPosition.pack(relZ, scale));
                } else {
                    vec3d = trackedServerPosition.withDeltaLegacy(TrackedPosition.packLegacy(relX, scale), TrackedPosition.packLegacy(relY, scale), TrackedPosition.packLegacy(relZ, scale));
                }
                trackedServerPosition.setPos(vec3d);
            } else {
                trackedServerPosition.setPos(new Vector3d(relX, relY, relZ));
                // ViaVersion desync's here for teleports
                // It simply teleports the entity with its position divided by 32... ignoring the offset this causes.
                // Thanks a lot ViaVersion!  Please don't fix this, or it will be a pain to support.
                if (player.getClientVersion().isOlderThan(ClientVersion.V_1_9)) {
                    trackedServerPosition.setPos(new Vector3d(((int) (relX * 32)) / 32d, ((int) (relY * 32)) / 32d, ((int) (relZ * 32)) / 32d));
                }
            }
        }

        this.oldPacketLocation = newPacketLocation;
        this.newPacketLocation = new ReachInterpolationData(player, oldPacketLocation.getPossibleLocationCombined(), trackedServerPosition, this);
    }

    // Remove the possibility of the old packet location
    public void onSecondTransaction() {
        this.oldPacketLocation = null;
    }

    // If the old and new packet location are split, we need to combine bounding boxes
    public void onMovement(boolean tickingReliably) {
        newPacketLocation.tickMovement(oldPacketLocation == null, tickingReliably);

        // Handle uncertainty of second transaction spanning over multiple ticks
        if (oldPacketLocation != null) {
            oldPacketLocation.tickMovement(true, tickingReliably);
            newPacketLocation.updatePossibleStartingLocation(oldPacketLocation.getPossibleLocationCombined());
        }
    }

    public boolean hasPassenger(PacketEntity entity) {
        return passengers.contains(entity);
    }

    public void mount(PacketEntity vehicle) {
        if (riding != null) eject();
        vehicle.passengers.add(this);
        riding = vehicle;
    }

    public void eject() {
        if (riding != null) {
            riding.passengers.remove(this);
        }
        this.riding = null;
    }

    // This is for handling riding and entities attached to one another.
    public void setPositionRaw(SimpleCollisionBox box) {
        // I'm disappointed in you mojang.  Please don't set the packet position as it desyncs it...
        // But let's follow this flawed client-sided logic!
        this.trackedServerPosition.setPos(new Vector3d((box.maxX - box.minX) / 2 + box.minX, box.minY, (box.maxZ - box.minZ) / 2 + box.minZ));
        // This disables interpolation
        this.newPacketLocation = new ReachInterpolationData(box);
    }

    public SimpleCollisionBox getPossibleCollisionBoxes() {
        if (oldPacketLocation == null) {
            return newPacketLocation.getPossibleLocationCombined();
        }

        return ReachInterpolationData.combineCollisionBox(oldPacketLocation.getPossibleLocationCombined(), newPacketLocation.getPossibleLocationCombined());
    }

    public PacketEntity getRiding() {
        return riding;
    }

    public void addPotionEffect(PotionType effect, int amplifier) {
        if (potionsMap == null) {
            potionsMap = new HashMap<>();
        }
        potionsMap.put(effect, amplifier);
    }

    public void removePotionEffect(PotionType effect) {
        if (potionsMap == null) return;
        potionsMap.remove(effect);
    }
}
