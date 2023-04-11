package ac.grim.grimac.utils.lists;

import ac.grim.grimac.utils.data.Pair;

import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

// This class is copyright DefineOutside licensed under MIT
//
// This class calculates the running mode of a list in best case o(1) worst case o(n) time.
public class RunningMode {
    Queue<Double> addList;
    Map<Double, Integer> popularityMap = new HashMap<>();
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

    public int getMaxSize() {
        return maxSize;
    }

    public void add(double value) {
        pop();

        for (Map.Entry<Double, Integer> entry : popularityMap.entrySet()) {
            if (Math.abs(entry.getKey() - value) < threshold) {
                entry.setValue(entry.getValue() + 1);
                addList.add(entry.getKey());
                return;
            }
        }

        // Nothing found
        popularityMap.put(value, 1);
        addList.add(value);
    }

    private void pop() {
        if (addList.size() >= maxSize) {
            Double type = addList.poll();
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

        for (Map.Entry<Double, Integer> entry : popularityMap.entrySet()) {
            if (entry.getValue() > max) {
                max = entry.getValue();
                mostPopular = entry.getKey();
            }
        }

        return new Pair<>(mostPopular, max);
    }
}
