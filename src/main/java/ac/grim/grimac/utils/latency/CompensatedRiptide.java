package ac.grim.grimac.utils.latency;

import ac.grim.grimac.player.GrimPlayer;
import io.github.retrooper.packetevents.utils.player.ClientVersion;
import io.github.retrooper.packetevents.utils.server.ServerVersion;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class CompensatedRiptide {
    // We use this class in case the anticheat thread falls behind and the player uses riptide multiple times
    // A bit excessive but might as well do it when everything else supports the anticheat falling behind

    // The integers represent the expiration of the riptide event
    ConcurrentLinkedQueue<Integer> lagCompensatedRiptide = new ConcurrentLinkedQueue<>();
    ConcurrentHashMap<Integer, Boolean> lagCompensatedPose = new ConcurrentHashMap<>();
    GrimPlayer player;

    public CompensatedRiptide(GrimPlayer player) {
        this.player = player;
    }

    public void addRiptide() {
        lagCompensatedRiptide.add(player.packetStateData.packetLastTransactionReceived.get());
    }

    public void handleRemoveRiptide() {
        if (player.predictedVelocity.isTrident())
            lagCompensatedRiptide.poll();
    }

    public void setPose(boolean isPose) {
        lagCompensatedPose.put(player.lastTransactionSent.get(), isPose);
    }

    public boolean getPose(int lastTransaction) {
        return player.getClientVersion().isNewerThanOrEquals(ClientVersion.v_1_13) &&
                ServerVersion.getVersion().isNewerThanOrEquals(ServerVersion.v_1_13) &&
                LatencyUtils.getBestValue(lagCompensatedPose, lastTransaction);
    }

    public boolean getCanRiptide() {
        int lastTransactionReceived = player.lastTransactionReceived;

        if (player.inVehicle)
            return false;

        do {
            Integer integer = lagCompensatedRiptide.peek();

            // There is no possibility for a riptide
            if (integer == null)
                return false;

            // If the movement's transaction is greater than the riptide's transaction
            // Remove the riptide possibility to prevent players from "storing" riptides
            // For example, a client could store riptides to activate in pvp
            if (integer + 20 < lastTransactionReceived) {
                lagCompensatedRiptide.poll();
                continue;
            }

            return true;
        } while (true);
    }
}
