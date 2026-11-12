package ac.grim.grimac.utils.functions;

@FunctionalInterface
public interface ObjectAndPositionFunction<T, R> {
    R apply(T o, int x, int y, int z);
}
