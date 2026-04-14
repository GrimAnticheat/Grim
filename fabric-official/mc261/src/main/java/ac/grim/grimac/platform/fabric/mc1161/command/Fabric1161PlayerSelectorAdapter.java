package ac.grim.grimac.platform.fabric.mc1161.command;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.platform.api.command.PlayerSelector;
import ac.grim.grimac.platform.api.sender.Sender;
import ac.grim.grimac.platform.fabric.sender.FabricSenderFactory;
import net.minecraft.commands.CommandSourceStack;
import org.incendo.cloud.minecraft.modded.data.SinglePlayerSelector;

import java.util.Collection;
import java.util.Collections;

public class Fabric1161PlayerSelectorAdapter implements PlayerSelector {
    protected final SinglePlayerSelector fabricSelector;

    public Fabric1161PlayerSelectorAdapter(SinglePlayerSelector fabricSelector) {
        this.fabricSelector = fabricSelector;
    }

    @Override
    public boolean isSingle() {
        return true;
    }

    @Override
    public Sender getSinglePlayer() {
        try {
            Object selectedPlayer = fabricSelector.getClass().getMethod("single").invoke(fabricSelector);
            Object sourceStack = selectedPlayer.getClass().getMethod("createCommandSourceStack").invoke(selectedPlayer);
            return ((FabricSenderFactory) GrimAPI.INSTANCE.getSenderFactory()).wrap((CommandSourceStack) sourceStack);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to resolve Fabric command selector", e);
        }
    }

    @Override
    public Collection<Sender> getPlayers() {
        return Collections.singletonList(getSinglePlayer());
    }

    @Override
    public String inputString() {
        try {
            return (String) fabricSelector.getClass().getMethod("inputString").invoke(fabricSelector);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to read Fabric selector input", e);
        }
    }
}
