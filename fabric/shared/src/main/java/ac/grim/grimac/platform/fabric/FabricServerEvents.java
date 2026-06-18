package ac.grim.grimac.platform.fabric;

import ac.grim.grimac.platform.fabric.inject.FabricMinecraftServerHandle;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class FabricServerEvents {

    private FabricServerEvents() {}

    private static final List<Consumer<FabricMinecraftServerHandle>> STARTING = new ArrayList<>();
    private static final List<Consumer<FabricMinecraftServerHandle>> STOPPING = new ArrayList<>();
    private static final List<Consumer<FabricMinecraftServerHandle>> END_TICK = new ArrayList<>();

    public static void onServerStarting(Consumer<FabricMinecraftServerHandle> listener) {
        STARTING.add(listener);
    }

    public static void onServerStopping(Consumer<FabricMinecraftServerHandle> listener) {
        STOPPING.add(listener);
    }

    public static void onEndTick(Consumer<FabricMinecraftServerHandle> listener) {
        END_TICK.add(listener);
    }

    public static void fireServerStarting(FabricMinecraftServerHandle server) {
        for (Consumer<FabricMinecraftServerHandle> listener : STARTING) listener.accept(server);
    }

    public static void fireServerStopping(FabricMinecraftServerHandle server) {
        for (Consumer<FabricMinecraftServerHandle> listener : STOPPING) listener.accept(server);
    }

    public static void fireEndTick(FabricMinecraftServerHandle server) {
        for (Consumer<FabricMinecraftServerHandle> listener : END_TICK) listener.accept(server);
    }
}
