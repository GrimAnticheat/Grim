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
package ac.grim.grimac.utils.data;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.collisions.datatypes.CollisionBox;
import ac.grim.grimac.utils.collisions.datatypes.NoCollisionBox;
import ac.grim.grimac.utils.collisions.datatypes.SimpleCollisionBox;
import ac.grim.grimac.utils.data.packetentity.PacketEntity;
import ac.grim.grimac.utils.nmsutil.GetBoundingBox;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.util.Vector3d;

// You may not copy the check unless you are licensed under GPL
public class ReachInterpolationData {
    private final SimpleCollisionBox targetLocation;
    private SimpleCollisionBox startingLocation;
    private int interpolationStepsLowBound = 0;
    private int interpolationStepsHighBound = 0;
    private int interpolationSteps = 1;
    private boolean expandNonRelative = false;

    private final GrimPlayer player;
    private final PacketEntity entity;

    public ReachInterpolationData(GrimPlayer player, SimpleCollisionBox startingLocation, TrackedPosition position, PacketEntity entity) {
        final boolean isPointNine = !player.inVehicle() && player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_9);

        this.startingLocation = startingLocation;
        final Vector3d pos = position.getPos();
        this.targetLocation = new SimpleCollisionBox(pos.x, pos.y, pos.z, pos.x, pos.y, pos.z, false);
        this.player = player;
        this.entity = entity;

