package ac.grim.grimac.utils.latency;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.data.PlayerFlyingData;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CompensatedFlying {
    ConcurrentHashMap<Integer, Boolean> lagCompensatedFlyingMap = new ConcurrentHashMap<>();
    boolean isFlying;
    GrimPlayer player;

    public CompensatedFlying(GrimPlayer player) {
        this.player = player;
        this.isFlying = player.bukkitPlayer.isFlying();
        lagCompensatedFlyingMap.put(0, player.bukkitPlayer.getAllowFlight());
    }

    public void setCanPlayerFly(boolean canFly) {
        lagCompensatedFlyingMap.put(player.lastTransactionSent.get(), canFly);
    }

    public boolean somewhatLagCompensatedIsPlayerFlying() {
        if (!player.bukkitFlying && getCanPlayerFlyLagCompensated(player.lastTransactionReceived + 1)) {
            return player.packetFlyingDanger;
        }

        return player.bukkitPlayer.isFlying();
    }

    public boolean getCanPlayerFlyLagCompensated(int lastTransactionReceived) {
        int bestKey = 0;
        boolean bestValue = false;

        Iterator<Map.Entry<Integer, Boolean>> iterator = lagCompensatedFlyingMap.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, Boolean> flightStatus = iterator.next();

            if (flightStatus.getKey() > lastTransactionReceived) continue;

            if (flightStatus.getKey() < bestKey) {
                iterator.remove();
                continue;
            }

            bestKey = flightStatus.getKey();
            bestValue = flightStatus.getValue();
        }

        return bestValue;
    }

    public void tickUpdates(int minimumTickRequiredToContinue) {
        while (true) {
            PlayerFlyingData flyingData = player.playerFlyingQueue.peek();

            if (flyingData == null) break;
            // The anticheat thread is behind, this event has not occurred yet
            if (flyingData.tick > minimumTickRequiredToContinue) break;
            player.playerFlyingQueue.poll();

            player.bukkitFlying = flyingData.isFlying;
        }
    }
}
