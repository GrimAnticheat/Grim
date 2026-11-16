package ac.grim.grimac.utils.reflection;

import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

@UtilityClass
public class ReflectionUtils {

    @Contract(pure = true)
    public static boolean hasClass(String className) {
        return getClass(className) != null;
    }

    @Contract(pure = true)
    public static boolean hasMethod(@NotNull Class<?> clazz, String methodName, Class<?>... parameterTypes) {
        return getMethod(clazz, methodName, parameterTypes) != null;
    }

    @Contract(pure = true)
    public static @Nullable Method getMethod(@NotNull Class<?> clazz, @NotNull String methodName, Class<?>... parameterTypes) {
        try {
            return clazz.getMethod(methodName, parameterTypes);
        } catch (NoSuchMethodException e) {
            while (clazz != null) {
                try {
                    return clazz.getDeclaredMethod(methodName, parameterTypes);
                } catch (NoSuchMethodException ignored) {
                    clazz = clazz.getSuperclass();
                }
            }
        }

        return null;
    }

    @Contract(pure = true)
    public static <T> @Nullable Constructor<T> getConstructor(@NotNull Class<T> clazz, Class<?>... parameterTypes) {
        try {
            return clazz.getConstructor(parameterTypes);
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    @Contract(pure = true)
    public static @Nullable Class<?> getClass(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    /**
     * Finds a field by name, searching up the superclass hierarchy.
     */
    @Contract(pure = true)
    public static Field getField(Class<?> clazz, String fieldName) {
        while (clazz != null) {
            try {
                Field field = clazz.getDeclaredField(fieldName);
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            }
        }
        return null;
    }
}
