package ac.grim.grimac.platform.api.player;

import java.util.Collection;
import java.util.UUID;

public interface PlatformPlayerFactory {
    OfflinePlatformPlayer getOfflineFromUUID(UUID uuid);

    OfflinePlatformPlayer getOfflineFromName(String name);

    Collection<OfflinePlatformPlayer> getOfflinePlayers();

    PlatformPlayer getFromName(String name);

    PlatformPlayer getFromUUID(UUID uuid);

    PlatformPlayer getFromNativePlayerType(Object playerObject);

    void invalidatePlayer(UUID uuid);

    Collection<PlatformPlayer> getOnlinePlayers();
}
