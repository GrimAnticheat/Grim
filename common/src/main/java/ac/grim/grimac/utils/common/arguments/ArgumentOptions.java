package ac.grim.grimac.utils.common.arguments;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

@Getter
public class ArgumentOptions<T> {

    private ArgumentOptions(Class<T> clazz, String key, Supplier<T> defaultSupplier) {
        this.clazz = clazz;
        this.key = key;
        this.defaultSupplier = defaultSupplier;
    }

    private final Class<T> clazz;
    private String key;
    private Supplier<T> defaultSupplier;
    private Predicate<T> verifier = t -> true;
    private Function<T, T> modifier = t -> t;
    private SystemArgument.Visibility visibility = SystemArgument.Visibility.VISIBLE;
    private boolean nullable = false;

    public static <T> Builder<T> from(Class<T> clazz, String key, @NotNull Supplier<@NotNull T> defaultValue) {
        return new Builder<>(new ArgumentOptions<>(clazz, key, defaultValue));
    }

    public static <T> Builder<T> from(Class<T> clazz, String key, T defaultValue) {
        return new Builder<>(new ArgumentOptions<>(clazz, key, () -> defaultValue)).nullable(defaultValue == null);
    }

    public static <T> Builder<T> from(Class<T> clazz, String key) {
        return new Builder<>(new ArgumentOptions<>(clazz, key, () -> null)).nullable(true);
    }

    public record Builder<T>(ArgumentOptions<T> options) {

        public Builder<T> key(String key) {
            options.key = key;
            return this;
        }

        public Builder<T> verifier(Predicate<T> predicate) {
            options.verifier = predicate;
            return this;
        }

        public Builder<T> modifier(Function<T, T> modifier) {
            options.modifier = modifier;
            return this;
        }

        public Builder<T> defaultSupplier(Supplier<T> supplier) {
            options.defaultSupplier = supplier;
            return this;
        }

        public Builder<T> visibility(SystemArgument.Visibility visibility) {
            options.visibility = visibility;
            return this;
        }

        private Builder<T> nullable(boolean nullable) {
            options.nullable = nullable;
            return this;
        }

        public ArgumentOptions<T> build() {
            return options;
        }
    }
}
