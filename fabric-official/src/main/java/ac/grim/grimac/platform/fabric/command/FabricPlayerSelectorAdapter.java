package ac.grim.grimac.platform.fabric.command;

import ac.grim.grimac.platform.api.command.PlayerSelector;
import ac.grim.grimac.platform.api.sender.Sender;
import ac.grim.grimac.platform.fabric.GrimACFabricLoaderPlugin;
import org.incendo.cloud.minecraft.modded.data.SinglePlayerSelector;

import java.util.Collection;
import java.util.Collections;

// Adapts cloud's SinglePlayerSelector to Grim's PlayerSelector. fabric-official targets a
// single MC family, so (unlike the intermediary per-yarn-version split) it lives here. Uses
// the 1.21.2+ form single().createCommandSourceStack() (official mappings, no remap).
public class FabricPlayerSelectorAdapter implements PlayerSelector {
    private final SinglePlayerSelector fabricSelector;

    public FabricPlayerSelectorAdapter(SinglePlayerSelector fabricSelector) {
        this.fabricSelector = fabricSelector;
    }

    @Override
    public boolean isSingle() {
        return true;
    }

    @Override
    public Sender getSinglePlayer() {
        return GrimACFabricLoaderPlugin.LOADER.getFabricSenderFactory()
                .wrap(fabricSelector.single().createCommandSourceStack());
    }

    @Override
    public Collection<Sender> getPlayers() {
        return Collections.singletonList(getSinglePlayer());
    }

    @Override
    public String inputString() {
        return fabricSelector.inputString();
    }
}
