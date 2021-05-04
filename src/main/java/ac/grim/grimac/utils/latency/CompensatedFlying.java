package ac.grim.grimac.utils.latency;

import ac.grim.grimac.player.GrimPlayer;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CompensatedFlying {
    ConcurrentHashMap<Integer, Boolean> lagCompensatedFlyingMap = new ConcurrentHashMap<>();
    boolean canPlayerFly;
    GrimPlayer grimPlayer;

    public CompensatedFlying(GrimPlayer grimPlayer) {
        this.grimPlayer = grimPlayer;
        this.canPlayerFly = grimPlayer.bukkitPlayer.getAllowFlight();
    }

    public void setCanPlayerFly(boolean canFly) {
        lagCompensatedFlyingMap.put(grimPlayer.lastTransactionSent.get(), canFly);
    }

    public boolean getCanPlayerFlyLagCompensated() {
        int lastTransactionReceived = grimPlayer.lastTransactionReceived;

        boolean canFly = canPlayerFly;

        Iterator<Map.Entry<Integer, Boolean>> iterator = lagCompensatedFlyingMap.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, Boolean> flightStatus = iterator.next();

            if (flightStatus.getKey() > lastTransactionReceived) continue;
            canFly = flightStatus.getValue();

            iterator.remove();
        }

        canPlayerFly = canFly;

        return canFly;
    }
}
