package ac.grim.grimac.checks.impl.pingspoof;

import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientKeepAlive;

// Frequency BadPacketsP
public class PingSpoofA extends PacketCheck {
    int lastId = -1;
    int lastSendID = -1;

    public PingSpoofA(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.KEEP_ALIVE) {
            WrapperPlayClientKeepAlive packet = new WrapperPlayClientKeepAlive(event);

            // TODO: Refine this into separate checks
            if (lastId == packet.getId()) {
                //flag();
            } else {
                //reward();
            }
        }
    }
}
