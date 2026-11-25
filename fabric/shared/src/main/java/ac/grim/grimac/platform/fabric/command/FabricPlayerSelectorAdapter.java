package ac.grim.grimac.platform.fabric.command;

import ac.grim.grimac.platform.api.command.PlayerSelector;
import ac.grim.grimac.platform.api.sender.Sender;
import org.incendo.cloud.minecraft.modded.data.SinglePlayerSelector;

import java.util.Collection;
import java.util.Collections;
import java.util.function.Function;

public class FabricPlayerSelectorAdapter implements PlayerSelector {
    private final SinglePlayerSelector fabricSelector;
    private final Function<SinglePlayerSelector, Sender> singleResolver;
    private final Function<SinglePlayerSelector, String> inputResolver;

    public FabricPlayerSelectorAdapter(
            SinglePlayerSelector fabricSelector,
            Function<SinglePlayerSelector, Sender> singleResolver,
            Function<SinglePlayerSelector, String> inputResolver
    ) {
        this.fabricSelector = fabricSelector;
        this.singleResolver = singleResolver;
        this.inputResolver = inputResolver;
    }

    @Override
    public boolean isSingle() {
        return true;
    }

    @Override
    public Sender getSinglePlayer() {
        return singleResolver.apply(fabricSelector);
    }

    @Override
    public Collection<Sender> getPlayers() {
        return Collections.singletonList(getSinglePlayer());
    }

    @Override
    public String inputString() {
        return inputResolver.apply(fabricSelector);
    }
}
