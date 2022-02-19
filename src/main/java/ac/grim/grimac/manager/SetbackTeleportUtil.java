package ac.grim.grimac.manager;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.checks.type.PostPredictionCheck;
import ac.grim.grimac.events.packets.patch.ResyncWorldUtil;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.PredictionComplete;
import ac.grim.grimac.utils.chunks.Column;
import ac.grim.grimac.utils.data.Pair;
import ac.grim.grimac.utils.data.SetBackData;
import ac.grim.grimac.utils.data.TeleportAcceptData;
import ac.grim.grimac.utils.math.GrimMath;
import ac.grim.grimac.utils.math.VectorUtils;
import com.github.retrooper.packetevents.util.Vector3d;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.util.Vector;

import java.util.concurrent.ConcurrentLinkedQueue;

public class SetbackTeleportUtil extends PostPredictionCheck {
    // Sync to netty
    final ConcurrentLinkedQueue<Pair<Integer, Location>> teleports = new ConcurrentLinkedQueue<>();
    public int bukkitTeleportsProcessed = 0;
    // Sync to NETTY (Why does the bukkit thread have to modify this, can we avoid it?)
    // I think it should be safe enough because the worst that can happen is we overwrite another plugin teleport
    //
    // This is required because the required setback position is not sync to bukkit, and we must avoid
    // setting the player back to a position where they were cheating
    public boolean hasAcceptedSetbackPosition = true;
    // Sync to netty, a player MUST accept a teleport to spawn into the world
    public boolean hasAcceptedSpawnTeleport = false;
    // Was there a ghost block that forces us to block offsets until the player accepts their teleport?
    public boolean blockOffsets = false;
    public boolean blockPredictions = false;
    // This required setback data is sync to the BUKKIT MAIN THREAD (!)
    SetBackData requiredSetBack = null;
    // Sync to the anticheat thread
    // The anticheat thread MUST be the only thread that controls these safe setback position variables
    // This one prevents us from pulling positions the tick before a setback
    boolean wasLastMovementSafe = true;
    public int safeMovementTicks = 0;
    // Sync to anything, worst that can happen is sending an extra world update (which won't be noticed)
    long lastWorldResync = 0;
    // Generally safe teleport position (ANTICHEAT THREAD!)
    // Determined by the latest movement prediction
    // Positions until the player's current setback is accepted cannot become safe teleport positions
    SetbackLocationVelocity safeTeleportPosition;

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
        // Desync is fixed
        if (predictionComplete.getData().isTeleport()) {
            blockOffsets = false;
            blockPredictions = false;
        }

        // We must first check if the player has accepted their setback
        // If the setback isn't complete, then this position is illegitimate
        if (predictionComplete.getData().getSetback() != null) {
            // The player did indeed accept the setback, and there are no new setbacks past now!
            hasAcceptedSetbackPosition = true;
            safeMovementTicks = 0;
            safeTeleportPosition = new SetbackLocationVelocity(player.playerWorld, new Vector3d(player.x, player.y, player.z));
        } else if (hasAcceptedSetbackPosition) {
            if (safeMovementTicks++ > 10) { // You must be legit for at least 500 ms before getting a new setback pos...
                safeTeleportPosition = new SetbackLocationVelocity(player.playerWorld, new Vector3d(player.lastX, player.lastY, player.lastZ));
            }

            // We checked for a new pending setback above
            if (predictionComplete.getData().isTeleport()) {
                // Avoid setting the player back to positions before this teleport
                safeTeleportPosition = new SetbackLocationVelocity(player.playerWorld, new Vector3d(player.x, player.y, player.z));
            }
        } else {
            safeMovementTicks = 0;
        }

