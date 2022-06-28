package ac.grim.grimac.utils.lists;

import lombok.Getter;

import java.util.Collection;
import java.util.LinkedList;


public final class EvictingList<T> extends LinkedList<T> {
    @Getter
    private final int max;

    public EvictingList(int max) {
        this.max = max;
    }

    public EvictingList(Collection<? extends T> collection, final int max) {
        super(collection);
        this.max = max;
    }

    @Override
    public boolean add(T t) {
        if (size() >= max) {
            removeFirst();
        }
        return super.add(t);
    }

    public boolean isFull() {
        return size() >= max;
    }

}