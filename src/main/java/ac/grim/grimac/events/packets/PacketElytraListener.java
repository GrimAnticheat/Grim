package ac.grim.grimac.events.packets;

import ac.grim.grimac.GrimAC;
import ac.grim.grimac.player.GrimPlayer;
import io.github.retrooper.packetevents.event.PacketListenerAbstract;
import io.github.retrooper.packetevents.event.impl.PacketPlaySendEvent;
import io.github.retrooper.packetevents.event.priority.PacketEventPriority;
import io.github.retrooper.packetevents.packettype.PacketType;
import io.github.retrooper.packetevents.packetwrappers.play.out.entitymetadata.WrappedPacketOutEntityMetadata;
import io.github.retrooper.packetevents.packetwrappers.play.out.entitymetadata.WrappedWatchableObject;
import io.github.retrooper.packetevents.utils.player.ClientVersion;

public class PacketElytraListener extends PacketListenerAbstract {
    public PacketElytraListener() {
        super(PacketEventPriority.MONITOR);
    }

    @Override
    public void onPacketPlaySend(PacketPlaySendEvent event) {
        byte packetID = event.getPacketId();

        if (packetID == PacketType.Play.Server.ENTITY_METADATA) {
            WrappedPacketOutEntityMetadata entityMetadata = new WrappedPacketOutEntityMetadata(event.getNMSPacket());
            if (entityMetadata.getEntityId() == event.getPlayer().getEntityId()) {
                GrimPlayer player = GrimAC.playerGrimHashMap.get(event.getPlayer());
                WrappedWatchableObject watchable = entityMetadata.getWatchableObjects().get(0);
                Object zeroBitField = watchable.getRawValue();

                if (player == null)
                    return;

                if (zeroBitField instanceof Byte && watchable.getIndex() == 0) {
                    byte field = (byte) zeroBitField;
                    boolean isGliding = (field & 0x80) == 0x80 && player.getClientVersion().isNewerThanOrEquals(ClientVersion.v_1_9);

                    int transactionSent = player.lastTransactionSent.get();
                    event.setPostTask(player::sendTransactionOrPingPong);
                    player.compensatedElytra.tryAddStatus(transactionSent, isGliding);
                }
            }
        }
    }
}
