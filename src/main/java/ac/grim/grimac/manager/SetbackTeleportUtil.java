package ac.grim.grimac.manager;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.impl.badpackets.BadPacketsN;
import ac.grim.grimac.checks.type.PostPredictionCheck;
import ac.grim.grimac.events.packets.patch.ResyncWorldUtil;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.predictionengine.predictions.PredictionEngine;
import ac.grim.grimac.predictionengine.predictions.PredictionEngineElytra;
import ac.grim.grimac.predictionengine.predictions.PredictionEngineNormal;
import ac.grim.grimac.predictionengine.predictions.PredictionEngineWater;
import ac.grim.grimac.utils.anticheat.update.PredictionComplete;
import ac.grim.grimac.utils.chunks.Column;
import ac.grim.grimac.utils.collisions.datatypes.SimpleCollisionBox;
import ac.grim.grimac.utils.data.*;
import ac.grim.grimac.utils.math.GrimMath;
import ac.grim.grimac.utils.math.VectorUtils;
import ac.grim.grimac.utils.nmsutil.Collisions;
import ac.grim.grimac.utils.nmsutil.GetBoundingBox;
import ac.grim.grimac.utils.nmsutil.ReachUtils;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.protocol.teleport.RelativeFlag;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.play.server.*;
import io.github.retrooper.packetevents.util.FoliaCompatUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.util.Vector;

import java.util.Collections;
import java.util.HashSet;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;

public class SetbackTeleportUtil extends Check implements PostPredictionCheck {
    // Sync to netty
    public final ConcurrentLinkedQueue<TeleportData> pendingTeleports = new ConcurrentLinkedQueue<>();
    // Sync to netty, a player MUST accept a teleport to spawn into the world
    // A teleport is used to end the loading screen.  Some cheats pretend to never end the loading screen
    // in an attempt to disable the anticheat.  Be careful.
    // We fix this by blocking serverbound movements until the player is out of the loading screen.
    public boolean hasAcceptedSpawnTeleport = false;
    // Was there a ghost block that forces us to block offsets until the player accepts their teleport?
    public boolean blockOffsets = false;
    // This required setback data is the head of the teleport.
    // It is set by both bukkit and netty due to going on the bukkit thread to setback players
    private SetBackData requiredSetBack = null;
    public SetbackPosWithVector lastKnownGoodPosition;
    // Are we currently sending setback stuff?
    public boolean isSendingSetback = false;
    public int cheatVehicleInterpolationDelay = 0;
    private long lastWorldResync = 0;


