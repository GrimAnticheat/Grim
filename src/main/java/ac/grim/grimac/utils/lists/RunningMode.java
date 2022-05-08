package ac.grim.grimac.utils.lists;

import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;

// This class is copyright DefineOutside licensed under MIT
//
// This class calculates the running mode of a list in best case o(1) worst case o(n) time.
public class RunningMode<T> {
    Queue<T> addList;
    Map<T, Integer> popularityMap = new HashMap<>();
    int maxSize;

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

    public void add(T t) {
        if (addList.size() >= maxSize) {
            T type = addList.poll();
            int popularity = popularityMap.get(type);  // Being null isn't possible
            if (popularity == 1) {
                popularityMap.remove(type); // Make sure not to leak memory
            } else {
                popularityMap.put(type, popularity - 1); // Decrease popularity
            }
        }
        addList.add(t);
        popularityMap.put(t, popularityMap.getOrDefault(t, 0) + 1);
    }

    public T getMode() {
        int max = 0;
        T mostPopular = null;

        for (Map.Entry<T, Integer> entry : popularityMap.entrySet()) {
            if (entry.getValue() > max) {
                max = entry.getValue();
                mostPopular = entry.getKey();
            }
        }

        return mostPopular;
    }
}
