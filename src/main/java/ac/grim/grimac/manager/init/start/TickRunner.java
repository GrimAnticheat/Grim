package ac.grim.grimac.manager.init.start;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.manager.init.Initable;
import ac.grim.grimac.utils.anticheat.LogUtil;
import io.github.retrooper.packetevents.util.folia.FoliaScheduler;

public class TickRunner implements Initable {
    @Override
    public void start() {
        LogUtil.info("Registering tick schedulers...");

        if (FoliaScheduler.isFolia()) {
            GrimAPI.INSTANCE.getScheduler().getAsyncScheduler().runAtFixedRate(GrimAPI.INSTANCE.getGrimPlugin(), () -> {
                GrimAPI.INSTANCE.getTickManager().tickSync();
                GrimAPI.INSTANCE.getTickManager().tickAsync();
            }, 1, 1);
        } else {
            GrimAPI.INSTANCE.getScheduler().getGlobalRegionScheduler().runAtFixedRate(GrimAPI.INSTANCE.getGrimPlugin(), () -> GrimAPI.INSTANCE.getTickManager().tickSync(), 0, 1);
            GrimAPI.INSTANCE.getScheduler().getAsyncScheduler().runAtFixedRate(GrimAPI.INSTANCE.getGrimPlugin(), () -> GrimAPI.INSTANCE.getTickManager().tickAsync(), 0, 1);
        }
    }
}