    public SetbackTeleportUtil(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onPredictionComplete(final PredictionComplete predictionComplete) {
        // Grab friction now when we know player on ground and other variables
        Vector afterTickFriction = player.clientVelocity.clone();

        // We must first check if the player has accepted their setback
        // If the setback isn't complete, then this position is illegitimate
        if (predictionComplete.getData().getSetback() != null) {
            // The player needs to now wait for their vehicle to go into the right place before getting back in
            if (cheatVehicleInterpolationDelay > 0) cheatVehicleInterpolationDelay = 10;
            // Teleport, let velocity be reset
            lastKnownGoodPosition = new SetbackPosWithVector(new Vector3d(player.x, player.y, player.z), afterTickFriction);
        } else if (requiredSetBack == null || requiredSetBack.isComplete()) {
            cheatVehicleInterpolationDelay--;
            // No simulation... we can do that later. We just need to know the valid position.
            // As we didn't setback here, the new position is known to be safe!
            lastKnownGoodPosition = new SetbackPosWithVector(new Vector3d(player.x, player.y, player.z), afterTickFriction);
        }

        if (requiredSetBack != null) requiredSetBack.tick();
    }

    public void executeForceResync() {
        if (player.gamemode == GameMode.SPECTATOR || player.disableGrim)
            return; // We don't care about spectators, they don't flag
        if (lastKnownGoodPosition == null) return; // Player hasn't spawned yet
        blockMovementsUntilResync(true, true);
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
        if (player.bukkitPlayer != null && player.noSetbackPermission) return true;
        return false;
    }

    private void simulateFriction(Vector vector) {
        // We must always do this before simulating positions, as this is the last actual (safe) movement
        // We must not do this for knockback or explosions, as they are at the start of the tick
        if (player.wasTouchingWater) {
            PredictionEngineWater.staticVectorEndOfTick(player, vector, 0.8F, player.gravity, true);
        } else if (player.wasTouchingLava) {
            vector.multiply(0.5D);
            if (player.hasGravity)
                vector.add(new Vector(0.0D, -player.gravity / 4.0D, 0.0D));
        } else if (player.isGliding) {
            PredictionEngineElytra.getElytraMovement(player, vector, ReachUtils.getLook(player, player.xRot, player.yRot)).multiply(player.stuckSpeedMultiplier).multiply(new Vector(0.99F, 0.98F, 0.99F));
            vector.setY(vector.getY() - 0.05); // Make the player fall a bit
        } else { // Gliding doesn't have friction, we handle it differently
            PredictionEngineNormal.staticVectorEndOfTick(player, vector); // Lava and normal movement
        }

        // stop 1.8 players from stepping onto 1.25 high blocks, because why not?
        new PredictionEngine().applyMovementThreshold(player, new HashSet<>(Collections.singletonList(new VectorData(vector, VectorData.VectorType.BestVelPicked))));
    }

    private void blockMovementsUntilResync(boolean simulateNextTickPosition, boolean isResync) {
        if (requiredSetBack == null) return; // Hasn't spawned
        requiredSetBack.setPlugin(false); // The player has illegal movement, block from vanilla ac override
        if (isPendingSetback()) return; // Don't spam setbacks

        // Only let us full resync once every five seconds to prevent unneeded bukkit load
        if (System.currentTimeMillis() - lastWorldResync > 5 * 1000) {
            ResyncWorldUtil.resyncPositions(player, player.boundingBox.copy().expand(1));
            lastWorldResync = System.currentTimeMillis();
        }

        Vector clientVel = lastKnownGoodPosition.vector.clone();

        Pair<VelocityData, Vector> futureKb = player.checkManager.getKnockbackHandler().getFutureKnockback();
        VelocityData futureExplosion = player.checkManager.getExplosionHandler().getFutureExplosion();

        // Velocity sets
        if (futureKb.getFirst() != null) {
            clientVel = futureKb.getSecond();
        }

        // Explosion adds
        if (futureExplosion != null && (futureKb.getFirst() == null || futureKb.getFirst().transaction < futureExplosion.transaction)) {
            clientVel.add(futureExplosion.vector);
        }

        Vector3d position = lastKnownGoodPosition.pos;

        SimpleCollisionBox oldBB = player.boundingBox;
        player.boundingBox = GetBoundingBox.getPlayerBoundingBox(player, position.getX(), position.getY(), position.getZ());

        // Mini prediction engine - simulate collisions
        if (simulateNextTickPosition) {
            Vector collide = Collisions.collide(player, clientVel.getX(), clientVel.getY(), clientVel.getZ());

            position = position.withX(position.getX() + collide.getX());
            // 1.8 players need the collision epsilon to not phase into blocks when being setback
            // Due to simulation, this will not allow a flight bypass by sending a billion invalid movements
            position = position.withY(position.getY() + collide.getY() + SimpleCollisionBox.COLLISION_EPSILON);
            position = position.withZ(position.getZ() + collide.getZ());

            if (clientVel.getX() != collide.getX()) clientVel.setX(0);
            if (clientVel.getY() != collide.getY()) clientVel.setY(0);
            if (clientVel.getZ() != collide.getZ()) clientVel.setZ(0);

            simulateFriction(clientVel);
        }

        player.boundingBox = oldBB; // reset back to the new bounding box

        if (!hasAcceptedSpawnTeleport || player.isFlying) clientVel = null; // if the player is flying or hasn't spawned... don't force kb

        // Something weird has occurred in the player's movement, block offsets until we resync
        if (isResync) {
            blockOffsets = true;
        }

        SetBackData data = new SetBackData(new TeleportData(position, new RelativeFlag(0b11000), player.lastTransactionSent.get(), 0), player.xRot, player.yRot, clientVel, player.compensatedEntities.getSelf().getRiding() != null, false);
        sendSetback(data);
    }

    private final Random random = new Random();

    private void sendSetback(SetBackData data) {
        isSendingSetback = true;
        Vector3d position = data.getTeleportData().getLocation();

        try {
            // Player is in a vehicle
            if (player.compensatedEntities.getSelf().getRiding() != null) {
                int vehicleId = player.compensatedEntities.getPacketEntityID(player.compensatedEntities.getSelf().getRiding());
                if (player.compensatedEntities.serverPlayerVehicle != null) {
                    // Dismount player from vehicle
                    if (PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_9)) {
                        player.user.sendPacket(new WrapperPlayServerSetPassengers(vehicleId, new int[2]));
                    } else {
                        player.user.sendPacket(new WrapperPlayServerAttachEntity(vehicleId, -1, false));
                    }

                    // Stop the player from being able to teleport vehicles and simply re-enter them to continue,
                    // therefore, teleport the entity
                    player.user.sendPacket(new WrapperPlayServerEntityTeleport(vehicleId, new Vector3d(position.getX(), position.getY(), position.getZ()), player.xRot % 360, 0, false));
                    player.getSetbackTeleportUtil().cheatVehicleInterpolationDelay = Integer.MAX_VALUE; // Set to max until player accepts the new position

                    // Make sure bukkit also knows the player got teleported out of their vehicle, can't do this async
                    FoliaCompatUtil.runTaskForEntity(player.bukkitPlayer, GrimAPI.INSTANCE.getPlugin(), () -> {
                        if (player.bukkitPlayer != null) {
                            Entity vehicle = player.bukkitPlayer.getVehicle();
                            if (vehicle != null) {
                                vehicle.eject();
                            }
                        }
                    }, null, 0);
                }
            }

            double y = position.getY();
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
            addSentTeleport(new Location(null, position.getX(), y, position.getZ(), player.xRot % 360, player.yRot % 360), data.getTeleportData().getTransaction(), new RelativeFlag(0b11000), false, teleportId);
            // This must be done after setting the sent teleport, otherwise we lose velocity data
            requiredSetBack = data;
            // Send after tracking to fix race condition
            PacketEvents.getAPI().getProtocolManager().sendPacketSilently(player.user.getChannel(), new WrapperPlayServerPlayerPositionAndLook(position.getX(), position.getY(), position.getZ(), 0, 0, data.getTeleportData().getFlags().getMask(), teleportId, false));
            player.sendTransaction();

            if (data.getVelocity() != null && data.getVelocity().lengthSquared() > 0) {
                player.user.sendPacket(new WrapperPlayServerEntityVelocity(player.entityID, new Vector3d(data.getVelocity().getX(), data.getVelocity().getY(), data.getVelocity().getZ())));
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
            double trueTeleportX = (teleportPos.isRelativeX() ? player.x : 0) + teleportPos.getLocation().getX();
            double trueTeleportY = (teleportPos.isRelativeY() ? player.y : 0) + teleportPos.getLocation().getY();
            double trueTeleportZ = (teleportPos.isRelativeZ() ? player.z : 0) + teleportPos.getLocation().getZ();

            // There seems to be a version difference in teleports past 30 million... just clamp the vector
            Vector3d clamped = VectorUtils.clampVector(new Vector3d(trueTeleportX, trueTeleportY, trueTeleportZ));
            double threshold = teleportPos.isRelativeX() ? player.getMovementThreshold() : 0;
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
                player.checkManager.getPacketCheck(BadPacketsN.class).flagAndAlert();
                pendingTeleports.poll();
                requiredSetBack.setPlugin(false);
                if (pendingTeleports.isEmpty()) {
                    sendSetback(requiredSetBack);
                }
                continue;
            }
            // No farther setbacks before the player's transactoin
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
            Pair<Integer, Vector3d> teleportPos = player.vehicleData.vehicleTeleports.peek();
            if (teleportPos == null) break;
            if (lastTransaction < teleportPos.getFirst()) {
                break;
            }

            Vector3d position = teleportPos.getSecond();
            if (position.getX() == x && position.getY() == y && position.getZ() == z) {
                player.vehicleData.vehicleTeleports.poll();

                return true;
            } else if (lastTransaction > teleportPos.getFirst() + 1) {
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
        return !player.disableGrim && (column == null || column.transaction >= player.lastTransactionReceived.get() ||
                // The player hasn't loaded past the DOWNLOADING TERRAIN screen
                !player.getSetbackTeleportUtil().hasAcceptedSpawnTeleport);
    }

    /**
     * @return The current data for the setback, regardless of whether it is complete or not
     */
    public SetBackData getRequiredSetBack() {
        return requiredSetBack;
    }

    public void addSentTeleport(Location position, int transaction, RelativeFlag flags, boolean plugin, int teleportId) {
        TeleportData data = new TeleportData(new Vector3d(position.getX(), position.getY(), position.getZ()), flags, transaction, teleportId);
        pendingTeleports.add(data);

        Vector3d safePosition = new Vector3d(position.getX(), position.getY(), position.getZ());

        // We must convert relative teleports to avoid them becoming client controlled in the case of setback
        if (flags.isSet(RelativeFlag.X.getMask())) {
            safePosition = safePosition.withX(safePosition.getX() + lastKnownGoodPosition.pos.getX());
        }

        if (flags.isSet(RelativeFlag.Y.getMask())) {
            safePosition = safePosition.withY(safePosition.getY() + lastKnownGoodPosition.pos.getY());
        }

        if (flags.isSet(RelativeFlag.Z.getMask())) {
            safePosition = safePosition.withZ(safePosition.getZ() + lastKnownGoodPosition.pos.getZ());
        }

        data = new TeleportData(safePosition, flags, transaction, teleportId);
        requiredSetBack = new SetBackData(data, player.xRot, player.yRot, null, false, plugin);

        this.lastKnownGoodPosition = new SetbackPosWithVector(safePosition, new Vector());
    }

    @AllArgsConstructor
    @Getter
    @Setter
    public static class SetbackPosWithVector {
        private Vector3d pos;
        private Vector vector;
    }
}
