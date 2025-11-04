package ac.grim.grimac.manager;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.impl.badpackets.BadPacketsN;
import ac.grim.grimac.checks.type.PostPredictionCheck;
import ac.grim.grimac.platform.api.entity.GrimEntity;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.predictionengine.predictions.PredictionEngine;
import ac.grim.grimac.predictionengine.predictions.PredictionEngineElytra;
import ac.grim.grimac.predictionengine.predictions.PredictionEngineNormal;
import ac.grim.grimac.predictionengine.predictions.PredictionEngineWater;
import ac.grim.grimac.utils.anticheat.update.PredictionComplete;
import ac.grim.grimac.utils.chunks.Column;
import ac.grim.grimac.utils.collisions.datatypes.SimpleCollisionBox;
import ac.grim.grimac.utils.data.Pair;
import ac.grim.grimac.utils.data.SetBackData;
import ac.grim.grimac.utils.data.TeleportAcceptData;
import ac.grim.grimac.utils.data.TeleportData;
import ac.grim.grimac.utils.data.VectorData;
import ac.grim.grimac.utils.data.VehicleData;
import ac.grim.grimac.utils.data.VelocityData;
import ac.grim.grimac.utils.math.GrimMath;
import ac.grim.grimac.utils.math.Vector3dm;
import ac.grim.grimac.utils.math.VectorUtils;
import ac.grim.grimac.utils.nmsutil.Collisions;
import ac.grim.grimac.utils.nmsutil.GetBoundingBox;
import ac.grim.grimac.utils.nmsutil.ReachUtils;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.protocol.teleport.RelativeFlag;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerAttachEntity;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityTeleport;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityVelocity;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerPositionAndLook;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetPassengers;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.Collections;
import java.util.HashSet;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;

public class SetbackTeleportUtil extends Check implements PostPredictionCheck {
    // Sync to netty
    public final ConcurrentLinkedQueue<TeleportData> pendingTeleports = new ConcurrentLinkedQueue<>();
    private final Random random = new Random();
    // Sync to netty, a player MUST accept a teleport to spawn into the world
    // A teleport is used to end the loading screen.  Some cheats pretend to never end the loading screen
    // in an attempt to disable the anticheat.  Be careful.
    // We fix this by blocking serverbound movements until the player is out of the loading screen.
    public boolean hasAcceptedSpawnTeleport = false;
    // Was there a ghost block that forces us to block offsets until the player accepts their teleport?
    public boolean blockOffsets = false;
    public SetbackPosWithVector lastKnownGoodPosition;
    // Are we currently sending setback stuff?
    public boolean isSendingSetback = false;
    public int cheatVehicleInterpolationDelay = 0;
    // This required setback data is the head of the teleport.
    // It is set by both bukkit and netty due to going on the bukkit thread to setback players
    @Getter
    private SetBackData requiredSetBack = null;
    private long lastWorldResync = 0;

