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

import java.util.concurrent.atomic.AtomicBoolean;

public class TeleportUtil {
    GrimPlayer player;
    SetBackData requiredSetBack;
    AtomicBoolean hasSetBackTask = new AtomicBoolean(false);
    int ignoreTransBeforeThis = 0;

    public TeleportUtil(GrimPlayer player) {
        this.player = player;
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
            if (position.getX() == x && position.getY() == y && position.getZ() == z) {
                player.teleports.poll();

                // Teleports remove the player from their vehicle
                player.vehicle = null;

                // Note the latest teleport accepted
                ignoreTransBeforeThis = lastTransaction;
                // Player has accepted their setback!
                if (hasSetBackTask.get() && requiredSetBack.getPosition().equals(teleportPos.getSecond())) {
                    hasSetBackTask.set(false);
                }

                return true;
            } else if (lastTransaction > teleportPos.getFirst() + 2) {
                player.teleports.poll();
                // Ignored teleport!  We should really do something about this!
                continue;
            }

            break;
        }

        if (hasSetBackTask.get() && requiredSetBack.getTrans() < player.packetStateData.packetLastTransactionReceived.get()) {
            hasSetBackTask.set(false);
            blockMovementsUntilResync(requiredSetBack.getWorld(), requiredSetBack.getPosition(), requiredSetBack.getXRot(), requiredSetBack.getYRot(), requiredSetBack.getVelocity(), requiredSetBack.getVehicle(), player.lastTransactionSent.get());
        }

        return false;
    }

    public void blockMovementsUntilResync(World world, Vector3d position, float xRot, float yRot, Vector velocity, Integer vehicle, int trans) {
        // Don't teleport cross world, it will break more than it fixes.
        if (world != player.bukkitPlayer.getWorld()) return;
        // A teleport has made this point in transaction history irrelevant
        // Meaning:
        // movement - movement - this point in time - movement - movement - teleport
        // or something similar, setting back would be obnoxious.
        if (trans < ignoreTransBeforeThis) return;

        if (hasSetBackTask.compareAndSet(false, true)) {
            requiredSetBack = new SetBackData(world, position, xRot, yRot, velocity, vehicle, trans);

            Bukkit.getScheduler().runTask(GrimAPI.INSTANCE.getPlugin(), () -> {
                // Vanilla is terrible at handling regular player teleports when in vehicle, eject to avoid issues
                player.bukkitPlayer.eject();
                player.bukkitPlayer.teleport(new Location(world, position.getX(), position.getY(), position.getZ(), xRot, yRot));
                player.bukkitPlayer.setVelocity(vehicle == null ? velocity : new Vector());
            });
        }
    }

    public boolean checkVehicleTeleportQueue(double x, double y, double z) {
        int lastTransaction = player.packetStateData.packetLastTransactionReceived.get();

        if (hasSetBackTask.get() && requiredSetBack.getTrans() < player.packetStateData.packetLastTransactionReceived.get()) {
            hasSetBackTask.set(false);
            blockMovementsUntilResync(requiredSetBack.getWorld(), requiredSetBack.getPosition(), requiredSetBack.getXRot(), requiredSetBack.getYRot(), requiredSetBack.getVelocity(), requiredSetBack.getVehicle(), player.lastTransactionSent.get());
        }

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
        return hasSetBackTask.get();
    }
}
