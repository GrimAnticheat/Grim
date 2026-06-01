package ac.grim.grimac.platform.fabric.mc1161.util.convert;

import ac.grim.grimac.platform.fabric.utils.message.IFabricMessageUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerPlayer;

public class Fabric1161MessageUtil implements IFabricMessageUtil {
    @Override
    public Component textLiteral(String message) {
        return new TextComponent(message);
    }

    @Override
    public void sendMessage(CommandSourceStack target, Component message, boolean overlay) {
        target.sendSuccess(message, overlay);
    }

    @Override
    public void sendSystemMessageToPlayer(ServerPlayer player, Component nativeText) {
        // intermediary mapping (base for 1.16.1 .. 1.21.11 via Fabric1190/1200 subclasses):
        // the player wrapper's original call, preserved verbatim.
        player.displayClientMessage(nativeText, false);
    }
}
