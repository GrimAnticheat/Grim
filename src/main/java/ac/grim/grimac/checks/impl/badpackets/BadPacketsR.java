package ac.grim.grimac.checks.impl.badpackets;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketType.Play.Client;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientEntityAction;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientEntityAction.Action;

@CheckData(name = "BadPacketsR")
public class BadPacketsR extends Check implements PacketCheck {
    public BadPacketsR(final GrimPlayer player) {
        super(player);
    }

    private long lastTransaction = 0;
    private int positions = 0;

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.WINDOW_CONFIRMATION ||
              event.getPacketType() == PacketType.Play.Client.PONG) {
            final long time = System.currentTimeMillis();
            final long diff = time - lastTransaction;

            if (diff > 1000) {
                if (positions == 0 && lastTransaction != 0) {
                    flagAndAlert("time=" + diff + " positions=" + positions);
                    player.compensatedWorld.removeInvalidPistonLikeStuff();
                } else {
                    reward();
                }
                positions = 0;
                lastTransaction = time;
            }
        }
        //
        if (event.getPacketType() == PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION ||
                event.getPacketType() == PacketType.Play.Client.PLAYER_POSITION
                 || event.getPacketType() == Client.STEER_VEHICLE) {
            positions++;
        }
    }
}
