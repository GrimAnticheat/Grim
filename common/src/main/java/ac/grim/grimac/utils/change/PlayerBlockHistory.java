package ac.grim.grimac.utils.change;

import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.function.Predicate;

/**
 * Tracks block modifications made by a player over time.
 */
public class PlayerBlockHistory {
    private final ConcurrentLinkedDeque<BlockModification> blockHistory = new ConcurrentLinkedDeque<>();

    /**
     * Adds a new block modification to the history.
     * @param modification The block modification to add
     */
    public void add(BlockModification modification) {
        blockHistory.add(modification);
    }

    /**
     * Retrieves recent modifications that match the given filter.
     * @param filter Predicate to filter modifications
     * @return Filtered list of block modifications
     */
    public Iterable<BlockModification> getRecentModifications(Predicate<BlockModification> filter) {
        return blockHistory.stream().filter(filter).toList();
    }

    /**
     * Removes modifications older than the specified tick.
     * @param maxTick The maximum tick age to keep
     */
    public void cleanup(int maxTick) {
        while (!blockHistory.isEmpty() && maxTick - blockHistory.peekFirst().tick() > 0) {
            blockHistory.removeFirst();
        }
    }

    public int size() {
        return blockHistory.size();
    }

    public void clear() {
        blockHistory.clear();
    }
}
