package ac.grim.grimac.utils.data.packetentity;

import ac.grim.grimac.player.GrimPlayer;
import org.bukkit.block.BlockFace;

public class PacketEntityShulker extends PacketEntity {
    public BlockFace facing = BlockFace.DOWN;

    public PacketEntityShulker(GrimPlayer player, ac.grim.grimac.utils.enums.EntityType type, double x, double y, double z) {
        super(player, type, x, y, z);
    }
}
