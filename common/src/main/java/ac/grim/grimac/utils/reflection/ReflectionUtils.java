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

    public static boolean hasServerBrand(@NotNull String expected) {
        try {
            Class<?> bukkit = getClass("org.bukkit.Bukkit");
            if (bukkit == null) return false;
            Method m = getMethod(bukkit, "getName");
            if (m == null) return false;
            Object v = m.invoke(null);
            return expected.equalsIgnoreCase(String.valueOf(v));
        } catch (Throwable t) {
            return false;
        }
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
