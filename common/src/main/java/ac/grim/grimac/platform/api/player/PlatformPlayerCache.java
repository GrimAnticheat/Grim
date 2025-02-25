package ac.grim.grimac.platform.api.player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlatformPlayerCache {
    private static final PlatformPlayerCache INSTANCE = new PlatformPlayerCache();
    private final Map<UUID, PlatformPlayer> playerCache = new ConcurrentHashMap<>();

    private PlatformPlayerCache() {
        // Private constructor to prevent instantiation
    }

    public static PlatformPlayerCache getInstance() {
        return INSTANCE;
    }

    /**
     * Adds or updates a PlatformPlayer in the cache.
     *
     * @param uuid   the UUID of the player
     * @param player the PlatformPlayer instance
     * @return the cached PlatformPlayer instance
     */
    public PlatformPlayer addOrGetPlayer(UUID uuid, PlatformPlayer player) {
        return playerCache.compute(uuid, (key, existing) -> {
            if (existing != null) {
                return existing; // Return existing instance if already cached
            }
            return player;
        });
    }

    /**
     * Removes a player from the cache.
     *
     * @param uuid the UUID of the player to remove
     */
    public void removePlayer(UUID uuid) {
        playerCache.remove(uuid);
    }

    /**
     * Gets a PlatformPlayer from the cache by UUID.
     *
     * @param uuid the UUID of the player
     * @return the cached PlatformPlayer, or null if not found
     */
    public PlatformPlayer getPlayer(UUID uuid) {
        return playerCache.get(uuid);
    }
}
