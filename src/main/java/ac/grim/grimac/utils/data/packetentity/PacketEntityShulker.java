package ac.grim.grimac.utils.data.packetentity;

import io.github.retrooper.packetevents.utils.vector.Vector3d;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.EntityType;

public class PacketEntityShulker extends PacketEntity {
    public BlockFace facing = BlockFace.DOWN;

    public PacketEntityShulker(EntityType type, Vector3d position) {
        super(type, position);
    }
}
