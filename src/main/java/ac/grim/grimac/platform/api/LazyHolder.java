package ac.grim.grimac.platform.api;

import java.util.function.Supplier;

public final class LazyHolder<T> {
    private final Supplier<T> supplier;
    private volatile T value;

    private LazyHolder(Supplier<T> supplier) {
        this.supplier = supplier;
    }

    public static <T> LazyHolder<T> of(Supplier<T> supplier) {
        return new LazyHolder<>(supplier);
    }

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
