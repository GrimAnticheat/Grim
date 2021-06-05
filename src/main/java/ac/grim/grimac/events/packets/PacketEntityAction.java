package ac.grim.grimac.events.packets;

import ac.grim.grimac.GrimAC;
import ac.grim.grimac.player.GrimPlayer;
import io.github.retrooper.packetevents.event.PacketListenerAbstract;
import io.github.retrooper.packetevents.event.impl.PacketPlayReceiveEvent;
import io.github.retrooper.packetevents.packettype.PacketType;
import io.github.retrooper.packetevents.packetwrappers.play.in.entityaction.WrappedPacketInEntityAction;
import org.bukkit.Bukkit;

public class PacketEntityAction extends PacketListenerAbstract {
    @Override
    public void onPacketPlayReceive(PacketPlayReceiveEvent event) {
        if (event.getPacketId() == PacketType.Play.Client.ENTITY_ACTION) {
            WrappedPacketInEntityAction action = new WrappedPacketInEntityAction(event.getNMSPacket());
            GrimPlayer player = GrimAC.playerGrimHashMap.get(event.getPlayer());

            if (player == null) return;

            switch (action.getAction()) {
                case START_SPRINTING:
                    player.packetStateData.isPacketSprinting = true;
                    break;
                case STOP_SPRINTING:
                    player.packetStateData.isPacketSprinting = false;
                    break;
                case START_SNEAKING:
                    player.packetStateData.isPacketSneaking = true;
                    break;
                case STOP_SNEAKING:
                    player.packetStateData.isPacketSneaking = false;
                    break;
                case START_FALL_FLYING:
                    player.compensatedElytra.lastToggleElytra = player.packetStateData.packetLastTransactionReceived;
                    break;
            }
        }
    }
}
