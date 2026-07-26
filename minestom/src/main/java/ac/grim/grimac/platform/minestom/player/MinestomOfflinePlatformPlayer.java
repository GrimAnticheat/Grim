package ac.grim.grimac.platform.minestom.player;

import ac.grim.grimac.platform.api.player.OfflinePlatformPlayer;
import net.minestom.server.MinecraftServer;

import java.util.UUID;

/**
 * Grim {@link OfflinePlatformPlayer} for Minestom. Minestom keeps no offline-player store, so
 * "offline" here is just a uuid+name pair whose online state is checked live.
 */
public final class MinestomOfflinePlatformPlayer implements OfflinePlatformPlayer {

    private final UUID uuid;
    private final String name;

    public MinestomOfflinePlatformPlayer(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = name;
    }

    @Override
    public boolean isOnline() {
        return uuid != null && MinecraftServer.getConnectionManager().getOnlinePlayerByUuid(uuid) != null;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public UUID getUniqueId() {
        return uuid;
    }
}
