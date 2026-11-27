package ac.grim.grimac.platform.bukkit.utils.reflection;

import ac.grim.grimac.utils.anticheat.LogUtil;
import ac.grim.grimac.utils.reflection.ReflectionUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.concurrent.atomic.AtomicBoolean;

public final class FoliaRegionTPS {

    private static final String[] REPORT_METHODS = {"getTickReport5s", "getTickReport15s", "getTickReport1m"};

    private static final Method GET_REGION_TPS = findRegionTpsMethod();
    private static final Method GET_CURRENT_REGION = GET_REGION_TPS == null ? findCurrentRegionMethod() : null;
    private static final AtomicBoolean WARNED = new AtomicBoolean();
    private static Chain chain;
    private static boolean legacyUnavailable;

    private FoliaRegionTPS() {
    }

    public static boolean isSupported() {
        return GET_REGION_TPS != null || GET_CURRENT_REGION != null;
    }

    public static double currentRegionTPS(Player player) {
        return GET_REGION_TPS != null ? apiRegionTPS(player.getLocation()) : legacyRegionTPS();
    }

    private static double apiRegionTPS(Location location) {
        try {
            double[] tps = (double[]) GET_REGION_TPS.invoke(null, location);
            return tps != null && tps.length > 0 && Double.isFinite(tps[0]) ? tps[0] : Double.NaN;
        } catch (Exception e) {
            warn(e);
            return Double.NaN;
        }
    }

    private static double legacyRegionTPS() {
        if (legacyUnavailable) return Double.NaN;
        try {
            Object region = GET_CURRENT_REGION.invoke(null);
            if (region == null) return Double.NaN;

            Chain resolved = chain;
            if (resolved == null) {
                resolved = resolve(region);
                if (resolved == null) return Double.NaN;
                chain = resolved;
            }
            double tps = resolved.read(region);
            return Double.isFinite(tps) ? tps : Double.NaN;
        } catch (NoSuchMethodException | IllegalAccessException | IllegalArgumentException | ClassCastException e) {
            legacyUnavailable = true;
            warn(e);
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

    private static Chain resolve(Object region) throws ReflectiveOperationException {
        Method getData = require(region.getClass(), "getData");
        Object data = getData.invoke(region);
        if (data == null) return null;

        Method getHandle = require(data.getClass(), "getRegionSchedulingHandle");
        Object handle = getHandle.invoke(data);
        if (handle == null) return null;

        Method getReport = null;
        for (String name : REPORT_METHODS) {
            getReport = find(handle.getClass(), name, long.class);
            if (getReport != null) break;
        }
        if (getReport == null) throw new NoSuchMethodException(handle.getClass().getName() + "#getTickReport");

        Object report = getReport.invoke(handle, System.nanoTime());
        if (report == null) return null;

        Method tpsData = require(report.getClass(), "tpsData");
        Object tps = tpsData.invoke(report);
        if (tps == null) return null;

        Method segmentAll = require(tps.getClass(), "segmentAll");
        Object segment = segmentAll.invoke(tps);
        if (segment == null) return null;

        Method average = require(segment.getClass(), "average");
        return new Chain(getData, getHandle, getReport, tpsData, segmentAll, average);
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
        if (!WARNED.compareAndSet(false, true)) return;
        LogUtil.warn("Failed to read the Folia region TPS, %tps% will report NaN", e);
    }

    public static void warnUnsupported() {
        if (WARNED.compareAndSet(false, true)) {
            LogUtil.warn("Folia region TPS is not available on this server, %tps% will report NaN");
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
