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
import ac.grim.grimac.utils.nmsutil.GetBoundingBox;
import com.github.retrooper.packetevents.protocol.entity.type.EntityType;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.potion.PotionType;
import com.github.retrooper.packetevents.util.Vector3d;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

// You may not copy this check unless your anticheat is licensed under GPL
public class PacketEntity {
    public Vector3d desyncClientPos;
    public EntityType type;

    public PacketEntity riding;
    public List<PacketEntity> passengers = new ArrayList<>(0);
    public boolean isDead = false;
    public boolean isBaby = false;
    public boolean hasGravity = true;
    private ReachInterpolationData oldPacketLocation;
    private ReachInterpolationData newPacketLocation;

    public HashMap<PotionType, Integer> potionsMap = null;

    public PacketEntity(EntityType type) {
        this.type = type;
    }

    public PacketEntity(GrimPlayer player, EntityType type, double x, double y, double z) {
        this.desyncClientPos = new Vector3d(x, y, z);
        if (player.getClientVersion().isOlderThan(ClientVersion.V_1_9)) { // Thanks ViaVersion
            desyncClientPos = new Vector3d(((int) (desyncClientPos.getX() * 32)) / 32d, ((int) (desyncClientPos.getY() * 32)) / 32d, ((int) (desyncClientPos.getZ() * 32)) / 32d);
        }
        this.type = type;
        this.newPacketLocation = new ReachInterpolationData(player, GetBoundingBox.getPacketEntityBoundingBox(player, x, y, z, this),
                desyncClientPos.getX(), desyncClientPos.getY(), desyncClientPos.getZ(), !player.compensatedEntities.getSelf().inVehicle() && player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_9), this);
    }

    public boolean isLivingEntity() {
        return EntityTypes.isTypeInstanceOf(type, EntityTypes.LIVINGENTITY);
    }

    public boolean isMinecart() {
        return EntityTypes.isTypeInstanceOf(type, EntityTypes.MINECART_ABSTRACT);
    }

    public boolean isHorse() {
        return EntityTypes.isTypeInstanceOf(type, EntityTypes.ABSTRACT_HORSE);
    }

    public boolean isAgeable() {
        return EntityTypes.isTypeInstanceOf(type, EntityTypes.ABSTRACT_AGEABLE);
    }

    public boolean isAnimal() {
        return EntityTypes.isTypeInstanceOf(type, EntityTypes.ABSTRACT_ANIMAL);
    }

    public boolean isSize() {
        return type == EntityTypes.PHANTOM || type == EntityTypes.SLIME || type == EntityTypes.MAGMA_CUBE;
    }

    // Set the old packet location to the new one
    // Set the new packet location to the updated packet location
    public void onFirstTransaction(boolean relative, boolean hasPos, double relX, double relY, double relZ, GrimPlayer player) {
        if (hasPos) {
            if (relative) {
                // This only matters for 1.9+ clients, but it won't hurt 1.8 clients either... align for imprecision
                desyncClientPos = new Vector3d(Math.floor(desyncClientPos.getX() * 4096) / 4096, Math.floor(desyncClientPos.getY() * 4096) / 4096, Math.floor(desyncClientPos.getZ() * 4096) / 4096);
                desyncClientPos = desyncClientPos.add(new Vector3d(relX, relY, relZ));
            } else {
                desyncClientPos = new Vector3d(relX, relY, relZ);
                // ViaVersion desync's here for teleports
                // It simply teleports the entity with its position divided by 32... ignoring the offset this causes.
                // Thanks a lot ViaVersion!  Please don't fix this, or it will be a pain to support.
                if (player.getClientVersion().isOlderThan(ClientVersion.V_1_9)) {
                    desyncClientPos = new Vector3d(((int) (desyncClientPos.getX() * 32)) / 32d, ((int) (desyncClientPos.getY() * 32)) / 32d, ((int) (desyncClientPos.getZ() * 32)) / 32d);
                }
            }
        }

        this.oldPacketLocation = newPacketLocation;
        this.newPacketLocation = new ReachInterpolationData(player, oldPacketLocation.getPossibleLocationCombined(), desyncClientPos.getX(), desyncClientPos.getY(), desyncClientPos.getZ(), !player.compensatedEntities.getSelf().inVehicle() && player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_9), this);
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
        this.desyncClientPos = new Vector3d((box.maxX - box.minX) / 2 + box.minX, box.minY, (box.maxZ - box.minZ) / 2 + box.minZ);
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
