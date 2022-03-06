package ac.grim.grimac.checks.impl.badpackets;

import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrappedPacketInClientCommand;

@CheckData(name = "BadPacketsH")
public class BadPacketsH extends PacketCheck {
    public BadPacketsH(final GrimPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.CLIENT_COMMAND) {
            WrappedPacketInClientCommand packet = new WrappedPacketInClientCommand(event);

            if (packet.getClientCommand() == WrappedPacketInClientCommand.ClientCommand.PERFORM_RESPAWN) {
                if (player.bukkitPlayer.getHealth() > 0.0D) {
                    flagAndAlert();
                }
            }

        }
    }
}
