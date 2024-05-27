package ac.grim.grimac.utils.data.packetentity;

import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.protocol.entity.type.EntityType;

public class PacketEntitySizeable extends PacketEntity {
    public int size = 4; // To support entity metadata being sent after spawn, assume max size of vanilla slime

    public PacketEntitySizeable(final GrimPlayer player, final EntityType type,
                                final double x, final double y, final double z) {
        super(player, type, x, y, z);
    }
}
