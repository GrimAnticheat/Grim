package ac.grim.bukkit.command.parsers;

import ac.grim.grimac.platform.api.command.PlayerSelector;
import ac.grim.grimac.platform.api.manager.ParserDescriptorFactory;
import ac.grim.grimac.platform.api.sender.Sender;
import org.incendo.cloud.parser.ParserDescriptor;

public class BukkitParserDescriptorFactory implements ParserDescriptorFactory {

    BukkitPlayerSelectorParser<Sender> bukkitPlayerSelectorParser = new BukkitPlayerSelectorParser<>();

    @Override
    public ParserDescriptor<Sender, PlayerSelector> getSinglePlayer() {
        return bukkitPlayerSelectorParser.descriptor();
    }
}
