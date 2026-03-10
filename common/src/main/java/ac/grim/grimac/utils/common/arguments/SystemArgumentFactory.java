package ac.grim.grimac.utils.common.arguments;

import ac.grim.grimac.platform.api.Platform;
import org.jetbrains.annotations.NotNull;

import java.lang.management.ManagementFactory;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

public record SystemArgumentFactory(Map<String, String> arguments,
                                    Map<Class<?>, Function<String, ?>> parsers,
                                    Consumer<SystemArgument<?>> creationListener,
                                    Consumer<ArgumentOptions.Builder<?>> optionModifier) {

    public static class Builder {

        public static Builder of(final String prefix) {
            return new Builder(prefix);
        }

        public Builder onRegister(Consumer<SystemArgument<?>> listener) {
            this.registerListener = listener;
            return this;
        }

        public Builder optionModifier(Consumer<ArgumentOptions.Builder<?>> modifier) {
            this.optionModifier = modifier;
            return this;
        }

        public Builder supportEnv() {
            this.envSupport = true;
            return this;
        }

        private final String prefix;
        private boolean envSupport = false;
        private Consumer<SystemArgument<?>> registerListener = argument -> {
        };
        private final Map<Class<?>, Function<String, ?>> parseBuilder;
        private Consumer<ArgumentOptions.Builder<?>> optionModifier = null;

        private Builder(final String prefix) {
            this.prefix = prefix;
            this.parseBuilder = new HashMap<>();
            // Register default parsers
            registerDefaultParsers();
        }

        protected void registerDefaultParsers() {
            registerParser(Boolean.class, Boolean::parseBoolean)
                    .registerParser(Byte.class, Byte::parseByte)
                    .registerParser(Short.class, Short::parseShort)
                    .registerParser(Integer.class, Integer::parseInt)
                    .registerParser(Float.class, Float::parseFloat)
                    .registerParser(Double.class, Double::parseDouble)
                    .registerParser(Long.class, Long::parseLong)
                    .registerParser(Character.class, s -> !s.isEmpty() ? s.charAt(0) : '\0')
                    .registerParser(char[].class, String::toCharArray)
                    .registerParser(String.class, String::valueOf)
                    .registerParser(Charset.class, Charset::forName)
                    .registerParser(Platform.class, Platform::getByName);
        }

        public <T> Builder registerParser(Class<T> type, Function<String, T> parser) {
            parseBuilder.put(type, parser);
            return this;
        }

        private void updateFromEnv(Map<String, String> builder) {
            for (Map.Entry<String, String> entry : System.getenv().entrySet()) {
                if (entry.getKey().startsWith(prefix.toUpperCase())) {
                    if (builder.put(entry.getKey().toLowerCase(), entry.getValue()) != null) {
                        warn("Env variable overwriting system variable: " + entry.getKey());
                    }
                }
            }
        }

        public SystemArgumentFactory build() {
            // Parse arguments from JVM arguments
            final String findPrefix = "-d" + prefix.toLowerCase();
            Map<String, String> builder = getSystemPropertiesMap(findPrefix);
            if (envSupport) {
                try {
                    updateFromEnv(builder);
                } catch (Exception e) {
                    exception("Failed to read environment variables", e);
                }
            }
            //
            return new SystemArgumentFactory(Map.copyOf(builder), Map.copyOf(parseBuilder), registerListener, optionModifier);
        }

        protected @NotNull Map<String, String> getSystemPropertiesMap(String findPrefix) {
            Map<String, String> builder = new HashMap<>();
            for (String line : ManagementFactory.getRuntimeMXBean().getInputArguments()) {
                if (line.toLowerCase().startsWith(findPrefix)) {
                    int index = line.indexOf('=');
                    if (index > 0 && index < line.length() - 1) {
                        String key = line.substring(2, index); // remove -D
                        String value = line.substring(index + 1);
                        builder.put(key.toLowerCase(), value);
                    } else {
                        warn("Invalid startup argument: " + line);
                    }
                }
            }
            return builder;
        }
    }

    private <T> SystemArgument<T> createDefaultSupplier(ArgumentOptions<T> options) {
        T value = options.getModifier().apply(options.getDefaultSupplier().get());
        if (value == null && !options.isNullable())
            throw new IllegalArgumentException("Default value cannot be null for startup argument \"" + options.getKey() + "\"");
        if ((value != null && !options.getVerifier().test(value)))
            throw new IllegalArgumentException("Invalid default value for startup argument \"" + options.getKey() + "\"");
        SystemArgument<T> argument = new SystemArgument<>(options.getKey(), options.getClazz(), value, false, options.getVisibility());
        creationListener.accept(argument);
        return argument;
    }

    public <T> SystemArgument<T> create(ArgumentOptions.Builder<T> builder) {
        if (optionModifier != null) optionModifier.accept(builder);
        final ArgumentOptions<T> options = builder.build();
        //
        final String value = arguments.get(options.getKey().toLowerCase());
        if (value == null) return createDefaultSupplier(options);
        try {
            @SuppressWarnings("unchecked")
            Function<String, T> parser = (Function<String, T>) parsers.get(options.getClazz());
            if (parser == null) return createDefaultSupplier(options);
            T parsed = options.getModifier().apply(parser.apply(value));
            if (parsed == null || !options.getVerifier().test(parsed))
                return createDefaultSupplier(options);
            SystemArgument<T> newArgument = new SystemArgument<>(options.getKey(), options.getClazz(), parsed, true, options.getVisibility());
            creationListener.accept(newArgument);
            return newArgument;
        } catch (Exception e) {
            exception("Failed to parse value for startup argument \"" + options.getKey() + "\"", e);
            return createDefaultSupplier(options);
        }
    }

    public Map<String, String> getFoundArguments() {
        return arguments;
    }

    //TODO: add back logging once LogUtil has been refactored

    private static void exception(String message, Exception e) {
        //LogUtil.exception(message, e);
    }

    private static void warn(String message) {
        //LogUtil.warn(message);
    }

}
