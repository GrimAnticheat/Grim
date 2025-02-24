package ac.grim.grimac.commands;

import ac.grim.grimac.platform.api.sender.Sender;
import org.incendo.cloud.CommandManager;

public interface BuildableCommand {

    void register(CommandManager<Sender> manager);
}
