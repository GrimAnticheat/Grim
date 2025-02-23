package ac.grim.grimac.utils.lazy;

import java.util.function.Supplier;

public interface LazyHolder<T> {
    T get();

    static <T> LazyHolder<T> threadSafe(Supplier<T> supplier) {
        return new ThreadSafeLazyHolder<>(supplier);
    }

    static <T> LazyHolder<T> simple(Supplier<T> supplier) {
        return new SimpleLazyHolder<>(supplier);
    }
}

final class ThreadSafeLazyHolder<T> implements LazyHolder<T> {
    private final Supplier<T> supplier;
    private volatile T value;

    ThreadSafeLazyHolder(Supplier<T> supplier) {
        this.supplier = supplier;
    }

    @Override
    public T get() {
        T result = value;
        if (result == null) {
            synchronized (this) {
                result = value;
                if (result == null) {
                    result = supplier.get();
                    value = result;
                }
            }
        }
        return result;
    }
}

final class SimpleLazyHolder<T> implements LazyHolder<T> {
    private T value;
    private Supplier<T> supplier;

    SimpleLazyHolder(Supplier<T> supplier) {
        this.supplier = supplier;
    }

    @Override
    public T get() {
        if (supplier != null) {
            value = supplier.get();
            supplier = null;
        }
        return value;
    }
}
