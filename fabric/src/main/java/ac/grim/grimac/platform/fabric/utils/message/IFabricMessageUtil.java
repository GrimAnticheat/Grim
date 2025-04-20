package ac.grim.grimac.platform.fabric.utils.message;

import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

public interface IFabricMessageUtil {
    Text textLiteral(String message);
    void sendMessage(ServerCommandSource target, Text message, boolean overlay);
}
