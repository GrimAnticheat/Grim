package ac.grim.grimac.utils.data.packetentity;

import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.protocol.entity.type.EntityType;
import com.github.retrooper.packetevents.protocol.world.BlockFace;

public class PacketEntityShulker extends PacketEntity {
    public BlockFace facing = BlockFace.DOWN;

    public PacketEntityShulker(final GrimPlayer player, final EntityType type,
                               final double x, final double y, final double z) {
        super(player, type, x, y, z);
    }
}
