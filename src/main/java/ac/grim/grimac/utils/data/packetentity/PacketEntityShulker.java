package ac.grim.grimac.utils.data.packetentity;

import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;

public class PacketEntityShulker extends PacketEntity {
    BlockFace facing = BlockFace.DOWN;
    byte shieldHeight = 0;

    public PacketEntityShulker(Entity entity) {
        super(entity);
    }
}
