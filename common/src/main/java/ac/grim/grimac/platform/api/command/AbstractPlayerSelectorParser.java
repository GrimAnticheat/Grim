package ac.grim.grimac.platform.api.command;

import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.parser.ParserDescriptor;

import java.util.concurrent.CompletableFuture;

public abstract class AbstractPlayerSelectorParser<C> {

    public abstract ParserDescriptor<C, PlayerSelector> descriptor();

    protected abstract ParserDescriptor<C, ?> getPlatformSpecificDescriptor();

    protected abstract CompletableFuture<PlayerSelector> adaptToCommonSelector(CommandContext<C> context, Object platformSpecificSelector);

    // Helper method to create the ParserDescriptor
    protected ParserDescriptor<C, PlayerSelector> createDescriptor() {
        return ParserDescriptor.of(
                getPlatformSpecificDescriptor().parser().mapSuccess(this::adaptToCommonSelector),
                PlayerSelector.class
        );
    }
}
