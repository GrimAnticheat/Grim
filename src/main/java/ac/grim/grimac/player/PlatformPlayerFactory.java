package ac.grim.grimac.player;

import java.util.UUID;

public interface PlatformPlayerFactory {
    PlatformPlayer getFromUUID(UUID uuid);
}
