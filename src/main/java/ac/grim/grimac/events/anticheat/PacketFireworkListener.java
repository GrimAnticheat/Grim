package ac.grim.grimac.events.anticheat;

import ac.grim.grimac.GrimAC;
import ac.grim.grimac.GrimPlayer;
import ac.grim.grimac.utils.data.FireworkData;
import io.github.retrooper.packetevents.event.PacketListenerDynamic;
import io.github.retrooper.packetevents.event.impl.PacketPlaySendEvent;
import io.github.retrooper.packetevents.event.priority.PacketEventPriority;
import io.github.retrooper.packetevents.packettype.PacketType;
import io.github.retrooper.packetevents.packetwrappers.play.out.entity.WrappedPacketOutEntity;
import net.minecraft.server.v1_16_R3.DataWatcher;
import net.minecraft.server.v1_16_R3.PacketPlayOutEntityDestroy;
import net.minecraft.server.v1_16_R3.PacketPlayOutEntityMetadata;
import org.bukkit.Bukkit;
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
            PacketPlayOutEntityMetadata metadata = (PacketPlayOutEntityMetadata) event.getNMSPacket().getRawNMSPacket();

            try {
                Field entityID = metadata.getClass().getDeclaredField("a");
                entityID.setAccessible(true);

                if (fireworks.remove(entityID.getInt(metadata))) {
                    Field data = metadata.getClass().getDeclaredField("b");
                    data.setAccessible(true);
                    List<DataWatcher.Item<?>> b = (List<DataWatcher.Item<?>>) data.get(metadata);

                    DataWatcher.Item<?> entry = b.get(4);
                    Field value = entry.getClass().getDeclaredField("b");
                    value.setAccessible(true);

                    OptionalInt attachedEntityID = (OptionalInt) value.get(entry);

                    if (attachedEntityID.isPresent()) {
                        Bukkit.broadcastMessage("What is this? " + attachedEntityID.getAsInt());

                        for (GrimPlayer grimPlayer : GrimAC.playerGrimHashMap.values()) {
                            if (grimPlayer.entityID == attachedEntityID.getAsInt()) {
                                grimPlayer.fireworks.put(entityID.getInt(metadata), new FireworkData());
                            }
                        }
                    }
                }
            } catch (NoSuchFieldException | IllegalAccessException e) {
                e.printStackTrace();
            }
        }

        if (packetID == PacketType.Play.Server.ENTITY_DESTROY) {
            PacketPlayOutEntityDestroy destroy = (PacketPlayOutEntityDestroy) event.getNMSPacket().getRawNMSPacket();
            try {
                Field entities = destroy.getClass().getDeclaredField("a");
                entities.setAccessible(true);

                for (int entity : (int[]) entities.get(destroy)) {
                    for (GrimPlayer grimPlayer : GrimAC.playerGrimHashMap.values()) {
                        if (grimPlayer.fireworks.containsKey(entity)) {
                            FireworkData fireworkData = grimPlayer.fireworks.get(entity);
                            fireworkData.setDestroyed();
                        }
                    }
                }
            } catch (NoSuchFieldException | IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }
}
