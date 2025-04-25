package ac.grim.grimac.platform.api.manager;

import ac.grim.grimac.platform.api.command.PlayerSelector;
import ac.grim.grimac.platform.api.sender.Sender;
import org.incendo.cloud.parser.ParserDescriptor;

public interface ParserDescriptorFactory {
    ParserDescriptor<Sender, PlayerSelector> getSinglePlayer();
}
