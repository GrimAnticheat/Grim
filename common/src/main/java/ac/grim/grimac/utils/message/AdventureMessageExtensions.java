package ac.grim.grimac.utils.message;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.NBTComponent;
import net.kyori.adventure.text.ObjectComponent;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

/** Additive Adventure 5 fields, while the common module still runs on Adventure 4.26. */
final class AdventureMessageExtensions {
    private static final MethodHandle PLAIN = getter(NBTComponent.class, "plain", boolean.class, false);
    private static final MethodHandle FALLBACK = getter(ObjectComponent.class, "fallback", Component.class, null);

    private AdventureMessageExtensions() {
    }

    static boolean plain(NBTComponent<?, ?> component) {
        try {
            return (boolean) PLAIN.invokeExact(component);
        } catch (Throwable failure) {
            throw new IllegalStateException("Cannot read the NBT component's plain flag", failure);
        }
    }

    static Component fallback(ObjectComponent component) {
        try {
            return (Component) FALLBACK.invokeExact(component);
        } catch (Throwable failure) {
            throw new IllegalStateException("Cannot read the object component's fallback", failure);
        }
    }

    private static MethodHandle getter(Class<?> owner, String name, Class<?> result, Object defaultValue) {
        try {
            return MethodHandles.publicLookup().findVirtual(owner, name, MethodType.methodType(result));
        } catch (NoSuchMethodException ignored) {
            return MethodHandles.dropArguments(MethodHandles.constant(result, defaultValue), 0, owner);
        } catch (IllegalAccessException failure) {
            throw new ExceptionInInitializerError(failure);
        }
    }
}
