package ac.grim.grimac.platform.bukkit.utils.reflection;

import ac.grim.grimac.utils.anticheat.LogUtil;
import ac.grim.grimac.utils.reflection.ReflectionUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.concurrent.atomic.AtomicBoolean;

public final class FoliaRegionTPS {

    private static final Class<?> TICK_REGION_SCHEDULER = ReflectionUtils.getClass("io.papermc.paper.threadedregions.TickRegionScheduler");
    private static final Method GET_CURRENT_REGION = findCurrentRegionMethod();
    private static final AtomicBoolean WARNED = new AtomicBoolean();
    private static volatile Chain chain;
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

            Chain resolved = chain;
            double tps = resolved != null ? resolved.read(region) : resolveAndRead(region);
            return Double.isFinite(tps) ? tps : Double.NaN;
        } catch (NoSuchMethodException | IllegalAccessException | IllegalArgumentException | ClassCastException e) {
            broken = true;
            warn(e);
        } catch (Exception e) {
            warn(e);
        }
        return Double.NaN;
    }

    private static Method findCurrentRegionMethod() {
        if (TICK_REGION_SCHEDULER == null) return null;
        Method method = ReflectionUtils.getMethod(TICK_REGION_SCHEDULER, "getCurrentRegion");
        return method != null && Modifier.isStatic(method.getModifiers()) ? method : null;
    }

    private static double resolveAndRead(Object region) throws ReflectiveOperationException {
        Method getData = require(region.getClass(), "getData");
        Object data = getData.invoke(region);
        if (data == null) return Double.NaN;

        Method getHandle = require(data.getClass(), "getRegionSchedulingHandle");
        Object handle = getHandle.invoke(data);
        if (handle == null) return Double.NaN;

        Method getReport = ReflectionUtils.getMethod(handle.getClass(), "getTickReport5s", long.class);
        if (getReport == null) getReport = require(handle.getClass(), "getTickReport15s", long.class);

        Object report = getReport.invoke(handle, System.nanoTime());
        if (report == null) return Double.NaN;

        Method tpsData = require(report.getClass(), "tpsData");
        Object tps = tpsData.invoke(report);
        if (tps == null) return Double.NaN;

        Method segmentAll = require(tps.getClass(), "segmentAll");
        Object segment = segmentAll.invoke(tps);
        if (segment == null) return Double.NaN;

        Method average = require(segment.getClass(), "average");
        double value = ((Number) average.invoke(segment)).doubleValue();

        chain = new Chain(getData, getHandle, getReport, tpsData, segmentAll, average);
        return value;
    }

    private static Method require(Class<?> type, String name, Class<?>... parameterTypes) throws NoSuchMethodException {
        Method method = ReflectionUtils.getMethod(type, name, parameterTypes);
        if (method == null) throw new NoSuchMethodException(type.getName() + "#" + name);
        return method;
    }

    private static void warn(Exception e) {
        if (WARNED.compareAndSet(false, true)) {
            LogUtil.warn("Failed to read the Folia region TPS, %tps% will report NaN", e);
        }
    }

    private record Chain(Method getData, Method getHandle, Method getReport, Method tpsData, Method segmentAll, Method average) {

        private double read(Object region) throws ReflectiveOperationException {
            Object data = getData.invoke(region);
            if (data == null) return Double.NaN;

            Object handle = getHandle.invoke(data);
            if (handle == null) return Double.NaN;

            Object report = getReport.invoke(handle, System.nanoTime());
            if (report == null) return Double.NaN;

            Object tps = tpsData.invoke(report);
            if (tps == null) return Double.NaN;

            Object segment = segmentAll.invoke(tps);
            if (segment == null) return Double.NaN;

            return ((Number) average.invoke(segment)).doubleValue();
        }
    }
}
