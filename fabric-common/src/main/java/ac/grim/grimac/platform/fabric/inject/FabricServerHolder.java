package ac.grim.grimac.platform.fabric.inject;

/**
 * Holds the running {@code net.minecraft.server.MinecraftServer} so NMS-free fabric-common
 * code can reach it through the Loom-injected {@link FabricMinecraftServerHandle} without
 * referencing the per-version {@code GrimACFabricLoaderPlugin.FABRIC_SERVER} (a
 * {@code MinecraftServer} field this module cannot type).
 *
 * <p>The per-version entry point sets {@link #set(Object)} at the same lifecycle point it
 * assigns {@code GrimACFabricLoaderPlugin.FABRIC_SERVER} (server-starting). Stored as
 * {@code Object} (the NMS server) and exposed as the injected handle; {@code volatile}
 * because it is written on the server thread and read elsewhere.
 */
public final class FabricServerHolder {

    private FabricServerHolder() {}

    private static volatile Object server;

    /** @param minecraftServer the running {@code net.minecraft.server.MinecraftServer} */
    public static void set(Object minecraftServer) {
        server = minecraftServer;
    }

    /** @return the server as the injected handle, or {@code null} before server-starting */
    public static FabricMinecraftServerHandle handle() {
        return (FabricMinecraftServerHandle) server;
    }
}
