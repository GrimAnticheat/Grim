package ac.grim.grimac.utils.lists;

import java.util.List;

// https://github.com/ThomasOM/Pledge/blob/master/src/main/java/dev/thomazz/pledge/util/collection/ListWrapper.java
public abstract class ListWrapper<T> implements List<T> {
    protected final List<T> base;

    public ListWrapper(List<T> base) {
        this.base = base;
    }

    public List<T> getBase() {
        return this.base;
    }
}