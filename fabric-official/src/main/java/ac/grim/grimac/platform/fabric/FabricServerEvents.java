package ac.grim.grimac.platform.fabric;

import net.minecraft.server.MinecraftServer;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

// Local shim for fabric-api's ServerLifecycleEvents + ServerTickEvents, driven by the
// registered MinecraftServerMixin (avoids a hard fabric-api dependency for two hooks).
public final class FabricServerEvents {

    private FabricServerEvents() {}

    private static final List<Consumer<MinecraftServer>> STARTING = new ArrayList<>();
    private static final List<Consumer<MinecraftServer>> STOPPING = new ArrayList<>();
    private static final List<Consumer<MinecraftServer>> END_TICK = new ArrayList<>();

    public static void onServerStarting(Consumer<MinecraftServer> listener) {
        STARTING.add(listener);
    }

    public static void onServerStopping(Consumer<MinecraftServer> listener) {
        STOPPING.add(listener);
    }

    public static void onEndTick(Consumer<MinecraftServer> listener) {
        END_TICK.add(listener);
    }

    public static void fireServerStarting(MinecraftServer server) {
        for (Consumer<MinecraftServer> l : STARTING) l.accept(server);
    }

    public static void fireServerStopping(MinecraftServer server) {
        for (Consumer<MinecraftServer> l : STOPPING) l.accept(server);
    }

    public static void fireEndTick(MinecraftServer server) {
        for (Consumer<MinecraftServer> l : END_TICK) l.accept(server);
    }
}
