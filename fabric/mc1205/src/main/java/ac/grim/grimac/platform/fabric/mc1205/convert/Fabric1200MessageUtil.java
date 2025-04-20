package ac.grim.grimac.platform.fabric.mc1205.convert;

import ac.grim.grimac.platform.fabric.mc1194.convert.Fabric1190MessageUtil;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

public class Fabric1200MessageUtil extends Fabric1190MessageUtil {
    @Override
    public void sendMessage(ServerCommandSource target, Text message, boolean overlay) {
        target.sendFeedback(() -> message, overlay);
    }
}
