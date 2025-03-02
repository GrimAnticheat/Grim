package ac.grim.grimac.platform.fabric.manager;

import ac.grim.grimac.platform.api.command.PlayerSelector;
import ac.grim.grimac.platform.api.manager.ParserDescriptorFactory;
import ac.grim.grimac.platform.api.sender.Sender;
import ac.grim.grimac.platform.fabric.command.FabricPlayerSelectorParser;
import org.incendo.cloud.parser.ParserDescriptor;

public class FabricParserDescriptorFactory implements ParserDescriptorFactory {

    private final FabricPlayerSelectorParser<Sender> fabricPlayerSelectorParser;

    public FabricParserDescriptorFactory(FabricPlayerSelectorParser<Sender> fabricPlayerSelectorParser) {
        this.fabricPlayerSelectorParser = fabricPlayerSelectorParser;
    }

    @Override
    public ParserDescriptor<Sender, PlayerSelector> getSinglePlayer() {
        return fabricPlayerSelectorParser.descriptor();
    }
}