    public SetbackTeleportUtil(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onPredictionComplete(final PredictionComplete predictionComplete) {
        // Grab friction now when we know player on ground and other variables
        final double afterTickFrictionX = player.clientVelocity.getX();
        final double afterTickFrictionY = player.clientVelocity.getY();
        final double afterTickFrictionZ = player.clientVelocity.getZ();

        // We must first check if the player has accepted their setback
        // If the setback isn't complete, then this position is illegitimate
        if (predictionComplete.getData().getSetback() != null) {
            // The player needs to now wait for their vehicle to go into the right place before getting back in
            if (cheatVehicleInterpolationDelay > 0) cheatVehicleInterpolationDelay = 10;
            // Teleport, let velocity be reset
            lastKnownGoodPosition = new SetbackPosWithVector(player.x, player.y, player.z, afterTickFrictionX, afterTickFrictionY, afterTickFrictionZ);
        } else if (requiredSetBack == null || requiredSetBack.isComplete()) {
            cheatVehicleInterpolationDelay--;
            // No simulation... we can do that later. We just need to know the valid position.
            // As we didn't setback here, the new position is known to be safe!
            lastKnownGoodPosition = new SetbackPosWithVector(player.x, player.y, player.z, afterTickFrictionX, afterTickFrictionY, afterTickFrictionZ);
        }

        if (requiredSetBack != null) requiredSetBack.tick();
    }

    public void executeForceResync() {
        if (player.gamemode == GameMode.SPECTATOR || player.disableGrim)
            return; // We don't care about spectators, they don't flag
        if (lastKnownGoodPosition == null) return; // Player hasn't spawned yet
        blockMovementsUntilResync(true, true);
    }

    public void executeForceResyncNoSimulation() {
        if (player.gamemode == GameMode.SPECTATOR || player.disableGrim)
            return; // We don't care about spectators, they don't flag
        if (lastKnownGoodPosition == null) return; // Player hasn't spawned yet
        blockMovementsUntilResync(false, true);
    }

    public void executeNonSimulatingSetback() {
        if (player.gamemode == GameMode.SPECTATOR || player.disableGrim)
            return; // We don't care about spectators, they don't flag
        if (lastKnownGoodPosition == null) return; // Player hasn't spawned yet
        blockMovementsUntilResync(false, false);
    }

    public boolean executeViolationSetback() {
        if (isExempt()) return false;
        blockMovementsUntilResync(true, false);
        return true;
    }

    private boolean isExempt() {
        // Not exempting spectators here because timer check for spectators is actually valid.
        // Player hasn't spawned yet
        if (lastKnownGoodPosition == null) return true;
        // Setbacks aren't allowed
        if (player.disableGrim) return true;
        // Player has permission to cheat, permission not given to OP by default.
        return player.platformPlayer != null && player.noSetbackPermission;
    }

    private void blockMovementsUntilResync(boolean simulateNextTickPosition, boolean isResync) {
        if (requiredSetBack == null) return; // Hasn't spawned
        if (player.platformPlayer != null && player.noSetbackPermission)
            return; // The player has permission to cheat
        requiredSetBack.setPlugin(false); // The player has illegal movement, block from vanilla ac override
        if (isPendingSetback()) return; // Don't spam setbacks

        // Only let us full resync once every five seconds to prevent unneeded bukkit load
        if (System.currentTimeMillis() - lastWorldResync > 5 * 1000) {
            player.resyncPositions(player.boundingBox.copy().expand(1));
            lastWorldResync = System.currentTimeMillis();
        }

        double clientVelX = lastKnownGoodPosition.getVectorX();
        double clientVelY = lastKnownGoodPosition.getVectorY();
        double clientVelZ = lastKnownGoodPosition.getVectorZ();

        Pair<VelocityData, Vector3dm> futureKb = player.checkManager.getKnockbackHandler().getFutureKnockback();
        VelocityData futureExplosion = player.checkManager.getExplosionHandler().getFutureExplosion();

        // Velocity sets
        if (futureKb.first() != null) {
            clientVelX = futureKb.second().getX();
            clientVelY = futureKb.second().getY();
            clientVelZ = futureKb.second().getZ();
        }

        // Explosion adds
        if (futureExplosion != null && (futureKb.first() == null || futureKb.first().transaction < futureExplosion.transaction)) {
            clientVelX += futureExplosion.vector.getX();
            clientVelY += futureExplosion.vector.getY();
            clientVelZ += futureExplosion.vector.getZ();
        }

        double x = lastKnownGoodPosition.posX;
        double y = lastKnownGoodPosition.posY;
        double z = lastKnownGoodPosition.posZ;

        SimpleCollisionBox oldBB = player.boundingBox;
        player.boundingBox = GetBoundingBox.getPlayerBoundingBox(player, x, y, z);

        // Mini prediction engine - simulate collisions
        if (simulateNextTickPosition) {
            Vector3dm collide = Collisions.collide(player, clientVelX, clientVelY, clientVelZ);

            x += collide.getX();
            y += collide.getY();
            // TODO: Is this even needed? Can't reproduce any phasing on vanilla 1.8 when being setback.
            if (player.getClientVersion().isOlderThan(ClientVersion.V_1_9)) {
                // 1.8 players need the collision epsilon to not phase into blocks when being setback
                // Due to simulation, this will not allow a flight bypass by sending a billion invalid movements
                y += SimpleCollisionBox.COLLISION_EPSILON;
            }
            z += collide.getZ();

            if (clientVelX != collide.getX()) clientVelX = 0;
            if (clientVelY != collide.getY()) clientVelY = 0;
            if (clientVelZ != collide.getZ()) clientVelZ = 0;

            // BEGIN SIMULATING FRICTION
            // We must always do this before simulating positions, as this is the last actual (safe) movement
            // We must not do this for knockback or explosions, as they are at the start of the tick
            double[] vector = {clientVelX, clientVelY, clientVelZ};
            if (player.wasTouchingWater) {
                PredictionEngineWater.staticVectorEndOfTick(player, vector, 0.8F, player.gravity, true);
            } else if (player.wasTouchingLava) {
                vector[0] *= 0.5D;
                vector[1] *= 0.5D;
                vector[2] *= 0.5D;
                if (player.hasGravity)
                    vector[1] += -player.gravity / 4.0D;
            } else if (player.isGliding) {
                Vector3dm getLook = ReachUtils.getLook(player, player.yaw, player.pitch);
                PredictionEngineElytra.applyElytraMovementInPlace(player, vector, getLook.getX(), getLook.getY(), getLook.getZ());
                vector[0] *= player.stuckSpeedMultiplier.getX() * 0.99F;
                vector[1] *= player.stuckSpeedMultiplier.getY() * 0.98F;
                vector[2] *= player.stuckSpeedMultiplier.getZ() * 0.99F;

                vector[1] -= 0.05; // Make the player fall a bit
            } else { // Gliding doesn't have friction, we handle it differently

                // Lava and normal movement
                vector[0] *= player.friction;
                vector[1] = PredictionEngineNormal.staticVectorEndOfTickY(player, vector[1]);
                vector[2] *= player.friction;

                // Prevent abusing setbacks to move out of blocks like webs
                vector[0] *= player.stuckSpeedMultiplier.getX();
                vector[1] *= player.stuckSpeedMultiplier.getY();
                vector[2] *= player.stuckSpeedMultiplier.getZ();
            }

            // stop 1.8 players from stepping onto 1.25 high blocks, because why not?
            new PredictionEngine().applyMovementThreshold(player, new HashSet<>(Collections.singletonList(new VectorData(vector[0], vector[1], vector[2], VectorData.VectorType.BestVelPicked))));
            // END SIMULATING FRICTION

            clientVelX = vector[0];
            clientVelY = vector[1];
            clientVelZ = vector[2];
        }

        player.boundingBox = oldBB; // reset back to the new bounding box

        boolean sendVelocity = true;

        if (!hasAcceptedSpawnTeleport || player.isFlying)
            sendVelocity = false; // if the player is flying or hasn't spawned... don't force kb

        // Something weird has occurred in the player's movement, block offsets until we resync
        if (isResync) {
            blockOffsets = true;
        }

        SetBackData data = new SetBackData(new TeleportData(x, y, z, 0, 0, 0, new RelativeFlag(0b11000), player.lastTransactionSent.get(), 0), player.yaw, player.pitch, clientVelX, clientVelY, clientVelZ, sendVelocity, player.inVehicle(), false);
        sendSetback(data);
    }

    private void sendSetback(SetBackData data) {
        isSendingSetback = true;
        double positionX = data.getTeleportData().getLocationX();
        double positionY = data.getTeleportData().getLocationY();
        double positionZ = data.getTeleportData().getLocationZ();

        try {
            // Player is in a vehicle
            if (player.inVehicle()) {
                int vehicleId = player.getRidingVehicleId();
                if (player.compensatedEntities.serverPlayerVehicle != null) {
                    // Dismount player from vehicle
                    if (PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_9)) {
                        player.user.sendPacket(new WrapperPlayServerSetPassengers(vehicleId, new int[2]));
                    } else {
                        player.user.sendPacket(new WrapperPlayServerAttachEntity(vehicleId, -1, false));
                    }

                    // Stop the player from being able to teleport vehicles and simply re-enter them to continue,
                    // therefore, teleport the entity
                    player.user.sendPacket(new WrapperPlayServerEntityTeleport(vehicleId, new Vector3d(positionX, positionY, positionZ), player.yaw % 360, 0, false));
                    player.getSetbackTeleportUtil().cheatVehicleInterpolationDelay = Integer.MAX_VALUE; // Set to max until player accepts the new position

                    // Make sure bukkit also knows the player got teleported out of their vehicle, can't do this async
                    GrimAPI.INSTANCE.getScheduler().getEntityScheduler().execute(player.platformPlayer, GrimAPI.INSTANCE.getGrimPlugin(), () -> {
                        if (player.platformPlayer != null) {
                            GrimEntity vehicle = player.platformPlayer.getVehicle();
                            if (vehicle != null) {
                                vehicle.eject();
                            }
                        }
                    }, null, 0);
                }
            }

            double y = positionY;
            if (PacketEvents.getAPI().getServerManager().getVersion().isOlderThanOrEquals(ServerVersion.V_1_7_10)) {
                y += 1.62; // 1.7 teleport offset if grim ever supports 1.7 again
            }

            // Send a transaction now to make sure there's always transactions around teleport
            player.sendTransaction();

            // Min value is 10000000000000000000000000000000 in binary, this makes sure the number is always < 0
            int teleportId = random.nextInt() | Integer.MIN_VALUE;
            data.setPlugin(false);
            data.getTeleportData().setTeleportId(teleportId);
            data.getTeleportData().setTransaction(player.lastTransactionSent.get());

            // Use provided transaction ID to make sure it can never desync, although there's no reason to do this
            addSentTeleport(positionX, y, positionZ, 0, 0, 0, data.getTeleportData().getTransaction(), new RelativeFlag(0b11000), false, teleportId);
            // This must be done after setting the sent teleport, otherwise we lose velocity data
            requiredSetBack = data;
            // Send after tracking to fix race condition
            PacketEvents.getAPI().getProtocolManager().sendPacketSilently(player.user.getChannel(), new WrapperPlayServerPlayerPositionAndLook(positionX, positionY, positionZ, 0, 0, data.getTeleportData().getFlags().getMask(), teleportId, false));
            player.sendTransaction();

            if (data.isHasVelocity() && (data.getVelocityX() != 0 || data.getVelocityY() != 0 || data.getVelocityZ() != 0)) {
                player.user.sendPacket(new WrapperPlayServerEntityVelocity(player.entityID, new Vector3d(data.getVelocityX(), data.getVelocityY(), data.getVelocityZ())));
            }
        } finally {
            isSendingSetback = false;
        }
    }

