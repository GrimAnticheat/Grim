package ac.grim.grimac.manager;

import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import it.unimi.dsi.fastutil.objects.ObjectArrayFIFOQueue;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;

/**
 * Stores every violation in monotonic timestamp order for constant-time expiry and
 * maintains identity-based counts for constant-time per-check lookups.
 */
final class ViolationHistory<T> {
    private static final int INITIAL_CAPACITY = 4;

    private LongArrayFIFOQueue timestamps;
    private ObjectArrayFIFOQueue<T> values;
    private Reference2IntOpenHashMap<T> counts;

    void record(long timestamp, T value, long maxAge) {
        if (timestamps == null) {
            timestamps = new LongArrayFIFOQueue(INITIAL_CAPACITY);
            values = new ObjectArrayFIFOQueue<>(INITIAL_CAPACITY);
            counts = new Reference2IntOpenHashMap<>(INITIAL_CAPACITY);
        }

        timestamps.enqueue(timestamp);
        values.enqueue(value);
        counts.addTo(value, 1);

        int expired = 0;
        while (!timestamps.isEmpty() && timestamp - timestamps.firstLong() > maxAge) {
            timestamps.dequeueLong();
            decrement(values.dequeue());
            expired++;
        }

        // Release oversized backing arrays after most of the history expires.
        if (expired > timestamps.size()) {
            timestamps.trim();
            values.trim();
            counts.trim();
        }
    }

    int size() {
        return timestamps == null ? 0 : timestamps.size();
    }

    int count(T value) {
        return counts == null ? 0 : counts.getInt(value);
    }

    private void decrement(T value) {
        int count = counts.getInt(value);
        if (count == 1) {
            counts.removeInt(value);
        } else {
            counts.put(value, count - 1);
        }
    }
}
