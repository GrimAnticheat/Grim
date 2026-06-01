package ac.grim.grimac.platform.fabric.command;

import ac.grim.grimac.platform.api.command.PlayerSelector;
import ac.grim.grimac.platform.api.sender.Sender;
import ac.grim.grimac.platform.fabric.GrimACFabricLoaderPlugin;
import org.incendo.cloud.minecraft.modded.data.SinglePlayerSelector;

import java.util.Collection;
import java.util.Collections;

// 26.X mojmap adapter from cloud's SinglePlayerSelector to Grim's PlayerSelector.
// fabric-official targets a single MC family (26.1), so unlike fabric-intermediary
// (which splits this per yarn version) the adapter lives in the main module. Uses the
// 1.21.2+ form fabricSelector.single().createCommandSourceStack(): single() returns the
// mojmap net.minecraft.server.level.ServerPlayer and createCommandSourceStack() is a
// mojmap MC method on 26.1, so this resolves on the empty-stub classpath with no remap.
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
