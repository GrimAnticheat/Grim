package ac.grim.grimac.platform.api.player;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public abstract class AbstractPlatformPlayerFactory<T> implements PlatformPlayerFactory {
    protected final PlatformPlayerCache cache = PlatformPlayerCache.getInstance();

    @Override
    public final @Nullable PlatformPlayer getFromUUID(@NotNull UUID uuid) {
        // Check cache first
        PlatformPlayer cachedPlayer = cache.getPlayer(uuid);
        if (cachedPlayer != null) {
            return cachedPlayer;
        }

        // If not in cache, get the native player
        T nativePlayer = getNativePlayer(uuid);
        if (nativePlayer == null) {
            return null;
        }

        // Create new PlatformPlayer and cache it
        PlatformPlayer platformPlayer = createPlatformPlayer(nativePlayer);
        return cache.addOrGetPlayer(uuid, platformPlayer);
    }

    @Override
    public @Nullable PlatformPlayer getFromName(@NotNull String name) {
        T nativePlayer = getNativePlayer(name);
        if (nativePlayer == null) {
            return null;
        }

        // Create new PlatformPlayer and cache it
        PlatformPlayer platformPlayer = createPlatformPlayer(nativePlayer);
        return cache.addOrGetPlayer(platformPlayer.getUniqueId(), platformPlayer);
    }

    @SuppressWarnings("unchecked")
    @Override
    public final PlatformPlayer getFromNativePlayerType(@NotNull Object playerObject) {
        T nativePlayer = (T) Objects.requireNonNull(playerObject);
        UUID uuid = getPlayerUUID(nativePlayer);

        // Check cache first
        PlatformPlayer cachedPlayer = cache.getPlayer(uuid);
        if (cachedPlayer != null) {
            return cachedPlayer;
        }

        // Create new PlatformPlayer and cache it
        PlatformPlayer platformPlayer = createPlatformPlayer(nativePlayer);
        return cache.addOrGetPlayer(uuid, platformPlayer);
    }

    @Override
    public final void invalidatePlayer(@NotNull UUID uuid) {
        cache.removePlayer(uuid);
    }

    @Override
    public Collection<PlatformPlayer> getOnlinePlayers() {
        Collection<T> nativePlayers = getNativeOnlinePlayers();

        // Pre-allocate the list with the expected size to avoid resizing
        List<PlatformPlayer> platformPlayers = new ArrayList<>(nativePlayers.size());

        for (T nativePlayer : nativePlayers) {
            platformPlayers.add(getFromNativePlayerType(nativePlayer));
        }

        return platformPlayers;
    }

    public void replaceNativePlayer(@NotNull UUID uuid, @NotNull T player) {}

    /**
     * Retrieves the native player object for the given UUID.
     *
     * @param uuid the UUID of the player
     * @return the native player object, or null if not found
     */
    protected abstract T getNativePlayer(@NotNull UUID uuid);

    protected abstract T getNativePlayer(@NotNull String name);

    /**
     * Creates a PlatformPlayer instance from the native player object.
     *
     * @param nativePlayer the native player object
     * @return a new PlatformPlayer instance
     */
    protected abstract PlatformPlayer createPlatformPlayer(@NotNull T nativePlayer);

    /**
     * Gets the UUID of the native player.
     *
     * @param nativePlayer the native player object
     * @return the UUID of the player
     */
    protected abstract UUID getPlayerUUID(@NotNull T nativePlayer);

    /**
     * Gets the native online player objects (e.g., Player for Bukkit, ServerPlayerEntity for Fabric).
     *
     * @return a collection of native player objects
     */
    protected abstract Collection<T> getNativeOnlinePlayers();


    @Override
    public abstract OfflinePlatformPlayer getOfflineFromUUID(@NotNull UUID uuid);

    @Override
    public abstract OfflinePlatformPlayer getOfflineFromName(@NotNull String name);
}
