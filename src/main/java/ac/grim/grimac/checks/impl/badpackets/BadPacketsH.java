package ac.grim.grimac.checks.impl.badpackets;

import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;
import io.github.retrooper.packetevents.PacketEvents;
import io.github.retrooper.packetevents.utils.server.ServerVersion;
import io.github.retrooper.packetevents.utils.player.ClientVersion;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrappedPacketInUseEntity;

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

            boolean exempt = PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_9) || 
			                 player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_9);

            if (packet.getAction() != WrappedPacketInUseEntity.EntityUseAction.ATTACK || exempt || swung) return;
			
			          flagAndAlert();
        } else if (event.getPacketType() == PacketType.Play.Client.ANIMATION) {
			      swung = true;
		    } else if (event.getPacketType() == PacketType.Play.Client.PLAYER_FLYING || 
		        event.getPacketType() == PacketType.Play.Client.PLAYER_POSITION || 
				    event.getPacketType() == PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION || 
				    event.getPacketType() == PacketType.Play.Client.PLAYER_ROTATION) {
			      swung = false;
		    }
    }
}

