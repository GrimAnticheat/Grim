package ac.grim.grimac.utils.anticheat;

import com.google.gson.internal.Primitives;

import java.lang.invoke.*;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

public class LambdaUtils {

    public static <T, V, R> BiFunction<V, R, T> createTwoArgConstructorForClass(Class<T> clazz,
                                                                                Class<V> firstArg, Class<R> secondArg) throws Throwable {
        final MethodHandles.Lookup caller = MethodHandles.lookup();

        final MethodHandle target = caller.findConstructor(clazz, MethodType.methodType(void.class, firstArg, secondArg));

        CallSite callSite = LambdaMetafactory.metafactory(caller, "apply", MethodType.methodType(BiFunction.class),
                target.type().generic(), target, MethodType.methodType(Object.class,
                        firstArg.isPrimitive() ? Primitives.wrap(firstArg) : firstArg,
                        secondArg.isPrimitive() ? Primitives.wrap(secondArg) : secondArg));
        MethodHandle factory = callSite.getTarget();
        return (BiFunction<V, R, T>) factory.invoke();
    }

    public static <T, V> Function<V, T> createArgConstructorForClass(Class<T> clazz, Class<V> firstArg) throws Throwable {
        final MethodHandles.Lookup caller = MethodHandles.lookup();

        final MethodHandle target = caller.findConstructor(clazz, MethodType.methodType(void.class, firstArg));

        final CallSite callSite = LambdaMetafactory.metafactory(caller, "apply", MethodType.methodType(Function.class),
                target.type().generic(), target, MethodType.methodType(Object.class, firstArg.isPrimitive() ? Primitives.wrap(firstArg) : firstArg));
        MethodHandle factory = callSite.getTarget();
        return (Function<V, T>) factory.invoke();
    }

    public static <T> Supplier<T> createEmptyConstructorForClass(Class<T> clazz) throws Throwable {
        final MethodHandles.Lookup caller = MethodHandles.lookup();
        final MethodHandle target = caller.findConstructor(clazz, MethodType.methodType(void.class));

        final CallSite callSite = LambdaMetafactory.metafactory(caller, "get", MethodType.methodType(Supplier.class),
                target.type().generic(), target, target.type());
        MethodHandle factory = callSite.getTarget();
        return (Supplier<T>) factory.invoke();
    }

    public static <C, T, V> BiFunction<C, V, T> createGetMethodSoloArg(Class<C> clazz, String methodName,
                                                                       Class<T> fieldType, Class<V> argument)
            throws Throwable {
        final MethodHandles.Lookup caller = MethodHandles.lookup();
        final MethodType methodType = MethodType.methodType(fieldType, argument);
        final MethodHandle target = caller.findVirtual(clazz, methodName, methodType);
        MethodType type = target.type();
        if (fieldType.isPrimitive()) type = type.changeReturnType(Primitives.wrap(fieldType));

        final CallSite callSite = LambdaMetafactory.metafactory(caller,
                "apply", MethodType.methodType(BiFunction.class),
                type.erase(), target, type);

        final MethodHandle factory = callSite.getTarget();
        return (BiFunction<C, V, T>) factory.invoke();
    }

    public static <C, T> Function<C, T> createGetMethod(Class<C> clazz, String methodName,
                                                        Class<T> type) throws Throwable {
        final MethodHandles.Lookup caller = MethodHandles.lookup();
        final MethodType methodType = MethodType.methodType(type);
        final MethodHandle target = caller.findVirtual(clazz, methodName, methodType);
        MethodType wrappedType = target.type();
        if (type.isPrimitive()) wrappedType = wrappedType.changeReturnType(Primitives.wrap(type));

        final CallSite callSite = LambdaMetafactory.metafactory(caller,
                "apply", MethodType.methodType(Function.class),
                wrappedType.erase(), target, wrappedType);

        final MethodHandle factory = callSite.getTarget();
        return (Function<C, T>) factory.invoke();
    }
    public static <C, T> BiConsumer<C, T> createSetMethod(Class<C> clazz, String methodName,
                                                          Class<T> type) throws Throwable {
        final MethodHandles.Lookup caller = MethodHandles.lookup();
        final MethodType methodType = MethodType.methodType(void.class, type);
        final MethodHandle target = caller.findVirtual(clazz, methodName, methodType);
        MethodType wrappedType = target.type();
        if (type.isPrimitive()) wrappedType = wrappedType.changeParameterType(1, Primitives.wrap(type));

        final CallSite callSite = LambdaMetafactory.metafactory(caller,
                "accept", MethodType.methodType(BiConsumer.class),
                wrappedType.erase(), target, wrappedType);

        final MethodHandle factory = callSite.getTarget();
        return (BiConsumer<C, T>) factory.invoke();
    }
}