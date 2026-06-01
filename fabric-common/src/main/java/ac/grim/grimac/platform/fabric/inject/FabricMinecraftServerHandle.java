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

    // These mirror vanilla MinecraftServer names. The ServerMixin @Interface uses
    // remap = NONE so the injected member names are NOT obf-remapped (a remapped name
    // wouldn't match this interface and would fail to apply on intermediary); the mixin
    // bodies' NMS calls are still remapped normally.
    int getTickCount();

    String getServerVersion();

    Sender createCommandSender();

    boolean usesAuthentication();

    boolean isRunning();

    int getPlayerCount();
}
