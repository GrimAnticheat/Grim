package ac.grim.grimac.events.packets;

import ac.grim.grimac.GrimAC;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.nmsImplementations.XMaterial;
import io.github.retrooper.packetevents.event.PacketListenerAbstract;
import io.github.retrooper.packetevents.event.impl.PacketPlaySendEvent;
import io.github.retrooper.packetevents.event.priority.PacketEventPriority;
import io.github.retrooper.packetevents.packettype.PacketType;
import io.github.retrooper.packetevents.packetwrappers.play.out.entity.WrappedPacketOutEntity;
import io.github.retrooper.packetevents.packetwrappers.play.out.entitydestroy.WrappedPacketOutEntityDestroy;
import io.github.retrooper.packetevents.packetwrappers.play.out.entitymetadata.WrappedPacketOutEntityMetadata;
import io.github.retrooper.packetevents.packetwrappers.play.out.entitymetadata.WrappedWatchableObject;
import org.bukkit.entity.Firework;

import java.util.HashSet;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;

public class PacketFireworkListener extends PacketListenerAbstract {
    Set<Integer> fireworks = new HashSet<>();

    public PacketFireworkListener() {
        super(PacketEventPriority.MONITOR);
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
                Optional<WrappedWatchableObject> fireworkWatchableObject = entityMetadata.getWatchableObjects().stream().filter(o -> o.getIndex() == (XMaterial.getVersion() >= 17 ? 9 : 8)).findFirst();
                if (!fireworkWatchableObject.isPresent()) return;

                OptionalInt attachedEntityID = (OptionalInt) fireworkWatchableObject.get().getRawValue();

                if (attachedEntityID.isPresent()) {
                    for (GrimPlayer player : GrimAC.playerGrimHashMap.values()) {
                        if (player.entityID == attachedEntityID.getAsInt()) {
                            player.compensatedFireworks.addNewFirework(entityMetadata.getEntityId());
                        }
                    }
                }
            }
        }

        if (packetID == PacketType.Play.Server.ENTITY_DESTROY) {
            WrappedPacketOutEntityDestroy destroy = new WrappedPacketOutEntityDestroy(event.getNMSPacket());

            if (destroy.getEntityIds().isPresent()) {
                for (int entity : destroy.getEntityIds().get()) {
                    for (GrimPlayer grimPlayer : GrimAC.playerGrimHashMap.values()) {
                        grimPlayer.compensatedFireworks.removeFirework(entity);
                    }
                }
            }
        }
    }
}
