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

// 26.X mojmap port of fabric-intermediary's command.FabricPlayerSelectorParser (verbatim).
// cloud-minecraft-modded's VanillaArgumentParsers + SinglePlayerSelector are mojmap on 26.1
// (SinglePlayerSelector extends Selector.Single<net.minecraft.server.level.ServerPlayer>), so
// this resolves on the empty-stub mojmap classpath with no remap.
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
