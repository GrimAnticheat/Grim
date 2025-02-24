package ac.grim.grimac.platform.bukkit.command;

import ac.grim.grimac.platform.api.command.AbstractPlayerSelectorParser;
import ac.grim.grimac.platform.api.command.PlayerSelector;
import org.incendo.cloud.bukkit.parser.selector.SinglePlayerSelectorParser;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.parser.ParserDescriptor;

import java.util.concurrent.CompletableFuture;

public class BukkitPlayerSelectorParser<C> extends AbstractPlayerSelectorParser<C> {

    @Override
    public ParserDescriptor<C, PlayerSelector> descriptor() {
        return super.createDescriptor();
    }

    @Override
    protected ParserDescriptor<C, ?> getPlatformSpecificDescriptor() {
        return SinglePlayerSelectorParser.singlePlayerSelectorParser();
    }

    @Override
    protected CompletableFuture<PlayerSelector> adaptToCommonSelector(CommandContext<C> context, Object platformSpecificSelector) {
        return CompletableFuture.completedFuture(new BukkitPlayerSelectorAdapter((org.incendo.cloud.bukkit.data.SinglePlayerSelector) platformSpecificSelector));
    }

}
