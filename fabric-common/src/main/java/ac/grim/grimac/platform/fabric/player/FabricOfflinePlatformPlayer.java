package ac.grim.grimac.platform.fabric.player;

import ac.grim.grimac.platform.api.player.OfflinePlatformPlayer;
import ac.grim.grimac.platform.fabric.inject.FabricMinecraftServerHandle;
import ac.grim.grimac.platform.fabric.inject.FabricServerHolder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

// Single shared copy (lives in NMS-free fabric-common): isOnline() now reaches the server
// through the Loom-injected FabricMinecraftServerHandle (held NMS-free by FabricServerHolder),
// so it no longer needs MinecraftServer on the compile classpath nor the per-version
// GrimACFabricLoaderPlugin.FABRIC_SERVER. Written once instead of duplicated per aggregator.
@RequiredArgsConstructor
@Getter
public class FabricOfflinePlatformPlayer implements OfflinePlatformPlayer {
    private final @NotNull UUID uniqueId;
    private final @NotNull String username;

    @Override
    public boolean isOnline() {
        FabricMinecraftServerHandle server = FabricServerHolder.handle();
        return server != null && server.isPlayerOnline(uniqueId);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof OfflinePlatformPlayer player && this.getUniqueId().equals(player.getUniqueId());
    }
}
