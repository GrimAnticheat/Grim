package ac.grim.grimac.utils.collections;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Queue;

/**
 * A queue that supports inserting while iterating.
 * <p>
 * Iterators reflect the state on creation, newly added elements are skipped.
 *
 */
public class FastIteLinkedQueue<E> implements Queue<E> {

    int size = 0;
    Node<E> head;
    Node<E> tail;

    @Override
    public int size() {
        return size;
    }

    @Override
    public boolean isEmpty() {
        return size != 0;
    }

    @Override
    public @NotNull Iterator<E> iterator() {
        return new FastIterator();
    }

    @Override
    public boolean add(E e) {
        return offer(e);
    }

    @Override
    public boolean contains(Object o) {
        for (E e : this) {
            if (e.equals(o)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public @NotNull Object[] toArray() {
        Object[] result = new Object[size];
        int i = 0;
        for (E e: this) {
            result[i++] = e;
        }
        return result;
    }

    @Override
    public boolean remove(Object o) {
        Iterator<E> iterator = iterator();
        while (iterator.hasNext()) {
            if (iterator.next().equals(o)) {
                iterator.remove();
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean addAll(@NotNull Collection<? extends E> c) {
        for (E e : c) {
            offer(e);
        }
        return true;
    }

    @Override
    public void clear() {
        Node<E> node = head;
        while (node != null) {
            Node<E> next = node.next;
            node.item = null;
            node.next = null;
            node.prev = null;
            node = next;
        }
        head = tail = null;
        size = 0;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof Collection<?> c))
            return false;

        Iterator<E> i1 = this.iterator();
        Iterator<?> i2 = c.iterator();
        while (i1.hasNext() && i2.hasNext()) {
            Object o1 = i1.next();
            Object o2 = i2.next();
            if (!(Objects.equals(o1, o2)))
                return false;
        }
        return !(i1.hasNext() || i2.hasNext());
    }

    @Override
    public boolean offer(E e) {
        Node<E> last = tail;
        Node<E> newNode = new Node<>(e, last, null);
        tail = newNode;
        if (last == null) {
            head = newNode;
        } else {
            last.next = newNode;
        }
        size++;
        return true;
    }

    @Override
    public E remove() {
        if (size == 0) throw new NoSuchElementException();
        return poll();
    }

    @Override
    public E poll() {
        if (head == null)
            return null;
        Node<E> node = head;
        Node<E> next = head.next;
        size--;
        head = next;
        // Unlink
        node.next = null;
        if (next == null)
            tail = null;
        else
            next.prev = null;
        return node.item;
    }

    @Override
    public E element() {
        if (size == 0) throw new NoSuchElementException();
        return peek();
    }

    @Override
    public E peek() {
        return head != null ? head.item : null;
    }

    void onRemove(Node<E> node) {
        final Node<E> next = node.next;
        final Node<E> prev = node.prev;

        if (prev == null) {
            head = next;
        } else {
            prev.next = next;
            node.prev = null;
        }

        if (next == null) {
            tail = prev;
        } else {
            next.prev = prev;
            node.next = null;
        }

        node.item = null;
        size--;
    }

    static class Node<E> {
        E item;
        Node<E> prev;
        Node<E> next;

        Node(E element, Node<E> prev, Node<E> next) {
            this.item = element;
            this.next = next;
            this.prev = prev;
        }
    }

    class FastIterator implements Iterator<E> {

        int remains = size;
        Node<E> last = null;
        Node<E> next = head;

        @Override
        public boolean hasNext() {
            return remains != 0;
        }

        @Override
        public E next() {
//            if (remains == 0) throw new NoSuchElementException(); // No bounds check
            Node<E> node = next;
            next = node.next;
            remains--;
            last = node;
            return node.item;
        }

        @Override
        public void remove() {
            Node<E> node = this.last;
            Node<E> prev = node.prev;
            Node<E> next = this.next;
            if (prev != null) {
                prev.next = next;
            }
            if (next != null) {
                next.prev = prev;
            }
            onRemove(node);
        }

    }

    @Override
    public @NotNull <T> T[] toArray(@NotNull T[] a) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean containsAll(@NotNull Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean removeAll(@NotNull Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean retainAll(@NotNull Collection<?> c) {
        throw new UnsupportedOperationException();
    }

}
