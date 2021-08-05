package ac.grim.grimac.utils.latency;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.data.packetentity.latency.FireworkData;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CompensatedFireworks {
    // Be concurrent to support async/multithreaded entity tracker
    ConcurrentHashMap<Integer, FireworkData> lagCompensatedFireworksMap = new ConcurrentHashMap<>();
    boolean canPlayerFly;
    GrimPlayer player;

    public CompensatedFireworks(GrimPlayer player) {
        this.player = player;
        this.canPlayerFly = player.bukkitPlayer.getAllowFlight();
    }

    public void addNewFirework(int entityID) {
        lagCompensatedFireworksMap.put(entityID, new FireworkData(player));
    }

    public void removeFirework(int entityID) {
        FireworkData fireworkData = lagCompensatedFireworksMap.get(entityID);
        if (fireworkData == null) return;

        lagCompensatedFireworksMap.get(entityID).setDestroyed();
    }

    public int getMaxFireworksAppliedPossible() {
        int fireworks = 0;

        Iterator<Map.Entry<Integer, FireworkData>> iterator = lagCompensatedFireworksMap.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, FireworkData> firework = iterator.next();

            if (firework.getValue().destroyTick < player.movementPackets) {
                iterator.remove();
                continue;
            }


            // If the firework has 100% been destroyed on the client side
            if (firework.getValue().destroyTime < player.lastTransactionReceived) {
                firework.getValue().destroyTick = player.movementPackets;
            }

            // If the firework hasn't applied yet
            if (firework.getValue().creationTime > player.lastTransactionReceived) {
                continue;
            }

            fireworks++;
        }

        return fireworks;
    }
}
