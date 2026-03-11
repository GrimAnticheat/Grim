package ac.grim.grimac.utils.lists;

import java.util.ArrayList;

// https://stackoverflow.com/a/21047889
// License: Originally CC By-SA 4.0 licensed as GPL
public class EvictingQueue<K> extends ArrayList<K> {

    private final int maxSize;

    public EvictingQueue(int size) {
        this.maxSize = size;
    }

    @Override
    public boolean add(K k) {
        boolean r = super.add(k);

        if (size() > maxSize) {
            removeRange(0, size() - maxSize);
        }

        return r;
    }

    public boolean isCollected() {
        return size() >= maxSize;
    }
}
