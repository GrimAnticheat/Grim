package ac.grim.grimac.utils.data.interpolation;

import com.github.retrooper.packetevents.protocol.vector.positionpath.PositionPath;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.collisions.datatypes.SimpleCollisionBox;
import ac.grim.grimac.utils.data.ReachInterpolationData;
import ac.grim.grimac.utils.data.packetentity.PacketEntity;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import org.jetbrains.annotations.Nullable;

public final class LegacyEntityInterpolation implements EntityInterpolation {

    private final GrimPlayer player;
    private final PacketEntity entity;

    private ReachInterpolationData oldPacketLocation;
    private ReachInterpolationData newPacketLocation;

    /**
     * Rotation the current interpolation is heading towards, mirroring vanilla
     * {@code InterpolationHandler}'s target yRot/xRot. Only used to decide whether an
     * incoming packet restates the running interpolation; the hitbox itself is
     * position-only. Grim convention: xRot is yaw, yRot is pitch.
     */
    private float interpolationTargetXRot, interpolationTargetYRot;

    public LegacyEntityInterpolation(GrimPlayer player, PacketEntity entity, SimpleCollisionBox position) {
        this.player = player;
        this.entity = entity;
        this.newPacketLocation = new ReachInterpolationData(player, position, entity.trackedServerPosition, entity);
    }

    // Set the old packet location to the new one
    // Set the new packet location to the updated packet location
    @Override
    public void begin(@Nullable PositionPath path, boolean relative, boolean hasPos,
                      double relX, double relY, double relZ,
                      @Nullable Float packetXRot, @Nullable Float packetYRot, boolean positionSync, int transaction) {

        // Vanilla's InterpolationHandler (1.21.5+) in 1.21.9+ only restarts the lerp when the
        // incoming target differs from the one it is already heading towards, so a
        // packet that merely restates the current target is a no-op client side.
        // Restarting it here instead leaves our interpolation permanently trailing
        // the client whenever a server re-sends the same position, which reads as
        // the entity hitbox sitting slightly off and false flags Hitboxes/Reach.
        //
        // Strictly 1.21.9+. InterpolationHandler exists from 1.21.5 but without this
        // equality guard, so 1.21.5 -> 1.21.8 really does restart on every packet
        // (the same absence that reintroduced MC-255263 for those builds), as does
        // the pre-1.21.5 Entity#lerpTo path. Restarting unconditionally is correct
        // there, which is why the freeze modelling below stays untouched: its
        // version range ends at 1.21.9, exactly where this begins.
        //
        // This covers rotation-only packets too. Vanilla routes them through
        // Entity#moveOrInterpolateTo(yRot, xRot), whose absent position defaults to
        // InterpolationHandler#position() - the current interpolation target while
        // steps > 0 - so the position term compares equal and an unchanged rotation
        // makes the whole packet a no-op.
        //
        // Skipping is safe for transaction splitting. We leave oldPacketLocation
        // untouched, so an in-flight uncertainty window from an earlier packet is
        // preserved rather than collapsed, and the redundant packet's own
        // onSecondTransaction only clears a window that its transaction already
        // proves the client has passed.
        if (player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_21_9)
                && newPacketLocation.restatesTarget(entity.trackedServerPosition.getPos(),
                packetXRot, packetYRot, interpolationTargetXRot, interpolationTargetYRot)) {
            return;
        }

        if (packetXRot != null && packetYRot != null) {
            this.interpolationTargetXRot = packetXRot;
            this.interpolationTargetYRot = packetYRot;
        }

        this.oldPacketLocation = newPacketLocation;
        // BUG FIX LOGIC for https://bugs.mojang.com/browse/MC-255263
        // 1. We MUST check !hasPos. If hasPos is true, we must let standard interpolation (4-arg) run.
        // 2. The 3-arg constructor is for versions where the client FREEZES (targets current pos) when rot only packets come in
        if (!hasPos &&
                // Logic for versions that FREEZE (Target = Current)
                // 1.21.5 -> 1.21.8 (regression)
                ((player.getClientVersion().isOlderThan(ClientVersion.V_1_21_9) && player.getClientVersion().isNewerThan(ClientVersion.V_1_21_4)) ||
                        // 1.15 -> 1.20.1 (Old bug)
                        (player.getClientVersion().isOlderThan(ClientVersion.V_1_20_2) && player.getClientVersion().isNewerThan(ClientVersion.V_1_14_4)))
        ) {
            // Apply Freeze Fix (Start = Box, Target = Box)
            this.newPacketLocation = new ReachInterpolationData(
                    player,
                    oldPacketLocation.getPossibleLocationCombined(),
                    entity
            );
        } else {
            // Standard Interpolation (Start = Box, Target = ServerPos)
            // This naturally fixes the "Slowdown"/Interpolation Reset in 1.20.2-1.21.4 and 1.21.9+ resetting the lerp timer
            this.newPacketLocation = new ReachInterpolationData(player, oldPacketLocation.getPossibleLocationCombined(), entity.trackedServerPosition, entity);
        }

        // In versions < 1.16.2 when the client receives non-relative teleport for an entity
        // And they move less by the thresholds given, the entity does not move client side
        if (hasPos && !relative && player.getClientVersion().isOlderThanOrEquals(ClientVersion.V_1_16_1)) {
            SimpleCollisionBox clientArea = newPacketLocation.getPossibleLocationCombined();
            if (clientArea.distanceX(relX) < 0.03125D
                    && clientArea.distanceY(relY) < 0.015625D
                    && clientArea.distanceZ(relZ) < 0.03125D) {
                newPacketLocation.expandNonRelative();
            }
        }
    }

    // Remove the possibility of the old packet location
    @Override
    public void confirm(int transaction) {
        this.oldPacketLocation = null;
    }

    // If the old and new packet location are split, we need to combine bounding boxes
    @Override
    public void tick(boolean tickingReliably) {
        newPacketLocation.tickMovement(oldPacketLocation == null, tickingReliably);

        // Handle uncertainty of second transaction spanning over multiple ticks
        if (oldPacketLocation != null) {
            oldPacketLocation.tickMovement(true, tickingReliably);
            newPacketLocation.updatePossibleStartingLocation(oldPacketLocation.getPossibleLocationCombined());
        }
    }

    // This is for handling riding and entities attached to one another.
    @Override
    public void reset(SimpleCollisionBox box) {
        // This disables interpolation
        this.newPacketLocation = new ReachInterpolationData(player, box, entity);
    }

    @Override
    public SimpleCollisionBox position() {
        if (oldPacketLocation == null) {
            return newPacketLocation.getPossibleLocationCombined();
        }

        return ReachInterpolationData.combineCollisionBox(oldPacketLocation.getPossibleLocationCombined(), newPacketLocation.getPossibleLocationCombined());
    }

    @Override
    public SimpleCollisionBox hitbox(GrimPlayer player, PacketEntity entity) {
        if (oldPacketLocation == null) {
            return newPacketLocation.getPossibleHitboxCombined();
        }

        return ReachInterpolationData.combineCollisionBox(oldPacketLocation.getPossibleHitboxCombined(), newPacketLocation.getPossibleHitboxCombined());
    }

}
