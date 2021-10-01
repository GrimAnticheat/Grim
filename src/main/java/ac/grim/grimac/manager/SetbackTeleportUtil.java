package ac.grim.grimac.manager;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.checks.type.PostPredictionCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.LogUtil;
import ac.grim.grimac.utils.anticheat.update.PredictionComplete;
import ac.grim.grimac.utils.chunks.Column;
import ac.grim.grimac.utils.data.SetBackData;
import ac.grim.grimac.utils.data.TeleportAcceptData;
import ac.grim.grimac.utils.math.GrimMath;
import io.github.retrooper.packetevents.utils.pair.Pair;
import io.github.retrooper.packetevents.utils.vector.Vector3d;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.util.Vector;

import java.util.concurrent.ConcurrentLinkedQueue;

public class SetbackTeleportUtil extends PostPredictionCheck {
    // Sync to NETTY (Why does the bukkit thread have to modify this, can we avoid it?)
    // I think it should be safe enough because the worst that can happen is we overwrite another plugin teleport
    //
    // This is required because the required setback position is not sync to bukkit, and we must avoid
    // setting the player back to a position where they were cheating
    public boolean hasAcceptedSetbackPosition = true;
    // Sync to netty
    // Also safe from corruption from the vanilla anticheat!
    final ConcurrentLinkedQueue<Pair<Integer, Location>> teleports = new ConcurrentLinkedQueue<>();
    // Was there a ghost block that forces us to block offsets until the player accepts their teleport?
    public boolean blockOffsets = false;
    // Sync to netty, a player MUST accept a teleport to spawn into the world
    public int acceptedTeleports = 0;
    // This handles not overriding another plugin's teleport
    // Safe from corruption from the vanilla anticheat!
    //
    // Sync to BUKKIT, referenced by only bukkit!  Don't overwrite another plugin's teleport
    // Null means the player hasn't spawned yet because the bukkit API is very inconsistent!
    public Location currentTargetTeleport;

    // This required setback data is sync to the BUKKIT MAIN THREAD (!)
    SetBackData requiredSetBack = null;
    // Prevent the player from getting into a limbo state if the last teleport got blocked
    public Location currentBukkitTarget;
    // Sync to the anticheat thread
    // The anticheat thread MUST be the only thread that controls these safe setback position variables
    // This one prevents us from pulling positions the tick before a setback
    boolean wasLastMovementSafe = true;
    // Sync to anything, worst that can happen is sending an extra world update (which won't be noticed)
    long lastWorldResync = 0;
    // Sync to anticheat thread
    Vector lastMovementVel = new Vector();
    // Generally safe teleport position (ANTICHEAT THREAD!)
    // Determined by the latest movement prediction
    // Positions until the player's current setback is accepted cannot become safe teleport positions
    SetbackLocationVelocity safeTeleportPosition;

    public SetbackTeleportUtil(GrimPlayer player) {
        super(player);
    }

    /**
     * Generates safe setback locations by looking at the current prediction
     */
    public void onPredictionComplete(final PredictionComplete predictionComplete) {
        // We must first check if the player has accepted their setback
        // If the setback isn't complete, then this position is illegitimate
        if (predictionComplete.getData().acceptedSetback) {
            // If there is a new pending setback, don't desync from the netty thread
            if (!requiredSetBack.isComplete()) return;
            // The player did indeed accept the setback, and there are no new setbacks past now!
            hasAcceptedSetbackPosition = true;
            safeTeleportPosition = new SetbackLocationVelocity(player.playerWorld, new Vector3d(player.x, player.y, player.z));
        } else if (hasAcceptedSetbackPosition) {
            safeTeleportPosition = new SetbackLocationVelocity(player.playerWorld, new Vector3d(player.lastX, player.lastY, player.lastZ), lastMovementVel);

            // Do NOT accept teleports as valid setback positions if the player has a current setback
            // This is due to players being able to trigger new teleports with the vanilla anticheat
            if (predictionComplete.getData().isJustTeleported) {
                // Avoid setting the player back to positions before this teleport
                safeTeleportPosition = new SetbackLocationVelocity(player.playerWorld, new Vector3d(player.x, player.y, player.z));
            }
        }
        wasLastMovementSafe = hasAcceptedSetbackPosition;
        lastMovementVel = player.clientVelocity;
    }

    public void executeForceResync() {
        blockOffsets = true;
        executeSetback();
    }

