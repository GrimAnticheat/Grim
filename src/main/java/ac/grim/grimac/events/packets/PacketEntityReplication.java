package ac.grim.grimac.events.packets;

import ac.grim.grimac.GrimAC;
import ac.grim.grimac.player.GrimPlayer;
import io.github.retrooper.packetevents.event.PacketListenerDynamic;
import io.github.retrooper.packetevents.event.impl.PacketPlaySendEvent;
import io.github.retrooper.packetevents.packettype.PacketType;
import io.github.retrooper.packetevents.packetwrappers.play.out.entity.WrappedPacketOutEntity;
import io.github.retrooper.packetevents.packetwrappers.play.out.entitydestroy.WrappedPacketOutEntityDestroy;
import io.github.retrooper.packetevents.packetwrappers.play.out.namedentityspawn.WrappedPacketOutNamedEntitySpawn;
import net.minecraft.server.v1_16_R3.PacketPlayOutBlockAction;
import org.bukkit.entity.Entity;

public class PacketEntityReplication extends PacketListenerDynamic {

    @Override
    public void onPacketPlaySend(PacketPlaySendEvent event) {
        byte packetID = event.getPacketId();

        if (packetID == PacketType.Play.Server.SPAWN_ENTITY) {
            WrappedPacketOutEntity packetOutEntity = new WrappedPacketOutEntity(event.getNMSPacket());
            GrimPlayer player = GrimAC.playerGrimHashMap.get(event.getPlayer());
            Entity entity = packetOutEntity.getEntity();

            // The entity must have been immediately despawned
            if (entity == null) return;

            player.compensatedEntities.addEntity(packetOutEntity.getEntity());
        }

        if (packetID == PacketType.Play.Server.ENTITY_DESTROY) {
            WrappedPacketOutEntityDestroy destroy = new WrappedPacketOutEntityDestroy(event.getNMSPacket());
            GrimPlayer player = GrimAC.playerGrimHashMap.get(event.getPlayer());
            int[] destroyEntityIds = destroy.getEntityIds();

            player.compensatedEntities.removeEntity(destroyEntityIds);
        }

        if (packetID == PacketType.Play.Server.SPAWN_ENTITY_LIVING) {

        }

        if (packetID == PacketType.Play.Server.SPAWN_ENTITY_SPAWN) {
            WrappedPacketOutNamedEntitySpawn spawn = new WrappedPacketOutNamedEntitySpawn(event.getNMSPacket());

        }

        if (packetID == PacketType.Play.Server.BLOCK_ACTION) {
            PacketPlayOutBlockAction action = (PacketPlayOutBlockAction) event.getNMSPacket().getRawNMSPacket();


        }
    }
}
