package ac.grim.grimac.platform.fabric.mc1216.command;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.platform.api.sender.Sender;
import ac.grim.grimac.platform.fabric.mc1161.command.Fabric1161PlayerSelectorAdapter;
import ac.grim.grimac.platform.fabric.sender.FabricSenderFactory;
import net.minecraft.commands.CommandSourceStack;
import org.incendo.cloud.minecraft.modded.data.SinglePlayerSelector;

public class Fabric1212PlayerSelectorAdapter extends Fabric1161PlayerSelectorAdapter {

    public Fabric1212PlayerSelectorAdapter(SinglePlayerSelector fabricSelector) {
        super(fabricSelector);
    }

    // 1.21.2 .getCommandSource() moves from entity to player
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
}
