
package ac.grim.grimac.utils.chunks;

import com.github.retrooper.packetevents.util.reflection.Reflection;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.function.Consumer;

// this class should be merged to packetevents upstream class
public class FoliaHelper {
    private static boolean folia;

    static {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            folia = true;
        } catch (ClassNotFoundException e) {
            folia = false;
        }
    }

    /**
     * Run a task in a region
     * @param plugin The plugin instance this task will be for
     * @param world The world this region is in
     * @param chunkX The chunk x that is in said region
     * @param chunkZ The chunk z that is in said region
     * @param task The task lambda.
     */
    public static void runTaskInRegion(final Plugin plugin, final World world, final int chunkX, final int chunkZ, final Consumer<Object> task) {
        if (!folia) {
            Bukkit.getScheduler().runTask(plugin, () -> task.accept(null));
            return;
        }
        try {
            Method getSchedulerMethod = Reflection.getMethod(Server.class, "getRegionScheduler", 0);
            Object regionScheduler = getSchedulerMethod.invoke(Bukkit.getServer());

            Class<?> schedulerClass = regionScheduler.getClass().getInterfaces()[0];
            Method executeMethod = schedulerClass.getDeclaredMethod("run", Plugin.class, World.class, int.class, int.class, Consumer.class);

            executeMethod.invoke(regionScheduler, plugin, world, chunkX, chunkZ, task);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
