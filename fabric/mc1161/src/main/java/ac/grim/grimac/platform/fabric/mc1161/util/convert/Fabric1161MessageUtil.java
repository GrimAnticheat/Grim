package ac.grim.grimac.platform.fabric.mc1161.util.convert;

import ac.grim.grimac.platform.fabric.utils.message.IFabricMessageUtil;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;

public class Fabric1161MessageUtil implements IFabricMessageUtil {
    @Override
    public Text textLiteral(String message) {
        return new LiteralText(message);
    }

    @Override
    public void sendMessage(ServerCommandSource target, Text message, boolean overlay) {
        target.sendFeedback(message, overlay);
    }
}
