package ac.grim.grimac.utils.latency;

import io.github.retrooper.packetevents.utils.pair.Pair;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

// Okay, this is meant to be a MODERN OOP class!
// Normal grim spaghetti is not allowed here
// Eventually, a ton more transaction related stuff will be transferred to this class
public class LatencyUtils {
    private final ConcurrentLinkedQueue<Pair<Integer, Runnable>> nettySyncTransactionMap = new ConcurrentLinkedQueue<>();

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

    public void addRealTimeTask(int transaction, Runnable runnable) {
        nettySyncTransactionMap.add(new Pair<>(transaction, runnable));
    }

    public void handleNettySyncTransaction(int transaction) {
        tickUpdates(nettySyncTransactionMap, transaction);
    }

    private void tickUpdates(ConcurrentLinkedQueue<Pair<Integer, Runnable>> map, int transaction) {
        Pair<Integer, Runnable> next = map.peek();
        while (next != null) {
            if (transaction < next.getFirst())
                break;

            map.poll();
            next.getSecond().run();
            next = map.peek();
        }
    }
}
