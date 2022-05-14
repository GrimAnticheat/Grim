package ac.grim.grimac.manager;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.checks.type.PostPredictionCheck;
import ac.grim.grimac.events.packets.patch.ResyncWorldUtil;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.predictionengine.predictions.PredictionEngineNormal;
import ac.grim.grimac.predictionengine.predictions.PredictionEngineWater;
import ac.grim.grimac.utils.anticheat.update.PredictionComplete;
import ac.grim.grimac.utils.chunks.Column;
import ac.grim.grimac.utils.collisions.datatypes.SimpleCollisionBox;
import ac.grim.grimac.utils.data.Pair;
import ac.grim.grimac.utils.data.SetBackData;
import ac.grim.grimac.utils.data.SetbackLocationVelocity;
import ac.grim.grimac.utils.data.TeleportAcceptData;
import ac.grim.grimac.utils.math.GrimMath;
import ac.grim.grimac.utils.math.VectorUtils;
import ac.grim.grimac.utils.nmsutil.Collisions;
import ac.grim.grimac.utils.nmsutil.GetBoundingBox;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.protocol.world.Location;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.play.server.*;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.util.Vector;

import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;

public class SetbackTeleportUtil extends PostPredictionCheck {
    // Sync to netty
    private final ConcurrentLinkedQueue<Pair<Integer, Location>> teleports = new ConcurrentLinkedQueue<>();
    // Sync to netty, a player MUST accept a teleport to spawn into the world
    // A teleport is used to end the loading screen.  Some cheats pretend to never end the loading screen
    // in an attempt to disable the anticheat.  Be careful.
    // We fix this by blocking serverbound movements until the player is out of the loading screen.
    public boolean hasAcceptedSpawnTeleport = false;
    // Was there a ghost block that forces us to block offsets until the player accepts their teleport?
    public boolean blockOffsets = false;
    // This patches timer from being able to crash predictions.
    public boolean blockPredictions = false;
    // Resetting velocity can be abused to "fly"
    // Therefore, only allow one setback position every half second to patch this flight exploit
    public int setbackConfirmTicksAgo = 0;
    // This required setback data is the head of the teleport.
    // It is set by both bukkit and netty due to going on the bukkit thread to setback players
    SetBackData requiredSetBack = null;
    // Sync to netty to stop excessive resync's
    long lastWorldResync = 0;
    // A legal place to setback the player to
    public SetbackLocationVelocity safeTeleportPosition;
    // Are we currently sending setback stuff?
    public boolean isSendingSetback = false;

    public SetbackTeleportUtil(GrimPlayer player) {
        super(player);
    }

    /**
     * Generates safe setback locations by looking at the current prediction
     * <p>
     * 2021-10-9 This method seems to be safe and doesn't allow bypasses
     */
    @Override
    public void onPredictionComplete(final PredictionComplete predictionComplete) {
        // We must first check if the player has accepted their setback
        // If the setback isn't complete, then this position is illegitimate
        if (predictionComplete.getData().getSetback() != null) {
            // The player did indeed accept the setback, and there are no new setbacks past now!
            setbackConfirmTicksAgo = 0;
            // Teleport, let velocity be reset
            safeTeleportPosition = new SetbackLocationVelocity(new Vector3d(player.x, player.y, player.z));
            blockOffsets = false;
            blockPredictions = false;
        } else if (requiredSetBack == null || requiredSetBack.isComplete()) {
            setbackConfirmTicksAgo++;
            // No simulation... we can do that later. We just need to know the valid position.
            // Don't worry about accidentally setting before a teleport, teleports set lastX/Y/Z to teleport position
            // TODO: Simplify setback logic to not include safe teleport position
            safeTeleportPosition = new SetbackLocationVelocity(new Vector3d(player.lastX, player.lastY, player.lastZ), player.clientVelocity.clone());
        } else {
            setbackConfirmTicksAgo = 0; // Pending setback
        }
    }

    public void executeForceResync() {
        if (player.gamemode == GameMode.SPECTATOR || player.disableGrim) return; // We don't care about spectators, they don't flag
        blockOffsets = true;
        if (safeTeleportPosition == null) return; // Player hasn't spawned yet
        blockMovementsUntilResync(safeTeleportPosition.position);
    }

    public boolean executeViolationSetback() {
        if (isExempt()) return false;
        blockMovementsUntilResync(safeTeleportPosition.position);
        return true;
    }

    public boolean executeNonSimulatingSetback() {
        if (isExempt()) return false;
        blockMovementsUntilResync(safeTeleportPosition.position, false, false);
        return true;
    }

    private boolean isExempt() {
        // Not exempting spectators here because timer check for spectators is actually valid.
        // Player hasn't spawned yet
        if (safeTeleportPosition == null) return true;
        // Setbacks aren't allowed
        if (player.disableGrim) return true;
        // Player has permission to cheat, permission not given to OP by default.
        if (player.bukkitPlayer != null && player.bukkitPlayer.hasPermission("grim.nosetback")) return true;
        return false;
    }