        // This can be simplified, but I'm afraid of bypasses and don't want to change the code as I know this works.
        wasLastMovementSafe = hasAcceptedSetbackPosition;
    }

    public void executeForceResync() {
        blockOffsets = true;
        executeSetback();
    }

    public void executeSetback() {
        if (safeTeleportPosition == null) return; // Player hasn't spawned yet
        blockMovementsUntilResync(safeTeleportPosition.position);
    }

    private void blockMovementsUntilResync(Location position) {
        // Don't teleport cross world, it will break more than it fixes.
        if (player.bukkitPlayer != null && position.getWorld() != player.bukkitPlayer.getWorld()) return;

        // Only let us full resync once every ten seconds to prevent unneeded bukkit load
        if (System.nanoTime() - lastWorldResync > 10e-9) {
            ResyncWorldUtil.resyncPositions(player, player.boundingBox.copy().expand(1));
            lastWorldResync = System.nanoTime();
        }

        int bukkitTeleports = bukkitTeleportsProcessed;

        Bukkit.getScheduler().runTask(GrimAPI.INSTANCE.getPlugin(), () -> {
            // First one - if another plugin has sent a new teleport, don't override it
            // (Fixes race condition at 0 latency conditions with teleports being immediately accepted)
            // Second one - if there is a pending teleport, don't override it
            // (Fixes race condition between bukkit and netty, we are sync to bukkit here)
            if (bukkitTeleportsProcessed > bukkitTeleports || isPendingTeleport() || player.bukkitPlayer == null)
                return;

            // Vanilla is terrible at handling regular player teleports when in vehicle, eject to avoid issues
            Entity playerVehicle = player.bukkitPlayer.getVehicle();

            if (playerVehicle != null) {
                playerVehicle.eject();
                // Stop the player from being able to teleport vehicles and simply re-enter them to continue
                Location vehicleLocation = playerVehicle.getLocation();
                playerVehicle.teleport(new Location(position.getWorld(), position.getX(), position.getY(), position.getZ(), vehicleLocation.getYaw() % 360, vehicleLocation.getPitch() % 360));
            }

            player.bukkitPlayer.teleport(new Location(position.getWorld(), position.getX(), position.getY(), position.getZ(), player.xRot % 360, player.yRot % 360));

            // Override essentials giving player invulnerability on teleport
            player.setVulnerable();
        });
    }

    public void resendSetback() {
        SetBackData setBack = requiredSetBack;
        blockMovementsUntilResync(setBack.getPosition());
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

            Location position = teleportPos.getSecond();

            if (lastTransaction < teleportPos.getFirst()) {
                break;
            }

            // There seems to be a version difference in teleports past 30 million... just clamp the vector
            Vector3d clamped = VectorUtils.clampVector(new Vector3d(position.getX(), position.getY(), position.getZ()));

            boolean closeEnoughY = Math.abs(clamped.getY() - y) < 1e-7; // 1.7 rounding
            if (clamped.getX() == x && closeEnoughY && clamped.getZ() == z) {
                teleports.poll();
                hasAcceptedSpawnTeleport = true;

                SetBackData setBack = requiredSetBack;

                // Player has accepted their setback!
                if (setBack != null && requiredSetBack.getPosition().getX() == teleportPos.getSecond().getX()
                        && Math.abs(requiredSetBack.getPosition().getY() - teleportPos.getSecond().getY()) < 1e-7
                        && requiredSetBack.getPosition().getZ() == teleportPos.getSecond().getZ()) {
                    player.lastOnGround = player.packetStateData.packetPlayerOnGround;
                    teleportData.setSetback(requiredSetBack);
                    setBack.setComplete(true);
                }

                teleportData.setTeleport(true);
            } else if (lastTransaction > teleportPos.getFirst() + 1) {
                teleports.poll();
                if (teleports.isEmpty()) {
                    resendSetback();
                }
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
        return isPendingSetback() || insideUnloadedChunk();
    }

    private boolean isPendingSetback() {
        SetBackData setBackData = requiredSetBack;
        return setBackData != null && !setBackData.isComplete();
    }

    public boolean isPendingTeleport() {
        return !teleports.isEmpty();
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
        return column == null || column.transaction > transaction ||
                // The player hasn't loaded past the DOWNLOADING TERRAIN screen
                !player.getSetbackTeleportUtil().hasAcceptedSpawnTeleport;
    }

    /**
     * @return The current data for the setback, regardless of whether it is complete or not
     */
    public SetBackData getRequiredSetBack() {
        return requiredSetBack;
    }

    /**
     * This method is unsafe to call outside the bukkit thread
     * This method sets a plugin teleport at this location
     *
     * @param position Position of the teleport
     */
    public void setTargetTeleport(Location position) {
        bukkitTeleportsProcessed++;

        hasAcceptedSetbackPosition = false;
        requiredSetBack = new SetBackData(position, player.xRot, player.yRot, new Vector(), null, player.lastTransactionSent.get(), true);
        safeTeleportPosition = new SetbackLocationVelocity(position.getWorld(), new Vector3d(position.getX(), position.getY(), position.getZ()));
    }

    /**
     * @param position A safe setback location
     */
    public void setSafeSetbackLocation(World world, Vector3d position) {
        this.safeTeleportPosition = new SetbackLocationVelocity(world, position);
    }

    /**
     * The netty thread is about to send a teleport to the player, should we allow it?
     * <p>
     * Bukkit, due to incompetence, doesn't call the teleport event for all teleports...
     * This means we have to discard teleports from the vanilla anticheat, as otherwise
     * it would allow the player to bypass our own setbacks
     */
    public void addSentTeleport(Location position, int transaction) {
        teleports.add(new Pair<>(transaction, new Location(player.bukkitPlayer != null ? player.bukkitPlayer.getWorld() : null, position.getX(), position.getY(), position.getZ())));
    }
}

class SetbackLocationVelocity {
    Location position;

    public SetbackLocationVelocity(World world, Vector3d vector3d) {
        this.position = new Location(world, vector3d.getX(), vector3d.getY(), vector3d.getZ());
    }
}
