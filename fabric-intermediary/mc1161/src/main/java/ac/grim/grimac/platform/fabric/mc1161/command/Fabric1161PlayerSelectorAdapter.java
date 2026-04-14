package ac.grim.grimac.platform.fabric.mc1161.command;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.platform.api.command.PlayerSelector;
import ac.grim.grimac.platform.api.sender.Sender;
import ac.grim.grimac.platform.fabric.sender.FabricSenderFactory;
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
        return ((FabricSenderFactory) GrimAPI.INSTANCE.getSenderFactory()).wrap(fabricSelector.single().createCommandSourceStack());
    }

    @Override
    public Collection<Sender> getPlayers() {
        return Collections.singletonList(getSinglePlayer()); // Assuming your ServerPlayer can be cast to Player
    }

    @Override
    public String inputString() {
        return fabricSelector.inputString();
    }
}
