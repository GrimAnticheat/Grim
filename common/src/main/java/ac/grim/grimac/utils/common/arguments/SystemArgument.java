package ac.grim.grimac.utils.common.arguments;

import java.util.function.Function;
import java.util.function.Predicate;

public record SystemArgument<T>(String key, Class<T> clazz, T value, boolean set,
                                Visibility visibility) {

    public boolean matches(Predicate<T> predicate) {
        return predicate.test(value);
    }

    public <K> K mapValue(Function<T, K> mapper, K otherwise) {
        try {
            return value == null ? otherwise : mapper.apply(value);
        } catch (Exception e) {
            //TODO: add back logging once LogUtil has been refactored
            //LogUtil.exception("Failed to map value for argument " + key, e);
        }
        return otherwise;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        SystemArgument<?> that = (SystemArgument<?>) o;
        return key.equals(that.key);
    }

    @Override
    public int hashCode() {
        return key.hashCode();
    }

    public enum Visibility {
        VISIBLE, // visible from commands
        HIDDEN, // only visible in console
        SECRET // only visible in console if set
    }

}
