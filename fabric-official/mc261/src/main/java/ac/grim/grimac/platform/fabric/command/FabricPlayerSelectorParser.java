package ac.grim.grimac.platform.fabric.command;

import ac.grim.grimac.platform.api.command.AbstractPlayerSelectorParser;
import ac.grim.grimac.platform.api.command.PlayerSelector;
import lombok.RequiredArgsConstructor;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.minecraft.modded.data.SinglePlayerSelector;
import org.incendo.cloud.minecraft.modded.parser.VanillaArgumentParsers;
import org.incendo.cloud.parser.ParserDescriptor;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

@RequiredArgsConstructor
public class FabricPlayerSelectorParser<C> extends AbstractPlayerSelectorParser<C> {

    private final Function<SinglePlayerSelector, PlayerSelector> selectorSupplier;

    @Override
    public ParserDescriptor<C, PlayerSelector> descriptor() {
        return createDescriptor();
    }

    @Override
    protected ParserDescriptor<C, ?> getPlatformSpecificDescriptor() {
        return VanillaArgumentParsers.singlePlayerSelectorParser();
    }

    @Override
    protected CompletableFuture<PlayerSelector> adaptToCommonSelector(CommandContext<C> context, Object platformSpecificSelector) {
        return CompletableFuture.completedFuture(
                selectorSupplier.apply((SinglePlayerSelector) platformSpecificSelector)
        );
    }
}
