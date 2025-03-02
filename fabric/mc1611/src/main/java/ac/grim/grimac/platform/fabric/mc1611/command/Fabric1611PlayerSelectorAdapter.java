package ac.grim.grimac.platform.fabric.mc1611.command;


import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.platform.api.sender.Sender;
import ac.grim.grimac.platform.fabric.command.FabricPlayerSelectorAdapter;
import ac.grim.grimac.platform.fabric.mc1611.sender.Fabric1611SenderFactory;
import org.incendo.cloud.minecraft.modded.data.SinglePlayerSelector;

public class Fabric1611PlayerSelectorAdapter extends FabricPlayerSelectorAdapter {

    public Fabric1611PlayerSelectorAdapter(SinglePlayerSelector fabricSelector) {
        super(fabricSelector);
    }

    @Override
    public Sender getSinglePlayer() {
        return ((Fabric1611SenderFactory) GrimAPI.INSTANCE.getSenderFactory()).map(fabricSelector.single().getCommandSource());
    }
}
