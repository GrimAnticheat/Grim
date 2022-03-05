package ac.grim.grimac.checks.impl.disabler;

import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientClientStatus;

@CheckData(name = "DisablerD")
public class DisablerD extends PacketCheck {
    public DisablerD(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.CLIENT_STATUS) {
            WrapperPlayClientClientStatus packet = new WrapperPlayClientClientStatus(event);

            if (packet.getAction() == WrapperPlayClientClientStatus.Action.PERFORM_RESPAWN) {
                if (player.isDead) {
                    reward();
                } else {
                    flagAndAlert();
                }
            }
        }
    }
}
