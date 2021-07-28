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

import ac.grim.grimac.utils.collisions.datatypes.SimpleCollisionBox;
import ac.grim.grimac.utils.data.ReachInterpolationData;
import ac.grim.grimac.utils.nmsImplementations.GetBoundingBox;
import io.github.retrooper.packetevents.utils.vector.Vector3d;

// You may not copy this check unless your anticheat is licensed under GPL
public class PlayerReachEntity {
    public Vector3d serverPos;
    public ReachInterpolationData oldPacketLocation;
    public ReachInterpolationData newPacketLocation;

    public PlayerReachEntity(double x, double y, double z) {
        serverPos = new Vector3d(x, y, z);
        this.newPacketLocation = new ReachInterpolationData(GetBoundingBox.getBoundingBoxFromPosAndSize(x, y, z, 0.6, 1.8),
                serverPos.getX(), serverPos.getY(), serverPos.getZ());
    }

    // Set the old packet location to the new one
    // Set the new packet location to the updated packet location
    public void onFirstTransaction(double x, double y, double z) {
        //GrimAC.staticGetLogger().info("Received first transaction for " + x + " " + y + " " + z);
        this.oldPacketLocation = newPacketLocation;
        this.newPacketLocation = new ReachInterpolationData(oldPacketLocation.getPossibleLocationCombined(), x, y, z);
    }

    // Remove the possibility of the old packet location
    public void onSecondTransaction() {
        //GrimAC.staticGetLogger().info("Received second transaction for the previous movement");
        this.oldPacketLocation = null;
    }

    // If the old and new packet location are split, we need to combine bounding boxes
    // TODO: Let 1.9 uncertainty fuck this all up - Thanks Mojang!
    public void onMovement() {
        //GrimAC.staticGetLogger().info("Ticking new packet start " + newPacketLocation.interpolationStepsLowBound + " and " + newPacketLocation.interpolationStepsHighBound);
        newPacketLocation.tickMovement(oldPacketLocation == null, false);
        //GrimAC.staticGetLogger().info("Ticking new packet end " + newPacketLocation.interpolationStepsLowBound + " and " + newPacketLocation.interpolationStepsHighBound);

        // Handle uncertainty of second transaction spanning over multiple ticks
        if (oldPacketLocation != null) {
            //GrimAC.staticGetLogger().info("Ticking new packet start " + oldPacketLocation.interpolationStepsLowBound + " and " + oldPacketLocation.interpolationStepsHighBound);
            oldPacketLocation.tickMovement(true, false);
            //GrimAC.staticGetLogger().info("Ticking new packet end " + oldPacketLocation.interpolationStepsLowBound + " and " + oldPacketLocation.interpolationStepsHighBound);
            newPacketLocation.updatePossibleStartingLocation(oldPacketLocation.getPossibleLocationCombined());
        }
    }

    public SimpleCollisionBox getPossibleCollisionBoxes() {
        if (oldPacketLocation == null) {
            //GrimAC.staticGetLogger().info(ChatColor.GOLD + "Certain, using collision box " + newPacketLocation.getPossibleLocationCombined());
            return newPacketLocation.getPossibleLocationCombined();
        }

        //GrimAC.staticGetLogger().info(ChatColor.GOLD + "Uncertain!  Combining collision boxes " + oldPacketLocation.getPossibleLocationCombined() + " and " +  newPacketLocation.getPossibleLocationCombined() + " into " + ReachInterpolationData.combineCollisionBox(oldPacketLocation.getPossibleLocationCombined(), newPacketLocation.getPossibleLocationCombined()));
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
