package ac.grim.grimac.platform.fabric.player;

import ac.grim.grimac.platform.api.player.OfflinePlatformPlayer;
import ac.grim.grimac.platform.fabric.GrimACFabricLoaderPlugin;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

@RequiredArgsConstructor
@Getter
public class FabricOfflinePlatformPlayer implements OfflinePlatformPlayer {
    private final @NotNull UUID uniqueId;
    private final @NotNull String name;

    @Override
    public boolean isOnline() {
        return GrimACFabricLoaderPlugin.FABRIC_SERVER.getPlayerList().getPlayer(uniqueId) != null;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof OfflinePlatformPlayer player && this.getUniqueId().equals(player.getUniqueId());
    }
}
