package ac.grim.grimac.platform.bukkit.utils.reflection;

import ac.grim.grimac.utils.anticheat.LogUtil;
import ac.grim.grimac.utils.reflection.ReflectionUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.concurrent.atomic.AtomicBoolean;

public final class FoliaRegionTPS {

    private static final String[] REPORT_METHODS = {"getTickReport5s", "getTickReport15s", "getTickReport1m"};

    private static final Method GET_REGION_TPS = findRegionTpsMethod();
    private static final Method GET_CURRENT_REGION = GET_REGION_TPS == null ? findCurrentRegionMethod() : null;
    private static final AtomicBoolean WARNED = new AtomicBoolean();
    private static volatile Chain chain;
    private static volatile boolean legacyUnavailable;

    private FoliaRegionTPS() {
    }

    public static boolean hasRegionTpsApi() {
        return GET_REGION_TPS != null;
    }

    public static boolean isLegacySupported() {
        return GET_CURRENT_REGION != null;
    }

    public static double regionTPS(Location location) {
        if (GET_REGION_TPS == null || location == null) return Double.NaN;
        try {
            double[] tps = (double[]) GET_REGION_TPS.invoke(null, location);
            return tps != null && tps.length > 0 && Double.isFinite(tps[0]) ? tps[0] : Double.NaN;
        } catch (Exception e) {
            warn(e);
            return Double.NaN;
        }
    }

    public static double currentRegionTPS() {
        if (legacyUnavailable || GET_CURRENT_REGION == null) return Double.NaN;
        try {
            Object region = GET_CURRENT_REGION.invoke(null);
            if (region == null) return Double.NaN;

            Chain resolved = chain;
            double tps = resolved != null ? resolved.read(region) : resolveAndRead(region);
            return Double.isFinite(tps) ? tps : Double.NaN;
        } catch (NoSuchMethodException | IllegalAccessException | IllegalArgumentException | ClassCastException e) {
            legacyUnavailable = true;
        } catch (Exception e) {
            warn(e);
        }
        return Double.NaN;
    }

    private static Method findRegionTpsMethod() {
        try {
            Method method = ReflectionUtils.getMethod(Bukkit.class, "getRegionTPS", Location.class);
            return method != null && Modifier.isStatic(method.getModifiers()) && method.getReturnType() == double[].class ? method : null;
        } catch (Exception | LinkageError e) {
            return null;
        }
    }

    private static Method findCurrentRegionMethod() {
        try {
            Class<?> scheduler = ReflectionUtils.getClass("io.papermc.paper.threadedregions.TickRegionScheduler");
            if (scheduler == null) return null;
            Method method = ReflectionUtils.getMethod(scheduler, "getCurrentRegion");
            if (method == null || !Modifier.isStatic(method.getModifiers())) return null;
            method.trySetAccessible();
            return method;
        } catch (Exception | LinkageError e) {
            return null;
        }
    }

    private static double resolveAndRead(Object region) throws ReflectiveOperationException {
        Method getData = require(region.getClass(), "getData");
        Object data = getData.invoke(region);
        if (data == null) return Double.NaN;

        Method getHandle = require(data.getClass(), "getRegionSchedulingHandle");
        Object handle = getHandle.invoke(data);
        if (handle == null) return Double.NaN;

        Method getReport = null;
        for (String name : REPORT_METHODS) {
            getReport = find(handle.getClass(), name, long.class);
            if (getReport != null) break;
        }
        if (getReport == null) throw new NoSuchMethodException(handle.getClass().getName() + "#getTickReport");

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

    private static Method find(Class<?> type, String name, Class<?>... parameterTypes) {
        Method method = ReflectionUtils.getMethod(type, name, parameterTypes);
        if (method != null) method.trySetAccessible();
        return method;
    }

    private static Method require(Class<?> type, String name, Class<?>... parameterTypes) throws NoSuchMethodException {
        Method method = find(type, name, parameterTypes);
        if (method == null) throw new NoSuchMethodException(type.getName() + "#" + name);
        return method;
    }

    public static void warn(Exception e) {
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
