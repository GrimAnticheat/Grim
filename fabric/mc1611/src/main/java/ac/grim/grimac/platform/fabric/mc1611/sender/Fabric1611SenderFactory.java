package ac.grim.grimac.platform.fabric.mc1611.sender;

import ac.grim.grimac.platform.api.permissions.PermissionDefaultValue;
import ac.grim.grimac.platform.fabric.sender.FabricSenderFactory;
import net.kyori.adventure.text.Component;
import net.minecraft.server.command.CommandOutput;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.rcon.RconCommandOutput;
import net.minecraft.text.LiteralText;

import java.util.HashMap;
import java.util.Map;

public class Fabric1611SenderFactory extends FabricSenderFactory {

    private final Map<String, PermissionDefaultValue> permissionDefaults = new HashMap<>();

    @Override
    protected void sendMessage(ServerCommandSource sender, String message) {
        sender.sendFeedback(new LiteralText(message), false);
    }

    @Override
    protected void sendMessage(ServerCommandSource sender, Component message) {
        sender.sendFeedback(toNativeText(message), false);
    }

    @Override
    protected boolean isConsole(ServerCommandSource sender) {
        CommandOutput output = sender.output;
        return output == sender.getMinecraftServer() || // Console
                output.getClass() == RconCommandOutput.class || // Rcon
                (output == CommandOutput.DUMMY && sender.getName().equals("")); // Functions
    }
}
