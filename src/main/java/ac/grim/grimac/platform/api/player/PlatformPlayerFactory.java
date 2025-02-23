package ac.grim.grimac.platform.api.player;

import java.util.UUID;

public interface PlatformPlayerFactory {
    PlatformPlayer getFromUUID(UUID uuid);
}
