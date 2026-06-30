package ac.grim.grimac.platform.fabric.utils.message;

import ac.grim.grimac.platform.api.sender.Sender;

public interface IFabricMessageUtil {
    Object textLiteral(String message);

    void sendMessage(Sender target, Object message, boolean overlay);
}
