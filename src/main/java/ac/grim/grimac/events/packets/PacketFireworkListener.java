package ac.grim.grimac.events.packets;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.nmsImplementations.WatchableIndexUtil;
import io.github.retrooper.packetevents.event.PacketListenerAbstract;
import io.github.retrooper.packetevents.event.PacketListenerPriority;
import io.github.retrooper.packetevents.event.impl.PacketPlaySendEvent;
import io.github.retrooper.packetevents.packettype.PacketType;
import io.github.retrooper.packetevents.packetwrappers.play.out.entity.WrappedPacketOutEntity;
import io.github.retrooper.packetevents.packetwrappers.play.out.entitydestroy.WrappedPacketOutEntityDestroy;
import io.github.retrooper.packetevents.packetwrappers.play.out.entitymetadata.WrappedPacketOutEntityMetadata;
import io.github.retrooper.packetevents.packetwrappers.play.out.entitymetadata.WrappedWatchableObject;
import io.github.retrooper.packetevents.utils.server.ServerVersion;
import org.bukkit.entity.Firework;

import java.util.HashSet;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;

public class PacketFireworkListener extends PacketListenerAbstract {
    Set<Integer> fireworks = new HashSet<>();

    public PacketFireworkListener() {
        super(PacketListenerPriority.MONITOR);
    }

    @Override
    public void onPacketPlaySend(PacketPlaySendEvent event) {
        byte packetID = event.getPacketId();

        if (packetID == PacketType.Play.Server.SPAWN_ENTITY) {
            WrappedPacketOutEntity entity = new WrappedPacketOutEntity(event.getNMSPacket());
            if (entity.getEntity() instanceof Firework) {
                fireworks.add(entity.getEntityId());
            }
        }

        if (packetID == PacketType.Play.Server.ENTITY_METADATA) {
            WrappedPacketOutEntityMetadata entityMetadata = new WrappedPacketOutEntityMetadata(event.getNMSPacket());

            if (fireworks.remove(entityMetadata.getEntityId())) {
                WrappedWatchableObject fireworkWatchableObject = WatchableIndexUtil.getIndex(entityMetadata.getWatchableObjects(), ServerVersion.getVersion().isNewerThanOrEquals(ServerVersion.v_1_17) ? 9 : 8);

                if (fireworkWatchableObject == null) return;

                OptionalInt attachedEntityID = (OptionalInt) fireworkWatchableObject.getRawValue();

                if (attachedEntityID.isPresent()) {
                    for (GrimPlayer player : GrimAPI.INSTANCE.getPlayerDataManager().getEntries()) {
                        if (player.entityID == attachedEntityID.getAsInt()) {
                            player.compensatedFireworks.addNewFirework(entityMetadata.getEntityId());
                        }
                    }
                }
            }
        }

        if (packetID == PacketType.Play.Server.ENTITY_DESTROY) {
            WrappedPacketOutEntityDestroy destroy = new WrappedPacketOutEntityDestroy(event.getNMSPacket());

            for (int entity : destroy.getEntityIds()) {
                for (GrimPlayer grimPlayer : GrimAPI.INSTANCE.getPlayerDataManager().getEntries()) {
                    grimPlayer.compensatedFireworks.removeFirework(entity);
                }
            }
        }
    }
}