    /**
     * @param x - Player X position
     * @param y - Player Y position
     * @param z - Player Z position
     * @return - Whether the player has completed a teleport by being at this position
     */
    public TeleportAcceptData checkTeleportQueue(double x, double y, double z) {
        // Support teleports without teleport confirmations
        // If the player is in a vehicle when teleported, they will exit their vehicle
        TeleportAcceptData teleportData = new TeleportAcceptData();

        TeleportData teleportPos;
        while ((teleportPos = pendingTeleports.peek()) != null) {
            double trueTeleportX = (teleportPos.isRelativeX() ? player.x : 0) + teleportPos.getLocationX();
            double trueTeleportY = (teleportPos.isRelativeY() ? player.y : 0) + teleportPos.getLocationY();
            double trueTeleportZ = (teleportPos.isRelativeZ() ? player.z : 0) + teleportPos.getLocationZ();

            // There seems to be a version difference in teleports past 30 million... just clamp the vector
            Vector3d clamped = VectorUtils.clampVector(trueTeleportX, trueTeleportY, trueTeleportZ);
            double threshold = teleportPos.isRelativePos() ? player.getMovementThreshold() : 0;
            boolean closeEnoughY = Math.abs(clamped.getY() - y) <= 1e-7 + threshold; // 1.7 rounding

            if (player.lastTransactionReceived.get() == teleportPos.getTransaction() && Math.abs(clamped.getX() - x) <= threshold && closeEnoughY && Math.abs(clamped.getZ() - z) <= threshold) {
                pendingTeleports.poll();
                hasAcceptedSpawnTeleport = true;
                blockOffsets = false;

                // Player has accepted their setback!
                // We can compare transactions to check if equals because each teleport gets its own transaction
                if (requiredSetBack != null && requiredSetBack.getTeleportData().getTransaction() == teleportPos.getTransaction()) {
                    teleportData.setSetback(requiredSetBack);
                    requiredSetBack.setComplete(true);
                }

                teleportData.setTeleportData(teleportPos);
                teleportData.setTeleport(true);
                break;
            } else if (player.lastTransactionReceived.get() > teleportPos.getTransaction()) {
                // The player ignored the teleport (and this teleport matters), resynchronize
                player.checkManager.getCheck(BadPacketsN.class).flagAndAlert();
                pendingTeleports.poll();
                requiredSetBack.setPlugin(false);
                if (pendingTeleports.isEmpty()) {
                    sendSetback(requiredSetBack);
                }
                continue;
            }
            // No farther setbacks before the player's transaction
            break;
        }

        return teleportData;
    }

