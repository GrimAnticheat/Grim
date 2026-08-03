package ac.grim.grimac.manager;

import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;

/**
 * Stores violations in monotonic timestamp order for constant-time expiry and maintains
 * identity-based counts for constant-time per-check lookups.
 */
final class ViolationHistory<T> {
    private final Long2ObjectLinkedOpenHashMap<T> entries = new Long2ObjectLinkedOpenHashMap<>();
    private final Reference2IntOpenHashMap<T> counts = new Reference2IntOpenHashMap<>();

    void record(long timestamp, T value, long maxAge) {
        // Keeps counts consistent when an entry with the same timestamp is replaced.
        T previous = entries.put(timestamp, value);
        if (previous != value) {
            if (previous != null) {
                decrement(previous);
            }
            counts.addTo(value, 1);
        }

        int expired = 0;
        while (!entries.isEmpty() && timestamp - entries.firstLongKey() > maxAge) {
            decrement(entries.removeFirst());
            expired++;
        }

        // Release oversized backing arrays after most of the history expires.
        if (expired > entries.size()) {
            entries.trim();
        }
    }

    int size() {
        return entries.size();
    }

    int count(T value) {
        return counts.getInt(value);
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
