package ac.grim.grimac.events.packets;

import ac.grim.grimac.GrimAC;
import ac.grim.grimac.player.GrimPlayer;
import io.github.retrooper.packetevents.event.PacketListenerDynamic;
import io.github.retrooper.packetevents.event.impl.PacketPlaySendEvent;
import io.github.retrooper.packetevents.event.priority.PacketEventPriority;
import io.github.retrooper.packetevents.packettype.PacketType;
import io.github.retrooper.packetevents.packetwrappers.play.out.entity.WrappedPacketOutEntity;
import io.github.retrooper.packetevents.packetwrappers.play.out.entitydestroy.WrappedPacketOutEntityDestroy;
import org.bukkit.entity.Firework;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.List;
import java.util.OptionalInt;
import java.util.Set;

public class PacketFireworkListener extends PacketListenerDynamic {
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
            // PacketPlayOutEntityMetadata
            Object metadata = event.getNMSPacket().getRawNMSPacket();

            try {
                Field entityID = metadata.getClass().getDeclaredField("a");
                entityID.setAccessible(true);

                if (fireworks.remove(entityID.getInt(metadata))) {
                    Field data = metadata.getClass().getDeclaredField("b");
                    data.setAccessible(true);
                    // DataWatcher.Item<?>
                    List<Object> b = (List<Object>) data.get(metadata);

                    // DataWatcher.Item<?>
                    Object entry = b.get(4);
                    Field value = entry.getClass().getDeclaredField("b");
                    value.setAccessible(true);

                    OptionalInt attachedEntityID = (OptionalInt) value.get(entry);

                    if (attachedEntityID.isPresent()) {
                        for (GrimPlayer grimPlayer : GrimAC.playerGrimHashMap.values()) {
                            if (grimPlayer.entityID == attachedEntityID.getAsInt()) {
                                grimPlayer.compensatedFireworks.addNewFirework(entityID.getInt(metadata));
                            }
                        }
                    }
                }
            } catch (NoSuchFieldException | IllegalAccessException e) {
                e.printStackTrace();
            }
        }

        if (packetID == PacketType.Play.Server.ENTITY_DESTROY) {
            WrappedPacketOutEntityDestroy destroy = new WrappedPacketOutEntityDestroy(event.getNMSPacket());

            for (int entity : destroy.getEntityIds()) {
                for (GrimPlayer grimPlayer : GrimAC.playerGrimHashMap.values()) {
                    grimPlayer.compensatedFireworks.removeFirework(entity);
                }
            }

        }
    }
}
