package ac.grim.grimac.events.packets;

import ac.grim.grimac.GrimAC;
import ac.grim.grimac.player.GrimPlayer;
import io.github.retrooper.packetevents.event.PacketListenerAbstract;
import io.github.retrooper.packetevents.event.PacketListenerPriority;
import io.github.retrooper.packetevents.event.impl.PacketPlayReceiveEvent;
import io.github.retrooper.packetevents.packettype.PacketType;
import io.github.retrooper.packetevents.packetwrappers.play.in.flying.WrappedPacketInFlying;

public class NoFallCorrector extends PacketListenerAbstract {

    public NoFallCorrector() {
        super(PacketListenerPriority.NORMAL);
    }

    @Override
    public void onPacketPlayReceive(PacketPlayReceiveEvent event) {
        byte packetID = event.getPacketId();

        if (packetID == PacketType.Play.Client.LOOK) {
            GrimPlayer player = GrimAC.playerGrimHashMap.get(event.getPlayer());
            if (player == null) return;

            WrappedPacketInFlying flying = new WrappedPacketInFlying(event.getNMSPacket());
            if (player.noFall.checkZeroPointZeroThreeGround(flying.isOnGround()))
                flying.setOnGround(false);
        }

        if (packetID == PacketType.Play.Client.FLYING) {
            GrimPlayer player = GrimAC.playerGrimHashMap.get(event.getPlayer());
            if (player == null) return;

            WrappedPacketInFlying flying = new WrappedPacketInFlying(event.getNMSPacket());
            if (player.noFall.checkZeroPointZeroThreeGround(flying.isOnGround()))
                flying.setOnGround(false);
        }
    }
}
