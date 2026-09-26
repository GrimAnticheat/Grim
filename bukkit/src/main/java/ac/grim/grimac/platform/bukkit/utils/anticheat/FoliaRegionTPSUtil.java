package ac.grim.grimac.platform.bukkit.utils.anticheat;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.function.Consumer;

public final class FoliaRegionTPSUtil {

    private static volatile boolean shiftResolved = false;
    private static int regionChunkShift = 4;

    private FoliaRegionTPSUtil() {}

    public static double[] getPlayerRegionTPSAndMSPT(Player player) {
        try {
            resolveRegionChunkShift();

            World world = player.getWorld();
            Object worldServer = world.getClass().getMethod("getHandle").invoke(world);

            Field regioniserField = findField(worldServer.getClass(), "regioniser");
            if (regioniserField == null) return null;
            regioniserField.setAccessible(true);
            Object engine = regioniserField.get(worldServer);
            if (engine == null) return null;

            Location loc = player.getLocation();
            int sectionX = (loc.getBlockX() >> 4) >> regionChunkShift;
            int sectionZ = (loc.getBlockZ() >> 4) >> regionChunkShift;
            long targetSection = (((long) sectionZ) << 32) | (sectionX & 0xFFFFFFFFL);

            Object region = findOwningRegion(engine, targetSection);
            if (region == null) return null;

            return readTpsAndMspt(region);
        } catch (Throwable t) {
            return null;
        }
    }

    private static Object findOwningRegion(Object engine, long targetSection) throws Exception {
        Object[] found = new Object[1];
        Method compute = engine.getClass().getMethod("computeForAllRegions", Consumer.class);
        compute.invoke(engine, (Consumer<Object>) region -> {
            if (found[0] != null) return;
            try {
                Method getOwned = region.getClass().getMethod("getOwnedSections");
                @SuppressWarnings("unchecked")
                List<Long> owned = (List<Long>) getOwned.invoke(region);
                if (owned.contains(targetSection)) {
                    found[0] = region;
                }
            } catch (Throwable ignored) {
            }
        });
        return found[0];
    }

    private static double[] readTpsAndMspt(Object region) throws Exception {
        Object data = region.getClass().getMethod("getData").invoke(region);
        Object handle = data.getClass().getMethod("getRegionSchedulingHandle").invoke(data);
        Object report = handle.getClass().getMethod("getTickReport5s", long.class)
                .invoke(handle, System.nanoTime());
        if (report == null) return null;

        Object tpsData = report.getClass().getMethod("tpsData").invoke(report);
        Object tpsSegment = tpsData.getClass().getMethod("segmentAll").invoke(tpsData);
        double tps = (double) tpsSegment.getClass().getMethod("average").invoke(tpsSegment);

        Object timeData = report.getClass().getMethod("timePerTickData").invoke(report);
        Object timeSegment = timeData.getClass().getMethod("segmentAll").invoke(timeData);
        double msptNanos = (double) timeSegment.getClass().getMethod("average").invoke(timeSegment);

        return new double[]{tps, msptNanos / 1_000_000.0};
    }

    private static synchronized void resolveRegionChunkShift() {
        if (shiftResolved) return;
        shiftResolved = true;
        try {
            Class<?> trClass = Class.forName("io.papermc.paper.threadedregions.TickRegions");
            Method getShift = trClass.getMethod("getRegionChunkShift");
            regionChunkShift = (int) getShift.invoke(null);
        } catch (Throwable ignored) {
        }
    }

    private static Field findField(Class<?> clazz, String name) {
        Class<?> current = clazz;
        while (current != null) {
            try {
                return current.getDeclaredField(name);
            } catch (NoSuchFieldException e) {
                current = current.getSuperclass();
            }
        }
        return null;
    }
}
