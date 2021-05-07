package ac.grim.grimac.utils.latency;

import ac.grim.grimac.player.GrimPlayer;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CompensatedFlying {
    ConcurrentHashMap<Integer, Boolean> lagCompensatedFlyingMap = new ConcurrentHashMap<>();
    ConcurrentHashMap<Integer, Boolean> forcedFlyMap = new ConcurrentHashMap<>();
    boolean canPlayerFly;
    boolean isFlying;
    GrimPlayer player;

    public CompensatedFlying(GrimPlayer player) {
        this.player = player;
        this.canPlayerFly = player.bukkitPlayer.getAllowFlight();
        this.isFlying = player.bukkitPlayer.isFlying();
    }

    public void setCanPlayerFly(boolean canFly) {
        lagCompensatedFlyingMap.put(player.lastTransactionSent.get(), canFly);
    }

    public void setServerForcedPlayerFly(boolean fly) {
        forcedFlyMap.put(player.lastTransactionSent.get(), fly);
    }

    public boolean isPlayerFlying() {
        int lastTransactionReceived = player.lastTransactionReceived;

        boolean isFly = canPlayerFly;
        int bestKey = 0;

        Iterator<Map.Entry<Integer, Boolean>> iterator = lagCompensatedFlyingMap.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, Boolean> flightStatus = iterator.next();

            if (flightStatus.getKey() > lastTransactionReceived) continue;

            if (flightStatus.getKey() < bestKey) {
                iterator.remove();
                continue;
            }

            bestKey = flightStatus.getKey();
            isFly = flightStatus.getValue();

            iterator.remove();
        }

        canPlayerFly = isFly;

        return isFly;
    }

    public boolean getCanPlayerFlyLagCompensated() {
        int lastTransactionReceived = player.lastTransactionReceived;

        boolean canFly = canPlayerFly;
        int bestKey = 0;

        Iterator<Map.Entry<Integer, Boolean>> iterator = lagCompensatedFlyingMap.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, Boolean> flightStatus = iterator.next();

            if (flightStatus.getKey() > lastTransactionReceived) continue;

            if (flightStatus.getKey() < bestKey) {
                iterator.remove();
                continue;
            }

            bestKey = flightStatus.getKey();
            canFly = flightStatus.getValue();

            iterator.remove();
        }

        canPlayerFly = canFly;

        return canFly;
    }
}
