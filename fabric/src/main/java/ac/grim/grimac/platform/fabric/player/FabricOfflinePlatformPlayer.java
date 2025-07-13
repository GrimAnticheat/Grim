package ac.grim.grimac.platform.fabric.player;

import ac.grim.grimac.platform.api.player.OfflinePlatformPlayer;
import ac.grim.grimac.platform.fabric.GrimACFabricLoaderPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class FabricOfflinePlatformPlayer implements OfflinePlatformPlayer {
    private final UUID uuid;
    private final String name;

    public FabricOfflinePlatformPlayer(@NotNull UUID uuid, @NotNull String name) {
        this.uuid = uuid;
        this.name = name;
    }

    @Override
    public boolean isOnline() {
        return GrimACFabricLoaderPlugin.FABRIC_SERVER.getPlayerManager().getPlayer(uuid) != null;
    }

    @Override
    public @NotNull String getName() {
        return name;
    }

    @Override
    public @NotNull UUID getUniqueId() {
        return uuid;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof OfflinePlatformPlayer offlinePlatformPlayer) {
            return this.getUniqueId().equals(offlinePlatformPlayer.getUniqueId());
        }
        return false;
    }
}
