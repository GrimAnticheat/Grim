package ac.grim.grimac.utils.latency;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.data.FireworkData;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CompensatedFireworks {
    // Be concurrent to support async/multithreaded entity tracker
    ConcurrentHashMap<Integer, FireworkData> lagCompensatedFireworksMap = new ConcurrentHashMap<>();
    boolean canPlayerFly;
    GrimPlayer grimPlayer;

    public CompensatedFireworks(GrimPlayer grimPlayer) {
        this.grimPlayer = grimPlayer;
        this.canPlayerFly = grimPlayer.bukkitPlayer.getAllowFlight();
    }

    public void addNewFirework(int entityID) {
        lagCompensatedFireworksMap.put(entityID, new FireworkData(grimPlayer));
    }

    public void removeFirework(int entityID) {
        FireworkData fireworkData = lagCompensatedFireworksMap.get(entityID);
        if (fireworkData == null) return;

        lagCompensatedFireworksMap.get(entityID).setDestroyed();
    }

    public int getMaxFireworksAppliedPossible() {
        int lastTransactionReceived = grimPlayer.lastTransactionReceived;
        int fireworks = 0;

        Iterator<Map.Entry<Integer, FireworkData>> iterator = lagCompensatedFireworksMap.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, FireworkData> firework = iterator.next();

            if (firework.getValue().destroyTime < lastTransactionReceived + 2) {
                iterator.remove();
                continue;
            }

            fireworks++;
        }

        return fireworks;
    }
}
