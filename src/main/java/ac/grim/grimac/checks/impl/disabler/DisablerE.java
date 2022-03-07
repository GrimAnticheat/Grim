package ac.grim.grimac.checks.impl.disabler;

import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientHeldItemChange;

@CheckData(name = "DisablerE")
public class DisablerE extends PacketCheck {
    private int lastId = -1;
  
    public DisablerE(final GrimPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.KEEP_ALIVE) {
            WrapperPlayClientKeepAlive packet = new WrapperPlayClientKeepAlive(event);
            long id = packet.getId();
          
            if (id == lastId || id == 0L) {
                flagAndAlert();
            } else {
                reward();
            }
          
            lastId = id;
        }
    }
}