    /**
     * @param x - Player X position
     * @param y - Player Y position
     * @param z - Player Z position
     * @return - Whether the player has completed a teleport by being at this position
     */
    public boolean checkVehicleTeleportQueue(double x, double y, double z) {
        int lastTransaction = player.lastTransactionReceived.get();

        while (true) {
            VehicleData.VehicleTeleport teleportPos = player.vehicleData.vehicleTeleports.peek();
            if (teleportPos == null) break;
            if (lastTransaction < teleportPos.teleportId()) {
                break;
            }

            if (teleportPos.x() == x && teleportPos.y() == y && teleportPos.z() == z) {
                player.vehicleData.vehicleTeleports.poll();

                return true;
            } else if (lastTransaction > teleportPos.teleportId() + 1) {
                player.vehicleData.vehicleTeleports.poll();

                // Vehicles have terrible netcode so just ignore it if the teleport wasn't from us setting the player back
                // Players don't have to respond to vehicle teleports if they aren't controlling the entity anyways
                continue;
            }

            break;
        }

        return false;
    }

    /**
     * @return If the player is in a desync state and is waiting on information from the server
     */
    public boolean shouldBlockMovement() {
        // This is required to ensure protection from servers teleporting from CREATIVE to SURVIVAL
        // I should likely refactor
        return insideUnloadedChunk() || blockOffsets || (requiredSetBack != null && !requiredSetBack.isComplete());
    }

