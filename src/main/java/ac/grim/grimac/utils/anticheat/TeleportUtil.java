package ac.grim.grimac.utils.anticheat;

import ac.grim.grimac.player.GrimPlayer;
import io.github.retrooper.packetevents.utils.pair.Pair;
import io.github.retrooper.packetevents.utils.vector.Vector3d;
import lombok.experimental.UtilityClass;

@UtilityClass
public class TeleportUtil {
    public boolean checkTeleportQueue(GrimPlayer player, double x, double y, double z) {
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

    public boolean checkVehicleTeleportQueue(GrimPlayer player, double x, double y, double z) {
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

                // Ignored teleport!  Do something about this!
                continue;
            }

            break;
        }

        return false;
    }
}
