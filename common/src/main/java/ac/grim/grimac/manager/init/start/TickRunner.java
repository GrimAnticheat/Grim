package ac.grim.grimac.manager.init.start;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.manager.init.Initable;
import ac.grim.grimac.utils.anticheat.LogUtil;

public class TickRunner implements Initable {
    @Override
    public void start() {
        LogUtil.info("Registering tick schedulers...");

        if (GrimAPI.PLATFORM == GrimAPI.Platform.FOLIA) {
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
