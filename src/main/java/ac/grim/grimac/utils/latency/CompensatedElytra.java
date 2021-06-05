package ac.grim.grimac.utils.latency;

import ac.grim.grimac.player.GrimPlayer;
import io.github.retrooper.packetevents.utils.player.ClientVersion;
import org.bukkit.Bukkit;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CompensatedElytra {
    private final ConcurrentHashMap<Integer, Boolean> lagCompensatedIsGlidingMap = new ConcurrentHashMap<>();
    private final GrimPlayer player;
    public boolean playerToggledElytra = false;

    public CompensatedElytra(GrimPlayer player) {
        this.player = player;
        this.lagCompensatedIsGlidingMap.put((int) Short.MIN_VALUE, player.bukkitPlayer.isGliding());
    }

    public boolean isGlidingLagCompensated(int lastTransaction) {
        return getBestValue(lagCompensatedIsGlidingMap, lastTransaction) && player.getClientVersion().isNewerThanOrEquals(ClientVersion.v_1_9);
    }

    private boolean getBestValue(ConcurrentHashMap<Integer, Boolean> hashMap, int lastTransactionReceived) {
        int bestKey = Integer.MIN_VALUE;
        // This value is always set because one value is always left in the maps
        boolean bestValue = false;

        Iterator<Map.Entry<Integer, Boolean>> iterator = hashMap.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, Boolean> flightStatus = iterator.next();

            Bukkit.broadcastMessage("Status is " + flightStatus.getKey() + " " + flightStatus.getValue());

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

    public void tryAddStatus(int transaction, boolean isGliding) {
        lagCompensatedIsGlidingMap.put(transaction, isGliding);
    }
}
