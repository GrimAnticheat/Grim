package ac.grim.grimac.utils.lists;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

// https://github.com/ThomasOM/Pledge/blob/master/src/main/java/dev/thomazz/pledge/util/collection/HookedListWrapper.java
@SuppressWarnings({"unchecked"})
public abstract class HookedListWrapper<T> extends ListWrapper<T> {
    public HookedListWrapper(List<T> base) {
        super(base);
    }

    // We can use the List#size call to execute some code
    public abstract void onIterator();

    @Override
    public int size() {
        return this.base.size();
    }

    @Override
    public boolean isEmpty() {
        return this.base.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return this.base.contains(o);
    }

    @Override
    public Iterator<T> iterator() {
        this.onIterator();
        return this.listIterator();
    }

    @Override
    public Object[] toArray() {
        return this.base.toArray();
    }

    @Override
    public boolean add(T o) {
        return this.base.add(o);
    }

    @Override
    public boolean remove(Object o) {
        return this.base.remove(o);
    }

    @Override
    public boolean addAll(Collection c) {
        return this.base.addAll(c);
    }

    @Override
    public boolean addAll(int index, Collection c) {
        return this.base.addAll(index, c);
    }

    @Override
    public void clear() {
        this.base.clear();
    }

    @Override
    public T get(int index) {
        return this.base.get(index);
    }

    @Override
    public T set(int index, T element) {
        return this.base.set(index, element);
    }

    @Override
    public void add(int index, T element) {
        this.base.add(index, element);
    }

    @Override
    public T remove(int index) {
        return this.base.remove(index);
    }

    @Override
    public int indexOf(Object o) {
        return this.base.indexOf(o);
    }

    @Override
    public int lastIndexOf(Object o) {
        return this.base.lastIndexOf(o);
    }

    @Override
    public ListIterator<T> listIterator() {
        return this.base.listIterator();
    }

    @Override
    public ListIterator<T> listIterator(int index) {
        return this.base.listIterator(index);
    }

    @Override
    public List<T> subList(int fromIndex, int toIndex) {
        return this.base.subList(fromIndex, toIndex);
    }

    @Override
    public boolean retainAll(Collection c) {
        return this.base.retainAll(c);
    }

    @Override
    public boolean removeAll(Collection c) {
        return this.base.removeAll(c);
    }

    @Override
    public boolean containsAll(Collection c) {
        return this.base.containsAll(c);
    }

    @Override
    public Object[] toArray(Object[] a) {
        return this.base.toArray(a);
    }
}