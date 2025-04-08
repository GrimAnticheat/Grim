package ac.grim.grimac.checks.impl.crash;

import ac.grim.grimac.checks.AbstractPacketCheck;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.PacketHandlerRegistry;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientSettings;

@CheckData(name = "CrashE")
public class CrashE extends AbstractPacketCheck {

    public CrashE(GrimPlayer playerData) {
        super(playerData);
    }

    @Override
    protected void registerReceiveHandlers(PacketHandlerRegistry<PacketReceiveEvent> registry) {
        registry.registerHandler(event -> {
            WrapperPlayClientSettings wrapper = new WrapperPlayClientSettings(event);
            int viewDistance = wrapper.getViewDistance();
            if (viewDistance < 2) {
                flagAndAlert("distance=" + viewDistance);
                wrapper.setViewDistance(2);
            }
        }, PacketType.Play.Client.CLIENT_SETTINGS);
    }
}
