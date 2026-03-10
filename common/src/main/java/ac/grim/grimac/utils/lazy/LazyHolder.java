package ac.grim.grimac.utils.lazy;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public interface LazyHolder<T> {
    @Contract(value = "_ -> new", pure = true)
    static <T> @NotNull LazyHolder<T> threadSafe(Supplier<T> supplier) {
        return new ThreadSafeLazyHolder<>(supplier);
    }

    @Contract(value = "_ -> new", pure = true)
    static <T> @NotNull LazyHolder<T> simple(Supplier<T> supplier) {
        return new SimpleLazyHolder<>(supplier);
    }

    T get();
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
                    value = result = supplier.get();
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