    public void blockMovementsUntilResync(Vector3d position) {
        blockMovementsUntilResync(position, false);
    }

    public void blockMovementsUntilResync(Vector3d position, boolean force) {
        blockMovementsUntilResync(position, force, true);
    }

    public void blockMovementsUntilResync(Vector3d position, boolean force, boolean simulateNextTickPosition) {
        if (requiredSetBack == null || player.bukkitPlayer == null)
            return; // Player hasn't gotten a single teleport yet.
        requiredSetBack.setPlugin(false); // The player has illegal movement, block from vanilla ac override
        if (!force && isPendingSetback()) return; // Don't spam setbacks

        // Only let us full resync once every five seconds to prevent unneeded bukkit load
        if (System.currentTimeMillis() - lastWorldResync > 5 * 1000) {
            ResyncWorldUtil.resyncPositions(player, player.boundingBox.copy().expand(1));
            lastWorldResync = System.currentTimeMillis();
        }

        Vector clientVel = player.predictedVelocity.vector.clone();
        SimpleCollisionBox oldBB = player.boundingBox;
        player.boundingBox = GetBoundingBox.getPlayerBoundingBox(player, position.getX(), position.getY(), position.getZ());

        // Apply knockback before applying explosions to stop people from ignoring explosions
        if (player.firstBreadKB != null && player.likelyKB == null) {
            clientVel = player.firstBreadKB.vector.clone();
        } else if (player.likelyKB != null) {
            clientVel = player.likelyKB.vector.clone();
        }

        // First bread explosion needs to be applied
        // Only apply if likely is not a valid explosion
        // Don't apply this twice
        if (player.firstBreadExplosion != null && player.likelyExplosions == null) {
            clientVel.add(player.firstBreadExplosion.vector);
        } else if (player.likelyExplosions != null) { // Likely explosion gets priority
            clientVel.add(player.likelyExplosions.vector);
        }

        // Prevent double velocity/explosions
        if (player.likelyExplosions != null) {
            player.likelyExplosions.hasSetbackForThis = true;
        }
        if (player.firstBreadExplosion != null) {
            player.firstBreadExplosion.hasSetbackForThis = true;
        }
        if (player.likelyKB != null) {
            player.likelyKB.hasSetbackForThis = true;
        }
        if (player.firstBreadKB != null) {
            player.firstBreadKB.hasSetbackForThis = true;
        }

        // Mini prediction engine - simulate collisions
        if (simulateNextTickPosition) {
            Vector collide = Collisions.collide(player, clientVel.getX(), clientVel.getY(), clientVel.getZ());

            // 1.8 players need the collision epsilon to not phase into blocks when being setback
            // Due to simulation, this will not allow a flight bypass by sending a billion invalid movements
            position = position.add(collide.getX(), collide.getY() + SimpleCollisionBox.COLLISION_EPSILON, collide.getZ());

            // TODO: Add support for elytra, water, lava, and end of ticks
            if (player.wasTouchingWater) {
                PredictionEngineWater.staticVectorEndOfTick(player, clientVel, 0.8F, player.gravity, true);
            } else if (!player.isGliding) { // Gliding doesn't have friction, we handle it differently
                PredictionEngineNormal.staticVectorEndOfTick(player, clientVel); // Lava and normal movement
            }
        }

        player.boundingBox = oldBB; // reset back to the new bounding box

        if (!hasAcceptedSpawnTeleport) clientVel = null; // if the player hasn't spawned... don't force kb

        // Don't let people get new velocities on demand
        if (player.checkManager.getKnockbackHandler().isPendingKb() ||
                player.checkManager.getExplosionHandler().isPendingExplosion()) {
            clientVel = null;
        }

        SetBackData data = new SetBackData(position, player.xRot, player.yRot, clientVel, null, false);
        requiredSetBack = data;

        isSendingSetback = true;

        try {
            // Player is in a vehicle
            Integer vehicleId = player.compensatedEntities.serverPlayerVehicle;
            if (player.compensatedEntities.serverPlayerVehicle != null) {
                if (PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_9)) {
                    player.user.sendPacket(new WrapperPlayServerSetPassengers(vehicleId, new int[2]));
                } else {
                    player.user.sendPacket(new WrapperPlayServerAttachEntity(vehicleId, -1, false));
                }

                // Stop the player from being able to teleport vehicles and simply re-enter them to continue
                player.user.sendPacket(new WrapperPlayServerEntityTeleport(vehicleId, position, player.xRot % 360, 0, false));

                // Make sure bukkit also knows the player got teleported out of their vehicle, can't do this async
                Bukkit.getScheduler().runTask(GrimAPI.INSTANCE.getPlugin(), () -> {
                    Entity vehicle = player.bukkitPlayer.getVehicle();
                    if (vehicle != null) {
                        vehicle.eject();
                    }
                });
            }

            player.sendTransaction();
            double y = position.getY();
            if (PacketEvents.getAPI().getServerManager().getVersion().isOlderThanOrEquals(ServerVersion.V_1_7_10)) {
                y += 1.62; // 1.7 teleport offset if grim ever supports 1.7 again
            }
            addSentTeleport(new Vector3d(position.getX(), y, position.getZ()), player.lastTransactionSent.get(), false);
            // Send after tracking to fix race condition
            PacketEvents.getAPI().getProtocolManager().sendPacketSilently(player.user.getChannel(), new WrapperPlayServerPlayerPositionAndLook(position.getX(), position.getY(), position.getZ(), 0, 0, (byte) 0b11000, new Random().nextInt(), false));
            player.sendTransaction();

            if (data.getVelocity() != null) {
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
        int lastTransaction = player.lastTransactionReceived.get();
        TeleportAcceptData teleportData = new TeleportAcceptData();

        while (true) {
            Pair<Integer, Location> teleportPos = teleports.peek();
            if (teleportPos == null) break;

            Location location = teleportPos.getSecond();

            if (lastTransaction < teleportPos.getFirst()) {
                break;
            }

            // There seems to be a version difference in teleports past 30 million... just clamp the vector
            Vector3d clamped = VectorUtils.clampVector(location.getPosition());

            boolean closeEnoughY = Math.abs(clamped.getY() - y) < 1e-7; // 1.7 rounding
            if (clamped.getX() == x && closeEnoughY && clamped.getZ() == z) {
                teleports.poll();
                hasAcceptedSpawnTeleport = true;

                SetBackData setBack = requiredSetBack;

                // Player has accepted their setback!
                if (setBack != null && requiredSetBack.getPosition().getX() == teleportPos.getSecond().getX()
                        && Math.abs(requiredSetBack.getPosition().getY() - teleportPos.getSecond().getY()) < 1e-7
                        && requiredSetBack.getPosition().getZ() == teleportPos.getSecond().getZ()) {
                    if (!player.compensatedEntities.getSelf().inVehicle()) {
                        player.lastOnGround = player.packetStateData.packetPlayerOnGround;
                    }
                    teleportData.setSetback(requiredSetBack);
                    setBack.setComplete(true);
                }

                teleportData.setTeleport(true);
            } else if (lastTransaction > teleportPos.getFirst() + 1) {
                teleports.poll();
                continue;
            }

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
     * @return Whether the current setback has been completed, or the player hasn't spawned yet
     */
    public boolean shouldBlockMovement() {
        // We must block movements if we were the one to cause the teleport
        // Else the vanilla anticheat will override our teleports causing a funny fly exploit
        return insideUnloadedChunk() || (requiredSetBack != null && !requiredSetBack.isComplete() && !requiredSetBack.isPlugin());
    }

    private boolean isPendingSetback() {
        return requiredSetBack != null && !requiredSetBack.isComplete();
    }

    /**
     * When the player is inside an unloaded chunk, they simply fall through the void which shouldn't be checked
     *
     * @return Whether the player has loaded the chunk or not
     */
    public boolean insideUnloadedChunk() {
        int transaction = player.lastTransactionReceived.get();
        double playerX = player.x;
        double playerZ = player.z;

        Column column = player.compensatedWorld.getChunk(GrimMath.floor(playerX) >> 4, GrimMath.floor(playerZ) >> 4);

        // The player is in an unloaded chunk
        return !player.disableGrim && (column == null || column.transaction > transaction ||
                // The player hasn't loaded past the DOWNLOADING TERRAIN screen
                !player.getSetbackTeleportUtil().hasAcceptedSpawnTeleport);
    }

    /**
     * @return The current data for the setback, regardless of whether it is complete or not
     */
    public SetBackData getRequiredSetBack() {
        return requiredSetBack;
    }

    /**
     * @param position A safe setback location
     */
    public void setSafeSetbackLocation(Vector3d position) {
        this.safeTeleportPosition = new SetbackLocationVelocity(position);
    }

    /**
     * The netty thread is about to send a teleport to the player, should we allow it?
     * <p>
     * Bukkit, due to incompetence, doesn't call the teleport event for all teleports...
     * This means we have to discard teleports from the vanilla anticheat, as otherwise
     * it would allow the player to bypass our own setbacks
     */
    public void addSentTeleport(Vector3d position, int transaction, boolean plugin) {
        requiredSetBack = new SetBackData(position, player.xRot, player.yRot, null, null, plugin);
        teleports.add(new Pair<>(transaction, new Location(position, player.xRot, player.yRot)));
        setSafeSetbackLocation(position);
    }
}
