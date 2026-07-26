package ac.grim.grimac.platform.minestom.player;

import ac.grim.grimac.platform.api.player.AbstractPlatformPlayerFactory;
import ac.grim.grimac.platform.api.player.OfflinePlatformPlayer;
import ac.grim.grimac.platform.api.player.PlatformPlayer;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.network.ConnectionManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Grim {@link AbstractPlatformPlayerFactory} over Minestom {@link Player}s. The abstract base
 * handles caching/wrapping; this supplies the native-player lookups.
 */
public final class MinestomPlatformPlayerFactory extends AbstractPlatformPlayerFactory<Player> {

    private static ConnectionManager connections() {
        return MinecraftServer.getConnectionManager();
    }

    @Override
    protected @Nullable Player getNativePlayer(@NotNull UUID uuid) {
        return connections().getOnlinePlayerByUuid(uuid);
    }

    @Override
    protected @Nullable Player getNativePlayer(@NotNull String name) {
        return connections().getOnlinePlayerByUsername(name);
    }

    @Override
    protected PlatformPlayer createPlatformPlayer(@NotNull Player nativePlayer) {
        return new MinestomPlatformPlayer(nativePlayer);
    }

    @Override
    protected UUID getPlayerUUID(@NotNull Player nativePlayer) {
        return nativePlayer.getUuid();
    }

    @Override
    protected Collection<Player> getNativeOnlinePlayers() {
        return connections().getOnlinePlayers();
    }

    @Override
    public OfflinePlatformPlayer getOfflineFromUUID(@NotNull UUID uuid) {
        Player online = connections().getOnlinePlayerByUuid(uuid);
        return new MinestomOfflinePlatformPlayer(uuid, online != null ? online.getUsername() : null);
    }

    @Override
    public OfflinePlatformPlayer getOfflineFromName(@NotNull String name) {
        Player online = connections().getOnlinePlayerByUsername(name);
        return new MinestomOfflinePlatformPlayer(online != null ? online.getUuid() : null, name);
    }

    @Override
    public Collection<OfflinePlatformPlayer> getOfflinePlayers() {
        // Minestom keeps no offline-player store; only live players are known.
        return List.of();
    }
}
