package ac.grim.grimac.utils.lists;

import ac.grim.grimac.utils.data.Pair;
import it.unimi.dsi.fastutil.doubles.Double2IntMap;
import it.unimi.dsi.fastutil.doubles.Double2IntOpenHashMap;
import lombok.Getter;

import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

// This class is copyright DefineOutside licensed under MIT
//
// This class calculates the running mode of a list in best case o(1) worst case o(n) time.
public class RunningMode {
    Queue<Double> addList;
    Double2IntMap popularityMap = new Double2IntOpenHashMap();
    @Getter
    int maxSize;

    private static final double threshold = 1e-3;

    public RunningMode(int maxSize) {
        if (maxSize == 0) throw new IllegalArgumentException("There's no mode to a size 0 list!");
        this.addList = new ArrayBlockingQueue<>(maxSize);
        this.maxSize = maxSize;
    }

    public int size() {
        return addList.size();
    }

    public void add(double value) {
        pop();

        for (Double2IntMap.Entry entry : popularityMap.double2IntEntrySet()) {
            if (Math.abs(entry.getDoubleKey() - value) < threshold) {
                entry.setValue(entry.getIntValue() + 1);
                addList.add(entry.getDoubleKey());
                return;
            }
        }

        // Nothing found
        popularityMap.put(value, 1);
        addList.add(value);
    }

    private void pop() {
        if (addList.size() >= maxSize) {
            double type = addList.poll();
            int popularity = popularityMap.get(type);  // Being null isn't possible
            if (popularity == 1) {
                popularityMap.remove(type); // Make sure not to leak memory
            } else {
                popularityMap.put(type, popularity - 1); // Decrease popularity
            }
        }
    }

    public Pair<Double, Integer> getMode() {
        int max = 0;
        Double mostPopular = null;

        for (Double2IntMap.Entry entry : popularityMap.double2IntEntrySet()) {
            if (entry.getIntValue() > max) {
                max = entry.getIntValue();
                mostPopular = entry.getDoubleKey();
            }
        }

        return new Pair<>(mostPopular, max);
    }
}
