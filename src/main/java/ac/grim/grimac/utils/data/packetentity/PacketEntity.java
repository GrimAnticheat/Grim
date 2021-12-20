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
import com.github.retrooper.packetevents.util.Vector3d;

// You may not copy this check unless your anticheat is licensed under GPL
public class PacketEntity {
    public Vector3d serverPos;
    public int lastTransactionHung;
    public EntityType type;
    public org.bukkit.entity.EntityType bukkitEntityType;

    public PacketEntity riding;
    public int[] passengers = new int[0];
    public boolean isDead = false;
    public boolean isBaby = false;
    public boolean hasGravity = true;
    private ReachInterpolationData oldPacketLocation;
    private ReachInterpolationData newPacketLocation;

    public PacketEntity(GrimPlayer player, EntityType type, double x, double y, double z) {
        this.serverPos = new Vector3d(x, y, z);
        this.type = type;
        this.newPacketLocation = new ReachInterpolationData(GetBoundingBox.getPacketEntityBoundingBox(x, y, z, this),
                serverPos.getX(), serverPos.getY(), serverPos.getZ(), player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_9));
    }

    public boolean isLivingEntity() {
        return EntityTypes.typeHasParent(type, EntityTypes.LIVINGENTITY);
    }

    public boolean isMinecart() {
        return EntityTypes.typeHasParent(type, EntityTypes.MINECART_ABSTRACT);
    }

    public boolean isHorse() {
        return EntityTypes.typeHasParent(type, EntityTypes.ABSTRACT_HORSE);
    }

    public boolean isAgeable() {
        return EntityTypes.typeHasParent(type, EntityTypes.ABSTRACT_AGEABLE);
    }

    public boolean isAnimal() {
        return EntityTypes.typeHasParent(type, EntityTypes.ABSTRACT_ANIMAL);
    }

    public boolean isSize() {
        return type == EntityTypes.PHANTOM || type == EntityTypes.SLIME || type == EntityTypes.MAGMA_CUBE;
    }

    // Set the old packet location to the new one
    // Set the new packet location to the updated packet location
    public void onFirstTransaction(double x, double y, double z, GrimPlayer player) {
        this.oldPacketLocation = newPacketLocation;
        this.newPacketLocation = new ReachInterpolationData(oldPacketLocation.getPossibleLocationCombined(), x, y, z, player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_9));
    }

    // Remove the possibility of the old packet location
    public void onSecondTransaction() {
        this.oldPacketLocation = null;
    }

    // If the old and new packet location are split, we need to combine bounding boxes
    public void onMovement() {
        newPacketLocation.tickMovement(oldPacketLocation == null);

        // Handle uncertainty of second transaction spanning over multiple ticks
        if (oldPacketLocation != null) {
            oldPacketLocation.tickMovement(true);
            newPacketLocation.updatePossibleStartingLocation(oldPacketLocation.getPossibleLocationCombined());
        }
    }

    public boolean hasPassenger(int entityID) {
        for (int passenger : passengers) {
            if (passenger == entityID) return true;
        }
        return false;
    }

    // This is for handling riding and entities attached to one another.
    public void setPositionRaw(SimpleCollisionBox box) {
        this.newPacketLocation = new ReachInterpolationData(box);
    }

    public SimpleCollisionBox getPossibleCollisionBoxes() {
        if (oldPacketLocation == null) {
            return newPacketLocation.getPossibleLocationCombined();
        }

        return ReachInterpolationData.combineCollisionBox(oldPacketLocation.getPossibleLocationCombined(), newPacketLocation.getPossibleLocationCombined());
    }

    @Override
    public String toString() {
        return "PlayerReachEntity{" +
                "serverPos=" + serverPos +
                ", oldPacketLocation=" + oldPacketLocation +
                ", newPacketLocation=" + newPacketLocation +
                '}';
    }
}