        // 1.9 -> 1.8 precision loss in packets
        // (ViaVersion is doing some stuff that makes this code difficult)
        if (!isPointNine && PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_9)) {
            targetLocation.expand(0.03125);
        }

        if (entity.isBoat()) {
            interpolationSteps = 10;
        } else if (entity.isMinecart()) {
            interpolationSteps = 5;
        } else if (entity.getType() == EntityTypes.SHULKER) {
            interpolationSteps = 1;
        } else if (entity.isLivingEntity()) {
            interpolationSteps = 3;
        } else {
            interpolationSteps = 1;
        }

        if (isPointNine) interpolationStepsHighBound = getInterpolationSteps();
    }

    // While riding entities, there is no interpolation.
    public ReachInterpolationData(GrimPlayer player, SimpleCollisionBox finishedLoc, PacketEntity entity) {
        this.startingLocation = finishedLoc;
        this.targetLocation = finishedLoc;
        this.entity = entity;
        this.player = player;
    }

    private int getInterpolationSteps() {
        return interpolationSteps;
    }

    public static SimpleCollisionBox combineCollisionBox(SimpleCollisionBox one, SimpleCollisionBox two) {
        double minX = Math.min(one.minX, two.minX);
        double maxX = Math.max(one.maxX, two.maxX);
        double minY = Math.min(one.minY, two.minY);
        double maxY = Math.max(one.maxY, two.maxY);
        double minZ = Math.min(one.minZ, two.minZ);
        double maxZ = Math.max(one.maxZ, two.maxZ);

        return new SimpleCollisionBox(minX, minY, minZ, maxX, maxY, maxZ);
    }

    public static CollisionBox getOverlapHitbox(CollisionBox b1, CollisionBox b2) {
        if (b1 == NoCollisionBox.INSTANCE || b2 == NoCollisionBox.INSTANCE) {
            return NoCollisionBox.INSTANCE;
        } else if (!(b1 instanceof SimpleCollisionBox) || !(b2 instanceof SimpleCollisionBox)) {
            throw new IllegalArgumentException("Both b1 and b2 must be SimpleCollisionBox instances");
        }

        SimpleCollisionBox box1 = (SimpleCollisionBox) b1;
        SimpleCollisionBox box2 = (SimpleCollisionBox) b2;

        // Calculate the potential overlap along each axis
        double overlapMinX = Math.max(box1.minX, box2.minX);
        double overlapMaxX = Math.min(box1.maxX, box2.maxX);
        double overlapMinY = Math.max(box1.minY, box2.minY);
        double overlapMaxY = Math.min(box1.maxY, box2.maxY);
        double overlapMinZ = Math.max(box1.minZ, box2.minZ);
        double overlapMaxZ = Math.min(box1.maxZ, box2.maxZ);

        // Check if there's actual overlap along each axis
        if (overlapMinX > overlapMaxX || overlapMinY > overlapMaxY || overlapMinZ > overlapMaxZ) {
            return NoCollisionBox.INSTANCE; // No overlap, return null or an appropriate "empty" box representation
        }

        // Return the overlapping hitbox
        return new SimpleCollisionBox(overlapMinX, overlapMinY, overlapMinZ, overlapMaxX, overlapMaxY, overlapMaxZ);
    }

    /**
     * Calculates a bounding box that contains all possible positions where the entity could be located
     * during interpolation. This takes into account:<p>
     * • The starting position<br>
     * • The target position<br>
     * • The number of interpolation steps<br>
     * • The current interpolation progress (low and high bounds)<p>
     *
     * To avoid expensive branching when bruteforcing interpolation, this method combines
     * the collision boxes for all possible steps into a single bounding box. This approach
     * was specifically designed to handle the uncertainty of minimum interpolation,
     * maximum interpolation, and target location on 1.9+ clients while still supporting 1.7-1.8.<p>
     *
     * For each possible interpolation step between the bounds, it calculates the position
     * and combines all these positions into a single bounding box that encompasses all of them.
     *
     * @return A SimpleCollisionBox containing all possible positions of the entity during interpolation
     */
    public SimpleCollisionBox getPossibleLocationCombined() {
        int interpSteps = getInterpolationSteps();

        double stepMinX = (targetLocation.minX - startingLocation.minX) / (double) interpSteps;
        double stepMaxX = (targetLocation.maxX - startingLocation.maxX) / (double) interpSteps;
        double stepMinY = (targetLocation.minY - startingLocation.minY) / (double) interpSteps;
        double stepMaxY = (targetLocation.maxY - startingLocation.maxY) / (double) interpSteps;
        double stepMinZ = (targetLocation.minZ - startingLocation.minZ) / (double) interpSteps;
        double stepMaxZ = (targetLocation.maxZ - startingLocation.maxZ) / (double) interpSteps;

        SimpleCollisionBox minimumInterpLocation = new SimpleCollisionBox(
                startingLocation.minX + (interpolationStepsLowBound * stepMinX),
                startingLocation.minY + (interpolationStepsLowBound * stepMinY),
                startingLocation.minZ + (interpolationStepsLowBound * stepMinZ),
                startingLocation.maxX + (interpolationStepsLowBound * stepMaxX),
                startingLocation.maxY + (interpolationStepsLowBound * stepMaxY),
                startingLocation.maxZ + (interpolationStepsLowBound * stepMaxZ));

        for (int step = interpolationStepsLowBound + 1; step <= interpolationStepsHighBound; step++) {
            minimumInterpLocation = combineCollisionBox(minimumInterpLocation, new SimpleCollisionBox(
                    startingLocation.minX + (step * stepMinX),
                    startingLocation.minY + (step * stepMinY),
                    startingLocation.minZ + (step * stepMinZ),
                    startingLocation.maxX + (step * stepMaxX),
                    startingLocation.maxY + (step * stepMaxY),
                    startingLocation.maxZ + (step * stepMaxZ)));
        }

        return minimumInterpLocation;
    }

    /**
     * Builds upon getPossibleLocationCombined() to create a larger bounding box that contains
     * not just where the entity could be located, but where any part of its hitbox could be.
     * This is done by:<p>
     *
     * 1. Getting the possible locations using getPossibleLocationCombined()<br>
     * 2. If needed expand appropriately due to a recent teleport that moved the entity by:<br>
     *    • X: 0.03125D<br>
     *    • Y: 0.015625D<br>
     *    • Z: 0.03125D<br>
     * 3. Expanding by the entity's bounding box dimensions, but only expanding:<br>
     *    • Minimum coordinates by negative bounding box values<br>
     *    • Maximum coordinates by positive bounding box values<p>
     *
     * This ensures we have a box containing all possible hitbox positions during interpolation.
     *
     * @return A SimpleCollisionBox containing all possible hitbox positions during interpolation
     */
    public SimpleCollisionBox getPossibleHitboxCombined() {
        SimpleCollisionBox minimumInterpLocation = getPossibleLocationCombined();

        if (expandNonRelative)
            minimumInterpLocation.expand(0.03125D, 0.015625D, 0.03125D);

        GetBoundingBox.expandBoundingBoxByEntityDimensions(minimumInterpLocation, player, entity);

        return minimumInterpLocation;
    }

    public void updatePossibleStartingLocation(SimpleCollisionBox possibleLocationCombined) {
        //GrimAC.staticGetLogger().info(ChatColor.BLUE + "Updated new starting location as second trans hasn't arrived " + startingLocation);
        this.startingLocation = combineCollisionBox(startingLocation, possibleLocationCombined);
        //GrimAC.staticGetLogger().info(ChatColor.BLUE + "Finished updating new starting location as second trans hasn't arrived " + startingLocation);
    }

    public void tickMovement(boolean incrementLowBound, boolean tickingReliably) {
        if (!tickingReliably) this.interpolationStepsHighBound = getInterpolationSteps();
        if (incrementLowBound)
            this.interpolationStepsLowBound = Math.min(interpolationStepsLowBound + 1, getInterpolationSteps());
        this.interpolationStepsHighBound = Math.min(interpolationStepsHighBound + 1, getInterpolationSteps());
    }

    @Override
    public String toString() {
        return "ReachInterpolationData{" +
                "targetLocation=" + targetLocation +
                ", startingLocation=" + startingLocation +
                ", interpolationStepsLowBound=" + interpolationStepsLowBound +
                ", interpolationStepsHighBound=" + interpolationStepsHighBound +
                '}';
    }

    public void expandNonRelative() {
        expandNonRelative = true;
    }
}
