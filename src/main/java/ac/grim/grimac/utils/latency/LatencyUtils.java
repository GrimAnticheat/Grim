package ac.grim.grimac.utils.latency;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class LatencyUtils {
    public static boolean getBestValue(ConcurrentHashMap<Integer, Boolean> hashMap, int lastTransactionReceived) {
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

        int finalBestKey = bestKey;
        hashMap.keySet().removeIf(value -> value < finalBestKey);

        return bestValue;
    }
}
