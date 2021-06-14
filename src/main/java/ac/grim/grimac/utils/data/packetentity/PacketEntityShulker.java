package ac.grim.grimac.utils.data.packetentity;

import io.github.retrooper.packetevents.utils.vector.Vector3d;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;

public class PacketEntityShulker extends PacketEntity {
    public BlockFace facing = BlockFace.DOWN;
    public byte wantedShieldHeight = 0;
    public long lastShieldChange = 0;

    public PacketEntityShulker(Entity entity, Vector3d position) {
        super(entity, position);
    }
}