    public void confirmPredictionTeleport() {
        blockOffsets = false;
    }

    public void executeSetback() {
        Vector setbackVel = new Vector();

        if (player.firstBreadKB != null) {
            setbackVel = player.firstBreadKB.vector;
        }

        if (player.likelyKB != null) {
            setbackVel = player.likelyKB.vector;
        }

        if (player.firstBreadExplosion != null) {
            setbackVel.add(player.firstBreadExplosion.vector);
        }

        if (player.likelyExplosions != null) {
            setbackVel.add(player.likelyExplosions.vector);
        }

        SetbackLocationVelocity data = safeTeleportPosition;

        // If the player has no explosion/velocity, set them back to the data's stored velocity
        if (setbackVel.equals(new Vector())) setbackVel = data.velocity;

        if (requiredSetBack != null) {
            LogUtil.info("if this setback was too far, report this debug for setting back " + player.bukkitPlayer.getName() + " from " + player.x + " " + player.y + " " + player.z + " to "
                    + data.position + " dvl " + data.velocity + " has " + hasAcceptedSetbackPosition + " acc "
                    + acceptedTeleports + " com " + requiredSetBack.isComplete() + " trn " + requiredSetBack.getTrans() + " pos "
                    + requiredSetBack.getPosition() + " vel " + requiredSetBack.getVelocity() + " sfe " + wasLastMovementSafe + " lvl "
                    + lastMovementVel);
        }

        blockMovementsUntilResync(data.position,
                player.packetStateData.packetPlayerXRot, player.packetStateData.packetPlayerYRot,
                setbackVel, player.vehicle, false);
    }

    private void blockMovementsUntilResync(Location position, float xRot, float yRot, Vector velocity, Integer vehicle, boolean force) {
        // Don't teleport cross world, it will break more than it fixes.
        if (position.getWorld() != player.bukkitPlayer.getWorld()) return;

        SetBackData setBack = requiredSetBack;
        if (force || setBack == null || setBack.isComplete()) {
            // Deal with ghost blocks near the player (from anticheat/netty thread)
            // Only let us full resync once every two seconds to prevent unneeded netty load
            if (System.nanoTime() - lastWorldResync > 2e-9) {
                player.getResyncWorldUtil().resyncPositions(player, player.boundingBox.copy().expand(1), false);
                lastWorldResync = System.nanoTime();
            }

            hasAcceptedSetbackPosition = false;

            Bukkit.getScheduler().runTask(GrimAPI.INSTANCE.getPlugin(), () -> {
                if (!teleports.isEmpty()) return; // Do we already have teleport that is being sent to the player?

                requiredSetBack = new SetBackData(position, xRot, yRot, velocity, vehicle, player.lastTransactionSent.get());

                // Vanilla is terrible at handling regular player teleports when in vehicle, eject to avoid issues
                Entity playerVehicle = player.bukkitPlayer.getVehicle();
                player.bukkitPlayer.eject();

                // Mojang is terrible and tied together:
                // on fire, is crouching, riding, sprinting, swimming, invisible, has glowing effect, fall flying
                // into one byte!  At least this gives me a very easy method to resync metadata on all server versions
                boolean isSneaking = player.bukkitPlayer.isSneaking();
                player.bukkitPlayer.setSneaking(!isSneaking);
                player.bukkitPlayer.setSneaking(isSneaking);

                if (playerVehicle != null) {
                    // Stop the player from being able to teleport vehicles and simply re-enter them to continue
                    playerVehicle.teleport(new Location(position.getWorld(), position.getX(), position.getY(), position.getZ(), playerVehicle.getLocation().getYaw(), playerVehicle.getLocation().getPitch()));
                }

                player.bukkitPlayer.teleport(new Location(position.getWorld(), position.getX(), position.getY(), position.getZ(), xRot, yRot));
                player.bukkitPlayer.setVelocity(vehicle == null ? velocity : new Vector());
            });
        }
    }

    public void tryResendExpiredSetback() {
        SetBackData setBack = requiredSetBack;

        if (setBack != null && !setBack.isComplete() && setBack.getTrans() + 2 < player.packetStateData.packetLastTransactionReceived.get()) {
            resendSetback(true);
        }
    }

