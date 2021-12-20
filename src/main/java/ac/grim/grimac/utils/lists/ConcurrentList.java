/*
 * This file is part of packetevents - https://github.com/retrooper/packetevents
 * Copyright (C) 2021 retrooper and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package ac.grim.grimac.utils.lists;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.util.*;

/**
 * Created by wea_ondara licensed under MIT
 * Taken from https://github.com/weaondara/BungeePerms/blob/master/src/main/java/net/alpenblock/bungeeperms/util/ConcurrentList.java
 *
 * @author wea_ondara
 */
public class ConcurrentList<E> extends ArrayList<E> {
    private final Object lock = new Object();

    @Override
    public boolean add(E e) {
        synchronized (lock) {
            return super.add(e);
        }
    }

    @Override
    public void add(int index, E element) {
        synchronized (lock) {
            super.add(index, element);
        }
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
        synchronized (lock) {
            return super.addAll(c);
        }
    }

    @Override
    public boolean addAll(int index, Collection<? extends E> c) {
        synchronized (lock) {
            return super.addAll(index, c);
        }
    }

    @Override
    public void clear() {
        synchronized (lock) {
            super.clear();
        }
    }

    @Override
    public Object clone() {
        synchronized (lock) {
            try {
                ConcurrentList<E> clist = (ConcurrentList<E>) super.clone();
                clist.modCount = 0;
                Field f = ArrayList.class.getDeclaredField("elementData");
                f.setAccessible(true);
                f.set(clist, Arrays.copyOf((Object[]) f.get(this), this.size()));

                return clist;
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public boolean contains(Object o) {
        synchronized (lock) {
            return super.contains(o);
        }
    }

    @Override
    public void ensureCapacity(int minCapacity) {
        synchronized (lock) {
            super.ensureCapacity(minCapacity);
        }
    }

    @Override
    public E get(int index) {
        synchronized (lock) {
            return super.get(index);
        }
    }

    @Override
    public int indexOf(Object o) {
        synchronized (lock) {
            return super.indexOf(o);
        }
    }

    @Override
    public int lastIndexOf(Object o) {
        synchronized (lock) {
            return super.lastIndexOf(o);
        }
    }

    @Override
    public E remove(int index) {
        synchronized (lock) {
            return super.remove(index);
        }
    }

    @Override
    public boolean remove(Object o) {
        synchronized (lock) {
            return super.remove(o);
        }
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        synchronized (lock) {
            return super.removeAll(c);
        }
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        synchronized (lock) {
            return super.retainAll(c);
        }
    }

    @Override
    public E set(int index, E element) {
        synchronized (lock) {
            return super.set(index, element);
        }
    }

    @Override
    public @NotNull List<E> subList(int fromIndex, int toIndex) {
        synchronized (lock) {
            return super.subList(fromIndex, toIndex);
        }
    }

    @Override
    public Object[] toArray() {
        synchronized (lock) {
            return super.toArray();
        }
    }

    @Override
    public <T> T[] toArray(T[] a) {
        synchronized (lock) {
            return super.toArray(a);
        }
    }

    @Override
    public void trimToSize() {
        synchronized (lock) {
            super.trimToSize();
        }
    }

    @Override
    public @NotNull ListIterator<E> listIterator() {
        return new ListItr(0);
    }

    @Override
    public @NotNull Iterator<E> iterator() {
        return new Itr();
    }

    private class Itr implements Iterator<E> {

        final ConcurrentList l;
        protected int cursor;
        protected int lastRet;

        public Itr() {
            cursor = 0;
            lastRet = -1;
            l = (ConcurrentList) ConcurrentList.this.clone();
        }

        @Override
        public boolean hasNext() {
            return cursor < l.size();
        }

        @Override
        public E next() {
            int i = cursor;
            if (i >= l.size()) {
                throw new NoSuchElementException();
            }
            cursor = i + 1;
            return (E) l.get(lastRet = i);
        }

        @Override
        public void remove() {
            if (lastRet < 0) {
                throw new IllegalStateException();
            }

            l.remove(lastRet);
            ConcurrentList.this.remove(lastRet);
            cursor = lastRet;
            lastRet = -1;
        }
    }

    public class ListItr extends Itr implements ListIterator<E> {

        ListItr(int index) {
            super();
            cursor = index;
        }

        @Override
        public boolean hasPrevious() {
            return cursor > 0;
        }

        @Override
        public int nextIndex() {
            return cursor;
        }

        @Override
        public int previousIndex() {
            return cursor - 1;
        }

        @Override
        public E previous() {
            int i = cursor - 1;
            if (i < 0) {
                throw new NoSuchElementException();
            }
            cursor = i;
            return (E) l.get(lastRet = i);
        }

        @Override
        public void set(E e) {
            if (lastRet < 0) {
                throw new IllegalStateException();
            }

            l.set(lastRet, e);
            ConcurrentList.this.set(lastRet, e);
        }

        @Override
        public void add(E e) {
            int i = cursor;
            l.add(i, e);
            ConcurrentList.this.add(i, e);
            cursor = i + 1;
            lastRet = -1;
        }
    }
}