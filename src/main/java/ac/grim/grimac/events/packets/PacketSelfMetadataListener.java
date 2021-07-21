package ac.grim.grimac.events.packets;

import ac.grim.grimac.GrimAC;
import ac.grim.grimac.player.GrimPlayer;
import io.github.retrooper.packetevents.event.PacketListenerAbstract;
import io.github.retrooper.packetevents.event.PacketListenerPriority;
import io.github.retrooper.packetevents.event.impl.PacketPlaySendEvent;
import io.github.retrooper.packetevents.packettype.PacketType;
import io.github.retrooper.packetevents.packetwrappers.play.out.entitymetadata.WrappedPacketOutEntityMetadata;
import io.github.retrooper.packetevents.packetwrappers.play.out.entitymetadata.WrappedWatchableObject;
import io.github.retrooper.packetevents.utils.player.ClientVersion;
import io.github.retrooper.packetevents.utils.server.ServerVersion;

import java.util.Optional;

public class PacketSelfMetadataListener extends PacketListenerAbstract {
    public PacketSelfMetadataListener() {
        super(PacketListenerPriority.MONITOR);
    }

    @Override
    public void onPacketPlaySend(PacketPlaySendEvent event) {
        byte packetID = event.getPacketId();

        if (packetID == PacketType.Play.Server.ENTITY_METADATA) {
            WrappedPacketOutEntityMetadata entityMetadata = new WrappedPacketOutEntityMetadata(event.getNMSPacket());
            if (entityMetadata.getEntityId() == event.getPlayer().getEntityId()) {
                GrimPlayer player = GrimAC.playerGrimHashMap.get(event.getPlayer());

                if (player == null)
                    return;

                Optional<WrappedWatchableObject> watchable = entityMetadata.getWatchableObjects()
                        .stream().filter(o -> o.getIndex() == (0)).findFirst();

                // This one has always been present but I guess some jar could mess it up
                if (watchable.isPresent()) {
                    Object zeroBitField = watchable.get().getRawValue();

                    if (zeroBitField instanceof Byte) {
                        byte field = (byte) zeroBitField;
                        boolean isGliding = (field & 0x80) == 0x80 && player.getClientVersion().isNewerThanOrEquals(ClientVersion.v_1_9);

                        int transactionSent = player.lastTransactionSent.get();
                        event.setPostTask(player::sendTransactionOrPingPong);
                        player.compensatedElytra.tryAddStatus(transactionSent, isGliding);
                    }
                }


                if (ServerVersion.getVersion().isNewerThanOrEquals(ServerVersion.v_1_13) &&
                        player.getClientVersion().isNewerThanOrEquals(ClientVersion.v_1_13)) {
                    Optional<WrappedWatchableObject> riptide = entityMetadata.getWatchableObjects()
                            .stream().filter(o -> o.getIndex() == (ServerVersion.getVersion().isNewerThanOrEquals(ServerVersion.v_1_17) ? 8 : 7)).findFirst();

                    // This one only present if it changed
                    if (riptide.isPresent() && riptide.get().getRawValue() instanceof Byte) {
                        boolean isRiptiding = (((byte) riptide.get().getRawValue()) & 0x04) == 0x04;

                        player.compensatedRiptide.setPose(isRiptiding);
                    }
                }
            }
        }
    }
}