    private boolean isPendingSetback() {
        // Relative setbacks shouldn't count
        if (requiredSetBack.getTeleportData().isRelativeX() || requiredSetBack.getTeleportData().isRelativeY() || requiredSetBack.getTeleportData().isRelativeZ()) {
            return false;
        }
        // The setback is not complete
        return requiredSetBack != null && !requiredSetBack.isComplete();
    }

    /**
     * When the player is inside an unloaded chunk, they simply fall through the void which shouldn't be checked
     *
     * @return Whether the player has loaded the chunk and accepted a teleport to correct movement or not
     */
    public boolean insideUnloadedChunk() {
        Column column = player.compensatedWorld.getChunk(GrimMath.floor(player.x) >> 4, GrimMath.floor(player.z) >> 4);

        // If true, the player is in an unloaded chunk
        return !player.disableGrim && (column == null || column.transaction() >= player.lastTransactionReceived.get() ||
                // The player hasn't loaded past the DOWNLOADING TERRAIN screen
                !player.getSetbackTeleportUtil().hasAcceptedSpawnTeleport);
    }

    public void addSentTeleport(double x, double y, double z, double velocityX, double velocityY, double velocityZ, int transaction, RelativeFlag flags, boolean plugin, int teleportId) {
        TeleportData data = new TeleportData(x, y, z, velocityX, velocityY, velocityZ, flags, transaction, teleportId);
        pendingTeleports.add(data);

        // We must convert relative teleports to avoid them becoming client controlled in the case of setback
        if (flags.has(RelativeFlag.X)) {
            x += lastKnownGoodPosition.posX;
        }

        if (flags.has(RelativeFlag.Y)) {
            y += lastKnownGoodPosition.posY;
        }

        if (flags.has(RelativeFlag.Z)) {
            z += lastKnownGoodPosition.posZ;
        }

        data = new TeleportData(x, y, z, velocityX, velocityY, velocityZ, new RelativeFlag(0b11000), transaction, teleportId);
        requiredSetBack = new SetBackData(data, player.yaw, player.pitch, false, plugin);

        this.lastKnownGoodPosition = new SetbackPosWithVector(x, y, z, 0, 0, 0);
    }

    @AllArgsConstructor
    @Getter
    @Setter
    public static class SetbackPosWithVector {
        private final double posX;
        private final double posY;
        private final double posZ;
        private final double vectorY;
        private double vectorX, vectorZ;
    }
}
