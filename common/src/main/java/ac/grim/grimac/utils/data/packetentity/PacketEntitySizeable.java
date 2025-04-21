package ac.grim.grimac.utils.data.packetentity;

import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.protocol.entity.type.EntityType;

import java.util.UUID;

public class PacketEntitySizeable extends PacketEntity {
    // It appears in modern versions (oldest tested was 1.16.5)
    // that no entity metadata is sent for a slime when {Size: 0} (which actually corresponds to size = 1, the smallest slime in vanilla)

    // Previously to support entity metadata being sent after spawn, we assumed max size of vanilla slime
    // as the default size; I'm not sure if we still need to do this. Will change behaviour if issues reported
    public int size = 1;

    public PacketEntitySizeable(GrimPlayer player, UUID uuid, EntityType type, double x, double y, double z) {
        super(player, uuid, type, x, y, z);
    }
}
