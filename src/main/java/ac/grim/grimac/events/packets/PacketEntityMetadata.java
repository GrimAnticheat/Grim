package ac.grim.grimac.events.packets;

import ac.grim.grimac.GrimAC;
import ac.grim.grimac.player.GrimPlayer;
import io.github.retrooper.packetevents.event.PacketListenerDynamic;
import io.github.retrooper.packetevents.event.impl.PacketPlayReceiveEvent;
import io.github.retrooper.packetevents.packettype.PacketType;
import io.github.retrooper.packetevents.packetwrappers.play.in.abilities.WrappedPacketInAbilities;

public class PacketEntityMetadata extends PacketListenerDynamic {
    @Override
    public void onPacketPlayReceive(PacketPlayReceiveEvent event) {
        if (event.getPacketId() == PacketType.Play.Client.ABILITIES) {
            WrappedPacketInAbilities action = new WrappedPacketInAbilities(event.getNMSPacket());
            GrimPlayer player = GrimAC.playerGrimHashMap.get(event.getPlayer());

            // TODO: We need to lag compensate can fly
            // TODO: If a player logs in while flying, the canFly is wrong.  Hacked around by using bukkit player isFlying
            player.packetIsFlying = action.isFlying() && (player.entityPlayer.abilities.canFly || player.bukkitPlayer.isFlying());
        }
    }
}