    /**
     * @param force - Should we setback the player to the last position regardless of if they have
     *              accepted the teleport, useful for overriding vanilla anticheat teleports.
     */
    public void resendSetback(boolean force) {
        SetBackData setBack = requiredSetBack;

        if (setBack != null && (!setBack.isComplete() || force)) {
            blockMovementsUntilResync(setBack.getPosition(), setBack.getXRot(), setBack.getYRot(), setBack.getVelocity(), setBack.getVehicle(), force);
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
        int lastTransaction = player.packetStateData.packetLastTransactionReceived.get();
        TeleportAcceptData teleportData = new TeleportAcceptData();

        while (true) {
            Pair<Integer, Location> teleportPos = teleports.peek();
            if (teleportPos == null) break;

            Location position = teleportPos.getSecond();

            if (lastTransaction < teleportPos.getFirst()) {
                break;
            }

            // Don't use prediction data because it doesn't allow positions past 29,999,999 blocks
            if (position.getX() == x && position.getY() == y && position.getZ() == z) {
                teleports.poll();
                acceptedTeleports++;

                SetBackData setBack = requiredSetBack;

                // Player has accepted their setback!
                if (setBack != null && requiredSetBack.getPosition().getX() == teleportPos.getSecond().getX()
                        && requiredSetBack.getPosition().getY() == teleportPos.getSecond().getY()
                        && requiredSetBack.getPosition().getZ() == teleportPos.getSecond().getZ()) {
                    teleportData.setSetback(true);
                    setBack.setComplete(true);
                }

                teleportData.setTeleport(true);
            } else if (lastTransaction > teleportPos.getFirst() + 2) { // Give a transaction or two lenience as we track transactions from bukkit
                teleports.poll();

                // Ignored teleport, teleport the player as a plugin would!
                position.setPitch(player.packetStateData.packetPlayerYRot);
                position.setYaw(player.packetStateData.packetPlayerXRot);
                Bukkit.getScheduler().runTask(GrimAPI.INSTANCE.getPlugin(), () -> player.bukkitPlayer.teleport(position));

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
        int lastTransaction = player.packetStateData.packetLastTransactionReceived.get();

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
            } else if (lastTransaction > teleportPos.getFirst() + 2) {
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

    public boolean insideUnloadedChunk() {
        int transaction = player.packetStateData.packetLastTransactionReceived.get();
        double playerX = player.packetStateData.packetPosition.getX();
        double playerZ = player.packetStateData.packetPosition.getZ();

        Column column = player.compensatedWorld.getChunk(GrimMath.floor(playerX) >> 4, GrimMath.floor(playerZ) >> 4);

        // The player is in an unloaded chunk
        return column == null || column.transaction > transaction &&
                // The player hasn't loaded past the DOWNLOADING TERRAIN screen
                player.getSetbackTeleportUtil().acceptedTeleports == 0;
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
        currentTargetTeleport = position;
        hasAcceptedSetbackPosition = false;
        requiredSetBack = new SetBackData(position, player.packetStateData.packetPlayerXRot, player.packetStateData.packetPlayerYRot, new Vector(), null, player.lastTransactionSent.get());
        safeTeleportPosition = new SetbackLocationVelocity(position);
    }

    /**
     * @param position A safe setback location
     */
    public void setSafeSetbackLocation(World world, Vector3d position) {
        this.safeTeleportPosition = new SetbackLocationVelocity(world, position);
    }

    public boolean addSentTeleport(Location position, int transaction) {
        currentBukkitTarget = position;
        if (currentTargetTeleport == null) { // Occurs for the first teleport on join
            currentTargetTeleport = new Location(player.bukkitPlayer.getWorld(), player.x, player.y, player.z);
        } else if (position.getX() != currentTargetTeleport.getX() || position.getY() != currentTargetTeleport.getY() || position.getZ() != currentTargetTeleport.getZ()) {
            return true; // Vanilla anticheat sent this (or a plugin that managed to desync us)
        }

        teleports.add(new Pair<>(transaction, new Location(player.bukkitPlayer.getWorld(), position.getX(), position.getY(), position.getZ())));
        return false;
    }
}

class SetbackLocationVelocity {
    Location position;
    Vector velocity = new Vector();

    public SetbackLocationVelocity(Location location) {
        this.position = location;
    }

    public SetbackLocationVelocity(World world, Vector3d vector3d) {
        this.position = new Location(world, vector3d.getX(), vector3d.getY(), vector3d.getZ());
    }

    public SetbackLocationVelocity(World world, Vector3d position, Vector velocity) {
        this.position = new Location(world, position.getX(), position.getY(), position.getZ());
        this.velocity = velocity;
    }
}
