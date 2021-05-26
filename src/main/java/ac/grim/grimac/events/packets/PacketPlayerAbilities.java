package ac.grim.grimac.events.packets;

import ac.grim.grimac.GrimAC;
import ac.grim.grimac.player.GrimPlayer;
import io.github.retrooper.packetevents.event.PacketListenerDynamic;
import io.github.retrooper.packetevents.event.impl.PacketPlayReceiveEvent;
import io.github.retrooper.packetevents.event.impl.PacketPlaySendEvent;
import io.github.retrooper.packetevents.packettype.PacketType;
import io.github.retrooper.packetevents.packetwrappers.play.in.abilities.WrappedPacketInAbilities;
import io.github.retrooper.packetevents.packetwrappers.play.out.abilities.WrappedPacketOutAbilities;

public class PacketPlayerAbilities extends PacketListenerDynamic {

    @Override
    public void onPacketPlayReceive(PacketPlayReceiveEvent event) {
        if (event.getPacketId() == PacketType.Play.Client.ABILITIES) {
            WrappedPacketInAbilities action = new WrappedPacketInAbilities(event.getNMSPacket());
            GrimPlayer player = GrimAC.playerGrimHashMap.get(event.getPlayer());
            if (player == null) return;

            player.packetFlyingDanger = action.isFlying();
        }
    }

    @Override
    public void onPacketPlaySend(PacketPlaySendEvent event) {
        if (event.getPacketId() == PacketType.Play.Server.ABILITIES) {
            WrappedPacketOutAbilities abilities = new WrappedPacketOutAbilities(event.getNMSPacket());
            GrimPlayer player = GrimAC.playerGrimHashMap.get(event.getPlayer());

            // Occurs on login - we set if the player can fly on PlayerJoinEvent
            if (player == null) return;

            player.compensatedFlying.setCanPlayerFly(abilities.isFlightAllowed());
        }
    }
}
