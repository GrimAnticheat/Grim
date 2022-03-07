package ac.grim.grimac.checks.impl.badpackets;

import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrappedPacketInUseEntity;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;

@CheckData(name = "BadPacketsH")
public class BadPacketsH extends PacketCheck {
    private boolean swung;

    public BadPacketsH(final GrimPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.USE_ENTITY) {
            WrappedPacketInUseEntity packet = new WrappedPacketInUseEntity(event);

            if (packet.getAction() != WrappedPacketInUseEntity.EntityUseAction.ATTACK) return;
			
	    if (!swung) flagAndAlert();
            swung = false;
	} else if (event.getPacketType() == PacketType.Play.Client.ANIMATION) {
	    swung = true;
	} else if (WrapperPlayClientPlayerFlying.isFlying(event.getPacket()) {
	    swung = false;
	}
      }
    }
}
		   
