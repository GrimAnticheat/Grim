package ac.grim.grimac.manager;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.data.SetBackData;
import io.github.retrooper.packetevents.utils.pair.Pair;
import io.github.retrooper.packetevents.utils.vector.Vector3d;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.util.Vector;

public class SetbackTeleportUtil {
    GrimPlayer player;
    SetBackData requiredSetBack = null;
    int ignoreTransBeforeThis = 0;
    double teleportEpsilon = 0.5;

    public SetbackTeleportUtil(GrimPlayer player) {
        this.player = player;
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

        if (setbackVel.equals(new Vector())) setbackVel = player.clientVelocity;

        blockMovementsUntilResync(player.playerWorld, new Vector3d(player.lastX, player.lastY, player.lastZ),
                player.packetStateData.packetPlayerXRot, player.packetStateData.packetPlayerYRot, setbackVel,
                player.vehicle, player.lastTransactionReceived, false);
    }

    private void blockMovementsUntilResync(World world, Vector3d position, float xRot, float yRot, Vector velocity, Integer vehicle, int trans, boolean force) {
        // Don't teleport cross world, it will break more than it fixes.
        if (world != player.bukkitPlayer.getWorld()) return;

        SetBackData setBack = requiredSetBack;
        if (force || setBack == null || setBack.isComplete()) {
            requiredSetBack = new SetBackData(world, position, xRot, yRot, velocity, vehicle, trans);

            Bukkit.getScheduler().runTask(GrimAPI.INSTANCE.getPlugin(), () -> {
                // Vanilla is terrible at handling regular player teleports when in vehicle, eject to avoid issues
                player.bukkitPlayer.eject();
                player.bukkitPlayer.teleport(new Location(world, position.getX(), position.getY(), position.getZ(), xRot, yRot));
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

    public void resendSetback(boolean force) {
        SetBackData setBack = requiredSetBack;

        if (setBack != null && (!setBack.isComplete() || force)) {
            blockMovementsUntilResync(setBack.getWorld(), setBack.getPosition(), setBack.getXRot(), setBack.getYRot(), setBack.getVelocity(), setBack.getVehicle(), player.lastTransactionSent.get(), force);
        }
    }

    public boolean checkTeleportQueue(double x, double y, double z) {
        // Support teleports without teleport confirmations
        // If the player is in a vehicle when teleported, they will exit their vehicle
        int lastTransaction = player.packetStateData.packetLastTransactionReceived.get();

        while (true) {
            Pair<Integer, Vector3d> teleportPos = player.teleports.peek();
            if (teleportPos == null) break;

            Vector3d position = teleportPos.getSecond();

            if (lastTransaction < teleportPos.getFirst()) {
                break;
            }

            // Don't use prediction data because it doesn't allow positions past 29,999,999 blocks
            if (Math.abs(position.getX() - x) < teleportEpsilon && Math.abs(position.getY() - y) < teleportEpsilon && Math.abs(position.getZ() - z) < teleportEpsilon) {
                player.teleports.poll();

                // Teleports remove the player from their vehicle
                player.vehicle = null;

                // Note the latest teleport accepted
                ignoreTransBeforeThis = lastTransaction;

                SetBackData setBack = requiredSetBack;

                // Player has accepted their setback!
                if (setBack != null && requiredSetBack.getPosition().equals(teleportPos.getSecond())) {
                    setBack.setComplete(true);
                }

                return true;
            } else if (lastTransaction > teleportPos.getFirst() + 2) {
                player.teleports.poll();
                // Ignored teleport!  We should really do something about this!
                continue;
            }

            break;
        }

        return false;
    }

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

                // Note the latest teleport accepted
                ignoreTransBeforeThis = lastTransaction;

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

    public boolean shouldBlockMovement() {
        SetBackData setBack = requiredSetBack;
        return setBack != null && !setBack.isComplete();
    }

    public SetBackData getRequiredSetBack() {
        return requiredSetBack;
    }
}
