package ac.grim.grimac.platform.fabric.mc1211.command;


import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.platform.api.sender.Sender;
import ac.grim.grimac.platform.fabric.command.FabricPlayerSelectorAdapter;
import ac.grim.grimac.platform.fabric.sender.FabricSenderFactory;
import org.incendo.cloud.minecraft.modded.data.SinglePlayerSelector;

public class Fabric1211PlayerSelectorAdapter extends FabricPlayerSelectorAdapter {

    public Fabric1211PlayerSelectorAdapter(SinglePlayerSelector fabricSelector) {
        super(fabricSelector);
    }

    @Override
    public Sender getSinglePlayer() {
        return ((FabricSenderFactory) GrimAPI.INSTANCE.getSenderFactory()).map(fabricSelector.single().getCommandSource());
    }
}
