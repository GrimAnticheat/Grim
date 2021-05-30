package ac.grim.grimac.events.packets;

import ac.grim.grimac.GrimAC;
import ac.grim.grimac.player.GrimPlayer;
import io.github.retrooper.packetevents.PacketEvents;
import io.github.retrooper.packetevents.event.PacketListenerDynamic;
import io.github.retrooper.packetevents.event.impl.PacketPlayReceiveEvent;
import io.github.retrooper.packetevents.event.impl.PacketPlaySendEvent;
import io.github.retrooper.packetevents.packettype.PacketType;
import io.github.retrooper.packetevents.packetwrappers.play.in.abilities.WrappedPacketInAbilities;
import io.github.retrooper.packetevents.packetwrappers.play.out.abilities.WrappedPacketOutAbilities;
import io.github.retrooper.packetevents.packetwrappers.play.out.transaction.WrappedPacketOutTransaction;

public class PacketPlayerAbilities extends PacketListenerDynamic {

    @Override
    public void onPacketPlayReceive(PacketPlayReceiveEvent event) {
        if (event.getPacketId() == PacketType.Play.Client.ABILITIES) {
            WrappedPacketInAbilities abilities = new WrappedPacketInAbilities(event.getNMSPacket());
            GrimPlayer player = GrimAC.playerGrimHashMap.get(event.getPlayer());
            if (player == null) return;

            player.compensatedFlying.lagCompensatedIsFlyingMap.put(player.lastTransactionReceived, abilities.isFlying());
        }
    }

    @Override
    public void onPacketPlaySend(PacketPlaySendEvent event) {
        if (event.getPacketId() == PacketType.Play.Server.ABILITIES) {
            WrappedPacketOutAbilities abilities = new WrappedPacketOutAbilities(event.getNMSPacket());
            GrimPlayer player = GrimAC.playerGrimHashMap.get(event.getPlayer());

            // Occurs on login - we set if the player can fly on PlayerJoinEvent
            if (player == null) return;

            player.originalPacket = !player.originalPacket;

            if (!player.originalPacket) {
                player.compensatedFlying.setCanPlayerFly(abilities.isFlightAllowed());
                player.compensatedFlying.lagCompensatedIsFlyingMap.put(player.lastTransactionSent.get(), abilities.isFlying());

                // Send a transaction packet immediately after this packet
                PacketEvents.get().getPlayerUtils().sendPacket(event.getPlayer(),
                        new WrappedPacketOutAbilities(abilities.isVulnerable(), abilities.isFlying(),
                                abilities.isFlightAllowed(), abilities.canBuildInstantly(),
                                abilities.getFlySpeed(), abilities.getWalkSpeed()));
                PacketEvents.get().getPlayerUtils().sendPacket(event.getPlayer(),
                        new WrappedPacketOutTransaction(0, player.getNextTransactionID(), false));

                // Do this last in case of errors, sending multiple abilities packets accidentally is fine
                event.setCancelled(true);
            }

        }
    }
}
