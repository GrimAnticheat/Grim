package ac.grim.grimac.platform.fabric.command;

import ac.grim.grimac.platform.api.command.PlayerSelector;
import ac.grim.grimac.platform.api.sender.Sender;
import lombok.RequiredArgsConstructor;
import org.incendo.cloud.minecraft.modded.data.SinglePlayerSelector;
import org.incendo.cloud.minecraft.modded.parser.VanillaArgumentParsers;
import org.incendo.cloud.parser.ParserDescriptor;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

@RequiredArgsConstructor
public class FabricPlayerSelectorParser<C> {

    private final Function<SinglePlayerSelector, Sender> singleResolver;
    private final Function<SinglePlayerSelector, String> inputResolver;

    public ParserDescriptor<C, PlayerSelector> descriptor() {
        ParserDescriptor<C, ?> descriptor = VanillaArgumentParsers.singlePlayerSelectorParser();
        return ParserDescriptor.of(
                descriptor.parser().mapSuccess((context, selector) -> CompletableFuture.completedFuture(
                        new FabricPlayerSelectorAdapter((SinglePlayerSelector) selector, singleResolver, inputResolver)
                )),
                PlayerSelector.class
        );
    }
}
