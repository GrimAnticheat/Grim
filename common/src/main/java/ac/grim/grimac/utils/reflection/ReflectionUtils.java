package ac.grim.grimac.utils.reflection;

import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;

@UtilityClass
public class ReflectionUtils {

    public static boolean hasClass(String className) {
        return getClass(className) != null;
    }

    public static boolean hasMethod(@NotNull Class<?> clazz, String methodName, Class<?>... parameterTypes) {
        return getMethod(clazz, methodName, parameterTypes) != null;
    }

    public static boolean hasMethod(@NotNull String className, @NotNull String methodName, Class<?>... parameterTypes) {
        Class<?> clazz = getClass(className);
        return clazz != null && hasMethod(clazz, methodName, parameterTypes);
    }

    public static boolean hasMethod(@NotNull String className, @NotNull String methodName) {
        return hasMethod(className, methodName, new Class<?>[0]);
    }

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

    public static @Nullable Class<?> getClass(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }
}
