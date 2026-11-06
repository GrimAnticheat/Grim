package ac.grim.grimac.platform.api;

import ac.grim.grimac.platform.api.sender.Sender;

import java.util.UUID;

public interface PlatformServer {

    String getPlatformImplementationString();

    void dispatchCommand(Sender sender, String command);

    Sender getConsoleSender();

    void registerOutgoingPluginChannel(String name);

    double getTPS();

    default double getTPS(UUID playerId) {
        return getTPS();
    }
}
