package ac.grim.grimac.platform.fabric.loader;

import com.github.retrooper.packetevents.manager.server.ServerVersion;

// fabric-official's chain interface intentionally diverges from fabric-intermediary's
// GrimACFabricLoaderPlugin contract: that abstract carries MC type references
// (CommandSourceStack, MinecraftServer) which require Mojang-named compile-time
// sources. mc261 entrypoints register their native version here and grow into a
// full loader once 26.X-mapped sources are available.
public interface Grim26ChainEntryPoint {
    ServerVersion getNativeVersion();
}
