package ac.grim.grimac.utils.collisions.types;

import ac.grim.grimac.utils.collisions.CollisionBox;
import ac.grim.grimac.utils.data.ProtocolVersion;
import org.bukkit.block.data.BlockData;

public interface CollisionFactory {
    // For legacy versions
    CollisionBox fetch(ProtocolVersion version, byte data, int x, int y, int z);

    // For modern versions
    CollisionBox fetch(ProtocolVersion version, BlockData block, int x, int y, int z);
}