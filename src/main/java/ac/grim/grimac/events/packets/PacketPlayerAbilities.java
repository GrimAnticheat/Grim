package ac.grim.grimac.events.packets;

import ac.grim.grimac.GrimAC;
import ac.grim.grimac.player.GrimPlayer;
import io.github.retrooper.packetevents.event.PacketListenerAbstract;
import io.github.retrooper.packetevents.event.impl.PacketPlayReceiveEvent;
import io.github.retrooper.packetevents.event.impl.PacketPlaySendEvent;
import io.github.retrooper.packetevents.packettype.PacketType;
import io.github.retrooper.packetevents.packetwrappers.play.in.abilities.WrappedPacketInAbilities;
import io.github.retrooper.packetevents.packetwrappers.play.out.abilities.WrappedPacketOutAbilities;

public class PacketPlayerAbilities extends PacketListenerAbstract {

    @Override
    public void onPacketPlayReceive(PacketPlayReceiveEvent event) {
        if (event.getPacketId() == PacketType.Play.Client.ABILITIES) {
            WrappedPacketInAbilities abilities = new WrappedPacketInAbilities(event.getNMSPacket());
            GrimPlayer player = GrimAC.playerGrimHashMap.get(event.getPlayer());
            if (player == null) return;

            player.compensatedFlying.lagCompensatedIsFlyingMap.put(player.packetStateData.packetLastTransactionReceived, abilities.isFlying());
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
            player.compensatedFlying.lagCompensatedIsFlyingMap.put(player.lastTransactionSent.get(), abilities.isFlying());

            event.setPostTask(player::sendTransactionOrPingPong);
        }
    }
}
