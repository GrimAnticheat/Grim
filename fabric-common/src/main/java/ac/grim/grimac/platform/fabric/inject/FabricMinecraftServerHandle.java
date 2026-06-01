package ac.grim.grimac.platform.fabric.inject;

import ac.grim.grimac.platform.api.sender.Sender;

import java.util.Collection;
import java.util.UUID;

public interface FabricMinecraftServerHandle {
    boolean isPlayerOnline(UUID uuid);

    FabricServerPlayerHandle playerByUuid(UUID uuid);

    FabricServerPlayerHandle playerByName(String name);

    Collection<FabricServerPlayerHandle> onlinePlayers();

    Collection<UUID> savedPlayerUuids();

    int getTickCount();

    String getServerVersion();

    Sender createCommandSender();

    boolean usesAuthentication();

    boolean isRunning();

    int getPlayerCount();
}
