package ac.grim.grimac.utils.latency;

import ac.grim.grimac.player.GrimPlayer;
import io.github.retrooper.packetevents.utils.player.ClientVersion;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CompensatedElytra {
    public final ConcurrentHashMap<Integer, Boolean> lagCompensatedIsGlidingMap = new ConcurrentHashMap<>();
    private final GrimPlayer player;
    public boolean playerToggledElytra = false;
    public int elytraOnTransaction = 0;

    public CompensatedElytra(GrimPlayer player) {
        this.player = player;
        this.lagCompensatedIsGlidingMap.put((int) Short.MIN_VALUE, player.bukkitPlayer.isGliding());
    }

    public boolean isGlidingLagCompensated(int lastTransaction) {
        return player.getClientVersion().isNewerThanOrEquals(ClientVersion.v_1_9) && getBestValue(lagCompensatedIsGlidingMap, lastTransaction);
    }

    private boolean getBestValue(ConcurrentHashMap<Integer, Boolean> hashMap, int lastTransactionReceived) {
        int bestKey = Integer.MIN_VALUE;
        // This value is always set because one value is always left in the maps
        boolean bestValue = false;

        Iterator<Map.Entry<Integer, Boolean>> iterator = hashMap.entrySet().iterator();
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
}
