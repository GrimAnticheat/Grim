package ac.grim.grimac.manager.init.start;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.manager.init.Initable;
import ac.grim.grimac.utils.anticheat.LogUtil;
import org.bukkit.Bukkit;

public class TickRunner implements Initable {
    @Override
    public void start() {
        LogUtil.info("Registering tick schedulers...");

        Bukkit.getScheduler().runTaskTimer(GrimAPI.INSTANCE.getPlugin(), () -> GrimAPI.INSTANCE.getTickManager().tickSync(), 0, 1);
        Bukkit.getScheduler().runTaskTimerAsynchronously(GrimAPI.INSTANCE.getPlugin(), () -> GrimAPI.INSTANCE.getTickManager().tickAsync(), 0, 1);
    }
}
