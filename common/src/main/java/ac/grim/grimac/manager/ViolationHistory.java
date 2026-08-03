package ac.grim.grimac.manager;

import it.unimi.dsi.fastutil.longs.Long2ObjectRBTreeMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectSortedMap;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Stores violations in timestamp order for efficient expiry and maintains
 * identity-based counts for constant-time per-check lookups.
 */
final class ViolationHistory<T> {
    private final Long2ObjectSortedMap<T> entries = new Long2ObjectRBTreeMap<>();
    private final Map<T, Integer> counts = new IdentityHashMap<>();

    void record(long timestamp, T value, long maxAge) {
        // Keeps counts consistent when an entry with the same timestamp is replaced.
        T previous = entries.put(timestamp, value);
        if (previous != value) {
            if (previous != null) {
                decrement(previous);
            }
            counts.merge(value, 1, Integer::sum);
        }

        while (!entries.isEmpty() && timestamp - entries.firstLongKey() > maxAge) {
            decrement(entries.remove(entries.firstLongKey()));
        }
    }

    int size() {
        return entries.size();
    }

    int count(T value) {
        return counts.getOrDefault(value, 0);
    }

    private void decrement(T value) {
        int count = counts.get(value);
        if (count == 1) {
            counts.remove(value);
        } else {
            counts.put(value, count - 1);
        }
    }
}
