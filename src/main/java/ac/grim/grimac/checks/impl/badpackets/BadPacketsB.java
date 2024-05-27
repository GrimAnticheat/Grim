package ac.grim.grimac.checks.impl.badpackets;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientSteerVehicle;

@CheckData(name = "BadPacketsB")
public class BadPacketsB extends Check implements PacketCheck {
    public BadPacketsB(final GrimPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(final PacketReceiveEvent event) {
        if (event.getPacketType() != PacketType.Play.Client.STEER_VEHICLE) return;

        final WrapperPlayClientSteerVehicle packet = new WrapperPlayClientSteerVehicle(event);

        final float forwards = Math.abs(packet.getForward());
        final float sideways = Math.abs(packet.getSideways());

        if (forwards > 0.98f || sideways > 0.98f) {
            flagAndAlert();
        }
    }
}
