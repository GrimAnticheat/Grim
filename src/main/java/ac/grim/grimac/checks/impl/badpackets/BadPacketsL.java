package ac.grim.grimac.checks.impl.badpackets;

import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.DiggingAction;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging;

//checks for impossible dig packets
@CheckData(name = "BadPacketsL")
public class BadPacketsL extends PacketCheck {

    public BadPacketsL(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.PLAYER_DIGGING) {
            WrapperPlayClientPlayerDigging packet = new WrapperPlayClientPlayerDigging(event);
            if (packet.getAction() == DiggingAction.RELEASE_USE_ITEM) {
                switch (packet.getFace()) {
                    case UP:
                    case NORTH:
                    case EAST:
                    case WEST: {
                        flagAndAlert();
                    }
                }
            }
        }
    }
}
