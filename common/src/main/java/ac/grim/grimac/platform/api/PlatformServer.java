package ac.grim.grimac.platform.api;

import ac.grim.grimac.platform.api.sender.Sender;
import org.jetbrains.annotations.NotNull;

public interface PlatformServer {

    String getPlatformImplementationString();

    void dispatchCommand(Sender sender, String command);

    Sender getConsoleSender();

    void registerOutgoingPluginChannel(@NotNull String name);

    double getTPS();
}
