package ac.grim.grimac.platform.bukkit.utils.reflection;

import ac.grim.grimac.utils.anticheat.LogUtil;
import ac.grim.grimac.utils.reflection.ReflectionUtils;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public final class FoliaRegionTPS {

    private static final Class<?> TICK_REGION_SCHEDULER = ReflectionUtils.getClass("io.papermc.paper.threadedregions.TickRegionScheduler");
    private static final Method GET_CURRENT_REGION = TICK_REGION_SCHEDULER == null ? null : ReflectionUtils.getMethod(TICK_REGION_SCHEDULER, "getCurrentRegion");
    private static final Map<MethodKey, Method> METHODS = new ConcurrentHashMap<>();
    private static final AtomicBoolean WARNED = new AtomicBoolean();
    private static volatile boolean broken;

    private FoliaRegionTPS() {
    }

    public static boolean isSupported() {
        return GET_CURRENT_REGION != null;
    }

    public static double currentRegionTPS() {
        if (broken || GET_CURRENT_REGION == null) return Double.NaN;
        try {
            Object region = GET_CURRENT_REGION.invoke(null);
            if (region == null) return Double.NaN;

            Object data = invoke(region, "getData");
            Object handle = invoke(data, "getRegionSchedulingHandle");
            Object report = resolve(handle.getClass(), "getTickReport5s", long.class).invoke(handle, System.nanoTime());
            if (report == null) return Double.NaN;

            Object tpsData = invoke(report, "tpsData");
            Object segment = invoke(tpsData, "segmentAll");
            double tps = ((Number) invoke(segment, "average")).doubleValue();
            return Double.isFinite(tps) ? tps : Double.NaN;
        } catch (NoSuchMethodException | IllegalAccessException | ClassCastException e) {
            broken = true;
            warn(e);
        } catch (Exception e) {
            warn(e);
        }
        return Double.NaN;
    }

    private static Object invoke(Object target, String name) throws ReflectiveOperationException {
        return resolve(target.getClass(), name).invoke(target);
    }

    private static Method resolve(Class<?> type, String name, Class<?>... parameterTypes) throws NoSuchMethodException {
        MethodKey key = new MethodKey(type, name, List.of(parameterTypes));
        Method cached = METHODS.get(key);
        if (cached != null) return cached;

        Method method = ReflectionUtils.getMethod(type, name, parameterTypes);
        if (method == null) throw new NoSuchMethodException(type.getName() + "#" + name);
        METHODS.put(key, method);
        return method;
    }

    private static void warn(Exception e) {
        if (WARNED.compareAndSet(false, true)) {
            LogUtil.warn("Failed to read the Folia region TPS, %tps% will report NaN", e);
        }
    }

    private record MethodKey(Class<?> type, String name, List<Class<?>> parameterTypes) {
    }
}
