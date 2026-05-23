package ac.grim.grimac.platform.fabric.utils.thread;

import ac.grim.grimac.GrimAPI;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public class FabricFutureUtil {
    public static <U> CompletableFuture<U> supplySync(Supplier<U> entityTeleportSupplier) {
        CompletableFuture<U> ret = new CompletableFuture<>();
        GrimAPI.INSTANCE.getScheduler().getGlobalRegionScheduler().run(GrimAPI.INSTANCE.getGrimPlugin(),
                () -> ret.complete(entityTeleportSupplier.get()));
        return ret;
    }
}
