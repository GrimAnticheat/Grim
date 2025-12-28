package ac.grim.grimac.platform.fabric.utils.message;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

public interface IFabricMessageUtil {
    Component textLiteral(String message);
    void sendMessage(CommandSourceStack target, Component message, boolean overlay);
}
