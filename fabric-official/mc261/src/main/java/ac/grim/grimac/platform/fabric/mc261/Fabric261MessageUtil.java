package ac.grim.grimac.platform.fabric.mc261;

import ac.grim.grimac.platform.fabric.utils.message.IFabricMessageUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class Fabric261MessageUtil implements IFabricMessageUtil {
    @Override
    public Component textLiteral(String message) {
        return Component.literal(message);
    }

    @Override
    public void sendMessage(CommandSourceStack target, Component message, boolean overlay) {
        target.sendSuccess(() -> message, overlay);
    }

    @Override
    public void sendSystemMessageToPlayer(ServerPlayer player, Component nativeText) {
        // official/26.x mapping: the player wrapper's original call, preserved verbatim.
        player.sendSystemMessage(nativeText, false);
    }
}
