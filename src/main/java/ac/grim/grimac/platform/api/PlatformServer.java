package ac.grim.grimac.platform.api;

import ac.grim.grimac.platform.api.player.PlatformPlayer;
import ac.grim.grimac.platform.api.sender.Sender;

import java.util.Collection;
import java.util.UUID;

public interface PlatformServer {
    Collection<PlatformPlayer> getOnlinePlayers();

    PlatformPlayer getPlayer(UUID uuid);

    String getPlatformImplementationString();

    void dispatchCommand(Sender sender, String command);

    Sender getConsoleSender();
}
